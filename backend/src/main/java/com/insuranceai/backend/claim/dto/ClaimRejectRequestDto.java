package com.insuranceai.backend.claim.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClaimRejectRequestDto(
        @NotBlank @Size(max = 500) String reason
) {
}
