package com.insuranceai.backend.claim.service;

import com.insuranceai.backend.claim.dto.ClaimApproveRequestDto;
import com.insuranceai.backend.claim.dto.ClaimRejectRequestDto;
import com.insuranceai.backend.claim.dto.ClaimRequestDto;
import com.insuranceai.backend.claim.dto.ClaimResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

public interface ClaimService {

    ClaimResponseDto create(ClaimRequestDto request);

    ClaimResponseDto getById(UUID id);

    Page<ClaimResponseDto> getByPolicy(UUID policyId, Pageable pageable);

    // @PreAuthorize is declared here (not just on ClaimController) so that any caller of this
    // service -- including the AI agent's tools, which invoke it directly rather than through the
    // controller -- is still subject to the same role check. Placed on the interface because
    // Spring Security's method-security proxy dispatches on the interface method for JDK proxies.
    // (Same pattern as PolicyService -- see the comment there for the full rationale.)
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    Page<ClaimResponseDto> getAll(Pageable pageable);

    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    ClaimResponseDto review(UUID id);

    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    ClaimResponseDto approve(UUID id, ClaimApproveRequestDto request);

    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    ClaimResponseDto reject(UUID id, ClaimRejectRequestDto request);

    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    ClaimResponseDto markPaid(UUID id);
}
