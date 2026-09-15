package com.insuranceai.backend.ai.rag.dto;

import java.util.List;

public record IngestionResultDto(String documentsDirectory, List<IngestedFileSummary> files) {

    public int filesProcessed() {
        return files.size();
    }

    public long filesSucceeded() {
        return files.stream().filter(IngestedFileSummary::success).count();
    }

    public int totalChunksIngested() {
        return files.stream().mapToInt(IngestedFileSummary::chunkCount).sum();
    }
}
