package com.insuranceai.backend.policy.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PolicyRenewRequestDto(
        @NotNull LocalDate newEndDate,
        @NotNull @Positive BigDecimal revisedPremiumAmount
) {
}
