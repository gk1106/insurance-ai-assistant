package com.insuranceai.backend.claim.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ClaimRequestDto(
        @NotNull UUID policyId,
        @NotNull @Positive BigDecimal claimAmount,
        @NotNull @PastOrPresent LocalDate incidentDate,
        @NotBlank @Size(max = 2000) String description
) {
}
