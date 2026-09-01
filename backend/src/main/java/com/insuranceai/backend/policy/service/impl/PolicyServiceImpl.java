package com.insuranceai.backend.policy.service.impl;

import com.insuranceai.backend.common.exception.BusinessRuleViolationException;
import com.insuranceai.backend.common.exception.ResourceNotFoundException;
import com.insuranceai.backend.customer.entity.Customer;
import com.insuranceai.backend.customer.repository.CustomerRepository;
import com.insuranceai.backend.policy.dto.PolicyRequestDto;
import com.insuranceai.backend.policy.dto.PolicyResponseDto;
import com.insuranceai.backend.policy.entity.Policy;
import com.insuranceai.backend.policy.entity.PolicyStatus;
import com.insuranceai.backend.policy.mapper.PolicyMapper;
import com.insuranceai.backend.policy.repository.PolicyRepository;
import com.insuranceai.backend.policy.service.PolicyService;
import com.insuranceai.backend.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository policyRepository;
    private final CustomerRepository customerRepository;
    private final PolicyMapper policyMapper;
    private final CurrentUserProvider currentUserProvider;

    public PolicyServiceImpl(PolicyRepository policyRepository,
                              CustomerRepository customerRepository,
                              PolicyMapper policyMapper,
                              CurrentUserProvider currentUserProvider) {
        this.policyRepository = policyRepository;
        this.customerRepository = customerRepository;
        this.policyMapper = policyMapper;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @Transactional
    public PolicyResponseDto create(PolicyRequestDto request) {
        validateDateRange(request.startDate(), request.endDate());

        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));

        Policy policy = policyMapper.toEntity(request);
        policy.setCustomer(customer);
        policy.setPolicyNumber(generatePolicyNumber());
        policy.setStatus(PolicyStatus.ACTIVE);

        policy = policyRepository.save(policy);
        return policyMapper.toResponseDto(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyResponseDto getById(UUID id) {
        Policy policy = findPolicyOrThrow(id);
        currentUserProvider.assertOwnerOrElevated(policy.getCustomer().getId());
        return policyMapper.toResponseDto(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyResponseDto getByPolicyNumber(String policyNumber) {
        Policy policy = policyRepository.findByPolicyNumber(policyNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyNumber));
        currentUserProvider.assertOwnerOrElevated(policy.getCustomer().getId());
        return policyMapper.toResponseDto(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PolicyResponseDto> getAll(Pageable pageable) {
        return policyRepository.findAll(pageable).map(policyMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PolicyResponseDto> getByCustomer(UUID customerId, Pageable pageable) {
        currentUserProvider.assertOwnerOrElevated(customerId);
        return policyRepository.findByCustomerId(customerId, pageable).map(policyMapper::toResponseDto);
    }

    @Override
    @Transactional
    public PolicyResponseDto update(UUID id, PolicyRequestDto request) {
        validateDateRange(request.startDate(), request.endDate());

        Policy policy = findPolicyOrThrow(id);
        policy.setPolicyType(request.policyType());
        policy.setCoverageAmount(request.coverageAmount());
        policy.setPremiumAmount(request.premiumAmount());
        policy.setStartDate(request.startDate());
        policy.setEndDate(request.endDate());

        policy = policyRepository.save(policy);
        return policyMapper.toResponseDto(policy);
    }

    @Override
    @Transactional
    public PolicyResponseDto cancel(UUID id) {
        Policy policy = findPolicyOrThrow(id);
        if (policy.getStatus() == PolicyStatus.CANCELLED) {
            throw new BusinessRuleViolationException("Policy is already cancelled");
        }
        policy.setStatus(PolicyStatus.CANCELLED);
        policy = policyRepository.save(policy);
        return policyMapper.toResponseDto(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyResponseDto> getExpiringWithin(int days) {
        LocalDate today = LocalDate.now();
        return policyRepository.findByEndDateBetween(today, today.plusDays(days)).stream()
                .map(policyMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public PolicyResponseDto applyRenewal(UUID policyId, LocalDate newEndDate, BigDecimal revisedPremiumAmount) {
        Policy policy = findPolicyOrThrow(policyId);
        policy.setEndDate(newEndDate);
        policy.setPremiumAmount(revisedPremiumAmount);
        policy.setStatus(PolicyStatus.ACTIVE);
        policy = policyRepository.save(policy);
        return policyMapper.toResponseDto(policy);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (!endDate.isAfter(startDate)) {
            throw new BusinessRuleViolationException("Policy end date must be after start date");
        }
    }

    private Policy findPolicyOrThrow(UUID id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + id));
    }

    private String generatePolicyNumber() {
        return "POL-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
