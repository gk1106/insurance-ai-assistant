package com.insuranceai.backend.ai.claims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentChatRequestDto(
        @NotBlank @Size(max = 4000) String message
) {
}
