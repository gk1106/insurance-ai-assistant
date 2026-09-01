package com.insuranceai.backend.claim.service.impl;

import com.insuranceai.backend.claim.dto.ClaimApproveRequestDto;
import com.insuranceai.backend.claim.dto.ClaimRequestDto;
import com.insuranceai.backend.claim.entity.Claim;
import com.insuranceai.backend.claim.entity.ClaimStatus;
import com.insuranceai.backend.claim.mapper.ClaimMapper;
import com.insuranceai.backend.claim.repository.ClaimRepository;
import com.insuranceai.backend.common.exception.BusinessRuleViolationException;
import com.insuranceai.backend.customer.entity.Customer;
import com.insuranceai.backend.policy.entity.Policy;
import com.insuranceai.backend.policy.entity.PolicyStatus;
import com.insuranceai.backend.policy.repository.PolicyRepository;
import com.insuranceai.backend.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClaimServiceImplTest {

    private final ClaimRepository claimRepository = mock(ClaimRepository.class);
    private final PolicyRepository policyRepository = mock(PolicyRepository.class);
    private final ClaimMapper claimMapper = mock(ClaimMapper.class);
    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);

    private final ClaimServiceImpl claimService =
            new ClaimServiceImpl(claimRepository, policyRepository, claimMapper, currentUserProvider);

    @Test
    void create_throwsWhenClaimAmountExceedsCoverage() {
        UUID policyId = UUID.randomUUID();
        Policy policy = new Policy();
        policy.setCustomer(new Customer());
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setCoverageAmount(new BigDecimal("1000.00"));
        when(policyRepository.findById(policyId)).thenReturn(Optional.of(policy));

        ClaimRequestDto request = new ClaimRequestDto(policyId, new BigDecimal("2000.00"), LocalDate.now(), "Crash");

        assertThatThrownBy(() -> claimService.create(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("cannot exceed");
    }

    @Test
    void create_throwsWhenPolicyNotActive() {
        UUID policyId = UUID.randomUUID();
        Policy policy = new Policy();
        policy.setCustomer(new Customer());
        policy.setStatus(PolicyStatus.CANCELLED);
        policy.setCoverageAmount(new BigDecimal("1000.00"));
        when(policyRepository.findById(policyId)).thenReturn(Optional.of(policy));

        ClaimRequestDto request = new ClaimRequestDto(policyId, new BigDecimal("500.00"), LocalDate.now(), "Crash");

        assertThatThrownBy(() -> claimService.create(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("active policy");
    }

    @Test
    void approve_throwsWhenApprovedAmountExceedsClaimAmount() {
        UUID claimId = UUID.randomUUID();
        Policy policy = new Policy();
        policy.setCoverageAmount(new BigDecimal("5000.00"));
        Claim claim = new Claim();
        claim.setPolicy(policy);
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        claim.setClaimAmount(new BigDecimal("1000.00"));
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));

        ClaimApproveRequestDto request = new ClaimApproveRequestDto(new BigDecimal("1500.00"));

        assertThatThrownBy(() -> claimService.approve(claimId, request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("cannot exceed the claim amount");
    }

    @Test
    void review_throwsWhenClaimNotSubmitted() {
        UUID claimId = UUID.randomUUID();
        Claim claim = new Claim();
        claim.setStatus(ClaimStatus.APPROVED);
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> claimService.review(claimId))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
