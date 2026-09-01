package com.insuranceai.backend.renewal.service;

import com.insuranceai.backend.renewal.dto.RenewalRequestDto;
import com.insuranceai.backend.renewal.dto.RenewalResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RenewalService {

    RenewalResponseDto create(RenewalRequestDto request);

    RenewalResponseDto getById(UUID id);

    Page<RenewalResponseDto> getByPolicy(UUID policyId, Pageable pageable);

    RenewalResponseDto confirm(UUID id);

    RenewalResponseDto reject(UUID id);
}
