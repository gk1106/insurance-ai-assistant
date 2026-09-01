package com.insuranceai.backend.policy.service;

import com.insuranceai.backend.policy.dto.PolicyRequestDto;
import com.insuranceai.backend.policy.dto.PolicyResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PolicyService {

    PolicyResponseDto create(PolicyRequestDto request);

    PolicyResponseDto getById(UUID id);

    PolicyResponseDto getByPolicyNumber(String policyNumber);

    Page<PolicyResponseDto> getAll(Pageable pageable);

    Page<PolicyResponseDto> getByCustomer(UUID customerId, Pageable pageable);

    PolicyResponseDto update(UUID id, PolicyRequestDto request);

    PolicyResponseDto cancel(UUID id);

    List<PolicyResponseDto> getExpiringWithin(int days);

    /**
     * Applies a confirmed renewal to its parent policy. This is the single write path for
     * renewal-driven policy changes, called by RenewalService rather than mutating Policy directly.
     */
    PolicyResponseDto applyRenewal(UUID policyId, LocalDate newEndDate, BigDecimal revisedPremiumAmount);
}
