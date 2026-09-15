package com.insuranceai.backend.ai.rag.dto;

/**
 * One matched chunk from the vector store. {@code source} and {@code page} come straight from
 * the metadata {@link org.springframework.ai.reader.pdf.PagePdfDocumentReader} attaches to every
 * page it reads, so a caller can always trace a result back to the document (and page) it came
 * from. {@code score} is cosine similarity in [0, 1] -- higher is more relevant.
 */
public record RagSearchResultDto(String content, String source, Integer page, Double score) {
}
