package com.insuranceai.backend.claim.service;

import com.insuranceai.backend.claim.dto.ClaimApproveRequestDto;
import com.insuranceai.backend.claim.dto.ClaimRejectRequestDto;
import com.insuranceai.backend.claim.dto.ClaimRequestDto;
import com.insuranceai.backend.claim.dto.ClaimResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ClaimService {

    ClaimResponseDto create(ClaimRequestDto request);

    ClaimResponseDto getById(UUID id);

    Page<ClaimResponseDto> getByPolicy(UUID policyId, Pageable pageable);

    Page<ClaimResponseDto> getAll(Pageable pageable);

    ClaimResponseDto review(UUID id);

    ClaimResponseDto approve(UUID id, ClaimApproveRequestDto request);

    ClaimResponseDto reject(UUID id, ClaimRejectRequestDto request);

    ClaimResponseDto markPaid(UUID id);
}
