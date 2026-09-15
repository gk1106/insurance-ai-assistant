package com.insuranceai.backend.renewal.service;

import com.insuranceai.backend.renewal.dto.RenewalRequestDto;
import com.insuranceai.backend.renewal.dto.RenewalResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

public interface RenewalService {

    RenewalResponseDto create(RenewalRequestDto request);

    RenewalResponseDto getById(UUID id);

    Page<RenewalResponseDto> getByPolicy(UUID policyId, Pageable pageable);

    // @PreAuthorize is declared here (not just on RenewalController) so that any caller of this
    // service -- including the AI agent's tools, which invoke it directly rather than through the
    // controller -- is still subject to the same role check. Same pattern as PolicyService /
    // ClaimService -- see the comment on PolicyService for the full rationale.
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    RenewalResponseDto confirm(UUID id);

    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    RenewalResponseDto reject(UUID id);
}
