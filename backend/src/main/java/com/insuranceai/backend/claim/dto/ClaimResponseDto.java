package com.insuranceai.backend.claim.dto;

import com.insuranceai.backend.claim.entity.ClaimStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ClaimResponseDto(
        UUID id,
        String claimNumber,
        UUID policyId,
        String policyNumber,
        BigDecimal claimAmount,
        BigDecimal approvedAmount,
        ClaimStatus status,
        LocalDate incidentDate,
        String description,
        String rejectionReason,
        Instant filedAt,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
