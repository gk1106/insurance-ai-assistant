package com.insuranceai.backend.renewal.dto;

import com.insuranceai.backend.renewal.entity.RenewalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RenewalResponseDto(
        UUID id,
        UUID policyId,
        String policyNumber,
        LocalDate previousEndDate,
        LocalDate newEndDate,
        BigDecimal revisedPremiumAmount,
        RenewalStatus status,
        Instant requestedAt,
        Instant decidedAt
) {
}
