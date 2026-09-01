package com.insuranceai.backend.renewal.service.impl;

import com.insuranceai.backend.common.exception.BusinessRuleViolationException;
import com.insuranceai.backend.common.exception.ResourceNotFoundException;
import com.insuranceai.backend.policy.entity.Policy;
import com.insuranceai.backend.policy.entity.PolicyStatus;
import com.insuranceai.backend.policy.repository.PolicyRepository;
import com.insuranceai.backend.policy.service.PolicyService;
import com.insuranceai.backend.renewal.dto.RenewalRequestDto;
import com.insuranceai.backend.renewal.dto.RenewalResponseDto;
import com.insuranceai.backend.renewal.entity.Renewal;
import com.insuranceai.backend.renewal.entity.RenewalStatus;
import com.insuranceai.backend.renewal.mapper.RenewalMapper;
import com.insuranceai.backend.renewal.repository.RenewalRepository;
import com.insuranceai.backend.renewal.service.RenewalService;
import com.insuranceai.backend.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class RenewalServiceImpl implements RenewalService {

    private static final int GRACE_PERIOD_DAYS = 30;

    private final RenewalRepository renewalRepository;
    private final PolicyRepository policyRepository;
    private final PolicyService policyService;
    private final RenewalMapper renewalMapper;
    private final CurrentUserProvider currentUserProvider;

    public RenewalServiceImpl(RenewalRepository renewalRepository,
                               PolicyRepository policyRepository,
                               PolicyService policyService,
                               RenewalMapper renewalMapper,
                               CurrentUserProvider currentUserProvider) {
        this.renewalRepository = renewalRepository;
        this.policyRepository = policyRepository;
        this.policyService = policyService;
        this.renewalMapper = renewalMapper;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @Transactional
    public RenewalResponseDto create(RenewalRequestDto request) {
        Policy policy = policyRepository.findById(request.policyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + request.policyId()));
        currentUserProvider.assertOwnerOrElevated(policy.getCustomer().getId());

        boolean withinGracePeriod = policy.getStatus() == PolicyStatus.EXPIRED
                && !LocalDate.now().isAfter(policy.getEndDate().plusDays(GRACE_PERIOD_DAYS));
        if (policy.getStatus() != PolicyStatus.ACTIVE && !withinGracePeriod) {
            throw new BusinessRuleViolationException(
                    "Policy cannot be renewed: it is not active and is outside the renewal grace period");
        }
        if (!request.newEndDate().isAfter(policy.getEndDate())) {
            throw new BusinessRuleViolationException("New end date must be after the current policy end date");
        }

        Renewal renewal = renewalMapper.toEntity(request);
        renewal.setPolicy(policy);
        renewal.setPreviousEndDate(policy.getEndDate());
        renewal.setStatus(RenewalStatus.PENDING);
        renewal.setRequestedAt(Instant.now());

        renewal = renewalRepository.save(renewal);
        return renewalMapper.toResponseDto(renewal);
    }

    @Override
    @Transactional(readOnly = true)
    public RenewalResponseDto getById(UUID id) {
        Renewal renewal = findRenewalOrThrow(id);
        currentUserProvider.assertOwnerOrElevated(renewal.getPolicy().getCustomer().getId());
        return renewalMapper.toResponseDto(renewal);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RenewalResponseDto> getByPolicy(UUID policyId, Pageable pageable) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));
        currentUserProvider.assertOwnerOrElevated(policy.getCustomer().getId());
        return renewalRepository.findByPolicyId(policyId, pageable).map(renewalMapper::toResponseDto);
    }

    @Override
    @Transactional
    public RenewalResponseDto confirm(UUID id) {
        Renewal renewal = findRenewalOrThrow(id);
        if (renewal.getStatus() != RenewalStatus.PENDING) {
            throw new BusinessRuleViolationException("Only pending renewals can be confirmed");
        }
        renewal.setStatus(RenewalStatus.CONFIRMED);
        renewal.setDecidedAt(Instant.now());
        renewal = renewalRepository.save(renewal);

        policyService.applyRenewal(renewal.getPolicy().getId(), renewal.getNewEndDate(), renewal.getRevisedPremiumAmount());
        return renewalMapper.toResponseDto(renewal);
    }

    @Override
    @Transactional
    public RenewalResponseDto reject(UUID id) {
        Renewal renewal = findRenewalOrThrow(id);
        if (renewal.getStatus() != RenewalStatus.PENDING) {
            throw new BusinessRuleViolationException("Only pending renewals can be rejected");
        }
        renewal.setStatus(RenewalStatus.REJECTED);
        renewal.setDecidedAt(Instant.now());
        renewal = renewalRepository.save(renewal);
        return renewalMapper.toResponseDto(renewal);
    }

    private Renewal findRenewalOrThrow(UUID id) {
        return renewalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Renewal not found: " + id));
    }
}
