package com.insuranceai.backend.customer.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerResponseDto(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        LocalDate dateOfBirth,
        String addressLine,
        String city,
        String postalCode,
        String country,
        Instant createdAt,
        Instant updatedAt
) {
}
