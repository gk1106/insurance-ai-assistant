package com.insuranceai.backend.ai.rag;

import com.insuranceai.backend.ai.rag.dto.IngestedFileSummary;
import com.insuranceai.backend.ai.rag.dto.IngestionResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The ingestion half of the local RAG pipeline: reads every PDF in the configured documents
 * directory, splits each into token-sized chunks, and stores their embeddings in the
 * pgvector-backed {@link VectorStore} (see V4__init_vector_store.sql). Not wired into any agent
 * or the Orchestrator yet -- this is the ingestion + retrieval foundation only.
 */
@Service
public class RagIngestionService {

    private static final Logger log = LoggerFactory.getLogger(RagIngestionService.class);
    private static final String METADATA_FILE_NAME = PagePdfDocumentReader.METADATA_FILE_NAME;

    private final VectorStore vectorStore;
    private final Path documentsDir;
    private final TokenTextSplitter textSplitter = new TokenTextSplitter();

    public RagIngestionService(VectorStore vectorStore,
                                @Value("${app.rag.documents-dir}") String documentsDir) {
        this.vectorStore = vectorStore;
        this.documentsDir = Path.of(documentsDir);
    }

    public IngestionResultDto ingestAll() {
        if (!Files.isDirectory(documentsDir)) {
            log.warn("rag ingest documentsDir={} outcome=missing", documentsDir.toAbsolutePath());
            return new IngestionResultDto(documentsDir.toAbsolutePath().toString(), List.of());
        }

        List<Path> pdfFiles = listPdfFiles();
        log.info("rag ingest start documentsDir={} pdfCount={}", documentsDir.toAbsolutePath(), pdfFiles.size());

        List<IngestedFileSummary> summaries = new ArrayList<>();
        for (Path pdfFile : pdfFiles) {
            summaries.add(ingestFile(pdfFile));
        }

        log.info("rag ingest end documentsDir={} filesProcessed={} totalChunks={}",
                documentsDir.toAbsolutePath(), summaries.size(),
                summaries.stream().mapToInt(IngestedFileSummary::chunkCount).sum());
        return new IngestionResultDto(documentsDir.toAbsolutePath().toString(), summaries);
    }

    private List<Path> listPdfFiles() {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(documentsDir,
                path -> Files.isRegularFile(path)
                        && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))) {
            stream.forEach(files::add);
        } catch (IOException e) {
            throw new RagIngestionException("Failed to list documents in " + documentsDir.toAbsolutePath(), e);
        }
        files.sort(Comparator.comparing(Path::getFileName));
        return files;
    }

    private IngestedFileSummary ingestFile(Path pdfFile) {
        String fileName = pdfFile.getFileName().toString();
        try {
            PagePdfDocumentReader reader = new PagePdfDocumentReader(new FileSystemResource(pdfFile));
            List<Document> pages = reader.get();
            List<Document> chunks = textSplitter.apply(pages);

            // Re-ingesting the same file (e.g. after editing it) replaces its previous chunks
            // instead of duplicating them.
            vectorStore.delete(new FilterExpressionBuilder().eq(METADATA_FILE_NAME, fileName).build());
            vectorStore.add(chunks);

            log.info("rag ingest file={} pages={} chunks={} outcome=success", fileName, pages.size(), chunks.size());
            return IngestedFileSummary.success(fileName, pages.size(), chunks.size());
        } catch (RuntimeException ex) {
            log.warn("rag ingest file={} outcome=error message={}", fileName, ex.getMessage());
            return IngestedFileSummary.failure(fileName, ex.getMessage());
        }
    }

    public static class RagIngestionException extends RuntimeException {
        public RagIngestionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
