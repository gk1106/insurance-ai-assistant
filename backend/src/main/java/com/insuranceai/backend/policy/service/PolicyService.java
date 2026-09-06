package com.insuranceai.backend.policy.service;

import com.insuranceai.backend.policy.dto.PolicyRequestDto;
import com.insuranceai.backend.policy.dto.PolicyResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PolicyService {

    // @PreAuthorize is declared here (not just on PolicyController) so that any caller of this
    // service -- including the AI agent's tools, which invoke it directly rather than through the
    // controller -- is still subject to the same role check. Placed on the interface because
    // Spring Security's method-security proxy dispatches on the interface method for JDK proxies.
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    PolicyResponseDto create(PolicyRequestDto request);

    PolicyResponseDto getById(UUID id);

    PolicyResponseDto getByPolicyNumber(String policyNumber);

    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    Page<PolicyResponseDto> getAll(Pageable pageable);

    Page<PolicyResponseDto> getByCustomer(UUID customerId, Pageable pageable);

    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    PolicyResponseDto update(UUID id, PolicyRequestDto request);

    PolicyResponseDto cancel(UUID id);

    List<PolicyResponseDto> getExpiringWithin(int days);

    /**
     * Applies a confirmed renewal to its parent policy. This is the single write path for
     * renewal-driven policy changes, called by RenewalService rather than mutating Policy directly.
     */
    PolicyResponseDto applyRenewal(UUID policyId, LocalDate newEndDate, BigDecimal revisedPremiumAmount);
}
