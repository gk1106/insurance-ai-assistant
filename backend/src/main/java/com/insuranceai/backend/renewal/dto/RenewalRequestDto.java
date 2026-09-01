package com.insuranceai.backend.renewal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RenewalRequestDto(
        @NotNull UUID policyId,
        @NotNull LocalDate newEndDate,
        @NotNull @Positive BigDecimal revisedPremiumAmount
) {
}
