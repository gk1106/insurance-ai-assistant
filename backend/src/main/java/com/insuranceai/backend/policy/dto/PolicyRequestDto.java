package com.insuranceai.backend.policy.dto;

import com.insuranceai.backend.policy.entity.PolicyType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PolicyRequestDto(
        @NotNull UUID customerId,
        @NotNull PolicyType policyType,
        @NotNull @Positive BigDecimal coverageAmount,
        @NotNull @Positive BigDecimal premiumAmount,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
