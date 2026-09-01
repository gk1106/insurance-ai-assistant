package com.insuranceai.backend.claim.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ClaimApproveRequestDto(
        @NotNull @Positive BigDecimal approvedAmount
) {
}
