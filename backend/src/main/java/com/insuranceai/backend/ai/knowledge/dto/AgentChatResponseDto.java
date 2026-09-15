package com.insuranceai.backend.ai.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.insuranceai.backend.ai.rag.dto.RagSearchResultDto;

import java.util.List;

/**
 * The natural-language reply is what the model said (and is expected to already cite its
 * sources inline); {@code sources} is the deduplicated list of document file names the retrieved
 * chunks actually came from, and {@code chunks} is the raw retrieved passages -- the frontend (or
 * a caller like the Orchestrator) should trust these over parsing the prose for source names.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentChatResponseDto(
        String reply,
        List<String> sources,
        List<RagSearchResultDto> chunks
) {
}
