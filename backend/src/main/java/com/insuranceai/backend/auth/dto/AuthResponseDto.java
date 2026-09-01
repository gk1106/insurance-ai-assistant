package com.insuranceai.backend.auth.dto;

import java.time.Instant;

public record AuthResponseDto(
        String token,
        String username,
        String role,
        Instant expiresAt
) {
}
