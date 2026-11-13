package com.insuranceai.backend.claim.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** All monetary amounts are INR (Indian rupees); numeric values are not converted. */
public record ClaimApproveRequestDto(
        @NotNull @Positive BigDecimal approvedAmount
) {
}
