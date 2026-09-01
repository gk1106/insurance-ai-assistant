package com.insuranceai.backend.policy.service.impl;

import com.insuranceai.backend.common.exception.BusinessRuleViolationException;
import com.insuranceai.backend.customer.repository.CustomerRepository;
import com.insuranceai.backend.policy.dto.PolicyRequestDto;
import com.insuranceai.backend.policy.entity.Policy;
import com.insuranceai.backend.policy.entity.PolicyStatus;
import com.insuranceai.backend.policy.entity.PolicyType;
import com.insuranceai.backend.policy.mapper.PolicyMapper;
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

class PolicyServiceImplTest {

    private final PolicyRepository policyRepository = mock(PolicyRepository.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final PolicyMapper policyMapper = mock(PolicyMapper.class);
    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);

    private final PolicyServiceImpl policyService =
            new PolicyServiceImpl(policyRepository, customerRepository, policyMapper, currentUserProvider);

    @Test
    void create_throwsWhenEndDateNotAfterStartDate() {
        PolicyRequestDto request = new PolicyRequestDto(
                UUID.randomUUID(), PolicyType.AUTO, new BigDecimal("1000"), new BigDecimal("100"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> policyService.create(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("end date must be after");
    }

    @Test
    void cancel_throwsWhenAlreadyCancelled() {
        UUID policyId = UUID.randomUUID();
        Policy policy = new Policy();
        policy.setStatus(PolicyStatus.CANCELLED);
        when(policyRepository.findById(policyId)).thenReturn(Optional.of(policy));

        assertThatThrownBy(() -> policyService.cancel(policyId))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
