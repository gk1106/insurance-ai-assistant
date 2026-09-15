package com.insuranceai.backend.ai.rag.dto;

/** Per-file outcome of one ingestion run -- either a chunk/page count, or an error message. */
public record IngestedFileSummary(String fileName, boolean success, int pageCount, int chunkCount, String error) {

    public static IngestedFileSummary success(String fileName, int pageCount, int chunkCount) {
        return new IngestedFileSummary(fileName, true, pageCount, chunkCount, null);
    }

    public static IngestedFileSummary failure(String fileName, String error) {
        return new IngestedFileSummary(fileName, false, 0, 0, error);
    }
}
