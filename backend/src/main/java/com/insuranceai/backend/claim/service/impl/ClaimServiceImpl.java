package com.insuranceai.backend.claim.service.impl;

import com.insuranceai.backend.claim.dto.ClaimApproveRequestDto;
import com.insuranceai.backend.claim.dto.ClaimRejectRequestDto;
import com.insuranceai.backend.claim.dto.ClaimRequestDto;
import com.insuranceai.backend.claim.dto.ClaimResponseDto;
import com.insuranceai.backend.claim.entity.Claim;
import com.insuranceai.backend.claim.entity.ClaimStatus;
import com.insuranceai.backend.claim.mapper.ClaimMapper;
import com.insuranceai.backend.claim.repository.ClaimRepository;
import com.insuranceai.backend.claim.service.ClaimService;
import com.insuranceai.backend.common.exception.BusinessRuleViolationException;
import com.insuranceai.backend.common.exception.ResourceNotFoundException;
import com.insuranceai.backend.policy.entity.Policy;
import com.insuranceai.backend.policy.entity.PolicyStatus;
import com.insuranceai.backend.policy.repository.PolicyRepository;
import com.insuranceai.backend.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.util.UUID;

@Service
public class ClaimServiceImpl implements ClaimService {

    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final ClaimMapper claimMapper;
    private final CurrentUserProvider currentUserProvider;

    public ClaimServiceImpl(ClaimRepository claimRepository,
                             PolicyRepository policyRepository,
                             ClaimMapper claimMapper,
                             CurrentUserProvider currentUserProvider) {
        this.claimRepository = claimRepository;
        this.policyRepository = policyRepository;
        this.claimMapper = claimMapper;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @Transactional
    public ClaimResponseDto create(ClaimRequestDto request) {
        Policy policy = policyRepository.findById(request.policyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + request.policyId()));
        currentUserProvider.assertOwnerOrElevated(policy.getCustomer().getId());

        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            throw new BusinessRuleViolationException("Claims can only be filed against an active policy");
        }
        if (request.claimAmount().compareTo(policy.getCoverageAmount()) > 0) {
            throw new BusinessRuleViolationException("Claim amount cannot exceed the policy coverage amount");
        }

        Claim claim = claimMapper.toEntity(request);
        claim.setPolicy(policy);
        claim.setClaimNumber(generateClaimNumber());
        claim.setStatus(ClaimStatus.SUBMITTED);
        claim.setFiledAt(Instant.now());

        claim = claimRepository.save(claim);
        return claimMapper.toResponseDto(claim);
    }

    @Override
    @Transactional(readOnly = true)
    public ClaimResponseDto getById(UUID id) {
        Claim claim = findClaimOrThrow(id);
        currentUserProvider.assertOwnerOrElevated(claim.getPolicy().getCustomer().getId());
        return claimMapper.toResponseDto(claim);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClaimResponseDto> getByPolicy(UUID policyId, Pageable pageable) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));
        currentUserProvider.assertOwnerOrElevated(policy.getCustomer().getId());
        return claimRepository.findByPolicyId(policyId, pageable).map(claimMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClaimResponseDto> getAll(Pageable pageable) {
        return claimRepository.findAll(pageable).map(claimMapper::toResponseDto);
    }

    @Override
    @Transactional
    public ClaimResponseDto review(UUID id) {
        Claim claim = findClaimOrThrow(id);
        transitionStatus(claim, ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW);
        claim = claimRepository.save(claim);
        return claimMapper.toResponseDto(claim);
    }

    @Override
    @Transactional
    public ClaimResponseDto approve(UUID id, ClaimApproveRequestDto request) {
        Claim claim = findClaimOrThrow(id);
        transitionStatus(claim, ClaimStatus.UNDER_REVIEW, ClaimStatus.APPROVED);

        if (request.approvedAmount().compareTo(claim.getClaimAmount()) > 0) {
            throw new BusinessRuleViolationException("Approved amount cannot exceed the claim amount");
        }

        BigDecimal alreadyApproved = claimRepository.sumApprovedAmountByPolicyId(claim.getPolicy().getId());
        BigDecimal coverageAmount = claim.getPolicy().getCoverageAmount();
        if (alreadyApproved.add(request.approvedAmount()).compareTo(coverageAmount) > 0) {
            throw new BusinessRuleViolationException("Total approved claims would exceed the policy coverage amount");
        }

        claim.setApprovedAmount(request.approvedAmount());
        claim.setResolvedAt(Instant.now());
        claim = claimRepository.save(claim);
        return claimMapper.toResponseDto(claim);
    }

    @Override
    @Transactional
    public ClaimResponseDto reject(UUID id, ClaimRejectRequestDto request) {
        Claim claim = findClaimOrThrow(id);
        if (claim.getStatus() != ClaimStatus.SUBMITTED && claim.getStatus() != ClaimStatus.UNDER_REVIEW) {
            throw new BusinessRuleViolationException("Only submitted or under-review claims can be rejected");
        }
        claim.setStatus(ClaimStatus.REJECTED);
        claim.setRejectionReason(request.reason());
        claim.setResolvedAt(Instant.now());
        claim = claimRepository.save(claim);
        return claimMapper.toResponseDto(claim);
    }

    @Override
    @Transactional
    public ClaimResponseDto markPaid(UUID id) {
        Claim claim = findClaimOrThrow(id);
        transitionStatus(claim, ClaimStatus.APPROVED, ClaimStatus.PAID);
        claim = claimRepository.save(claim);
        return claimMapper.toResponseDto(claim);
    }

    private void transitionStatus(Claim claim, ClaimStatus expectedCurrent, ClaimStatus next) {
        if (claim.getStatus() != expectedCurrent) {
            throw new BusinessRuleViolationException(
                    "Cannot move claim from %s to %s".formatted(claim.getStatus(), next));
        }
        claim.setStatus(next);
    }

    private Claim findClaimOrThrow(UUID id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found: " + id));
    }

    private String generateClaimNumber() {
        return "CLM-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
