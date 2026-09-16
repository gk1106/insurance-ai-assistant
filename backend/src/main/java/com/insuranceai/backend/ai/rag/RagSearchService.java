package com.insuranceai.backend.ai.rag;

import com.insuranceai.backend.ai.rag.dto.RagSearchResultDto;
import com.insuranceai.backend.common.exception.BusinessRuleViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * The retrieval half of the local RAG pipeline: semantic similarity search over whatever
 * {@link RagIngestionService} has ingested. Not wired into any agent or the Orchestrator yet.
 */
@Service
public class RagSearchService {

    private static final Logger log = LoggerFactory.getLogger(RagSearchService.class);
    private static final int DEFAULT_TOP_K = 5;

    private final VectorStore vectorStore;

    public RagSearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<RagSearchResultDto> search(String query, Integer topK) {
        return search(query, topK, SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL);
    }

    /**
     * Same as {@link #search(String, Integer)}, but with a minimum cosine-similarity cutoff.
     * Used by {@code search_knowledge_base} (the Knowledge Agent's tool) so an off-topic question
     * gets no chunks back rather than the nearest-but-irrelevant ones -- an LLM asked to answer
     * "only from the retrieved context" will otherwise still try to make something of a weakly
     * related passage.
     */
    public List<RagSearchResultDto> search(String query, Integer topK, double similarityThreshold) {
        if (query == null || query.isBlank()) {
            throw new BusinessRuleViolationException("query must not be blank");
        }
        int effectiveTopK = (topK != null && topK > 0) ? topK : DEFAULT_TOP_K;

        long startNanos = System.nanoTime();
        List<Document> matches = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(effectiveTopK).similarityThreshold(similarityThreshold).build());
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;

        List<RagSearchResultDto> results = matches.stream().map(this::toResult).toList();
        List<String> sourceDocuments = results.stream().map(RagSearchResultDto::source).distinct().toList();

        log.info("rag-retrieval topK={} similarityThreshold={} durationMs={} chunksReturned={} sources={}",
                effectiveTopK, similarityThreshold, durationMs, results.size(), sourceDocuments);

        return results;
    }

    private RagSearchResultDto toResult(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        String fileName = String.valueOf(metadata.getOrDefault(PagePdfDocumentReader.METADATA_FILE_NAME, "unknown"));
        Integer page = metadata.get(PagePdfDocumentReader.METADATA_START_PAGE_NUMBER) instanceof Number n
                ? n.intValue() : null;
        return new RagSearchResultDto(document.getText(), fileName, page, document.getScore());
    }
}
