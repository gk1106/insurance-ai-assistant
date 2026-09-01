package com.insuranceai.backend.policy.dto;

import com.insuranceai.backend.policy.entity.PolicyStatus;
import com.insuranceai.backend.policy.entity.PolicyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PolicyResponseDto(
        UUID id,
        String policyNumber,
        UUID customerId,
        String customerName,
        PolicyType policyType,
        PolicyStatus status,
        BigDecimal coverageAmount,
        BigDecimal premiumAmount,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt,
        Instant updatedAt
) {
}
