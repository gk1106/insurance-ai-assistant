package com.insuranceai.backend.renewal.service.impl;

import com.insuranceai.backend.common.exception.BusinessRuleViolationException;
import com.insuranceai.backend.customer.entity.Customer;
import com.insuranceai.backend.policy.entity.Policy;
import com.insuranceai.backend.policy.entity.PolicyStatus;
import com.insuranceai.backend.policy.repository.PolicyRepository;
import com.insuranceai.backend.policy.service.PolicyService;
import com.insuranceai.backend.renewal.dto.RenewalRequestDto;
import com.insuranceai.backend.renewal.mapper.RenewalMapper;
import com.insuranceai.backend.renewal.repository.RenewalRepository;
import com.insuranceai.backend.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RenewalServiceImplTest {

    private final RenewalRepository renewalRepository = mock(RenewalRepository.class);
    private final PolicyRepository policyRepository = mock(PolicyRepository.class);
    private final PolicyService policyService = mock(PolicyService.class);
    private final RenewalMapper renewalMapper = mock(RenewalMapper.class);
    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);

    private final RenewalServiceImpl renewalService = new RenewalServiceImpl(
            renewalRepository, policyRepository, policyService, renewalMapper, currentUserProvider);

    @Test
    void create_throwsWhenPolicyNotActiveAndOutsideGracePeriod() {
        UUID policyId = UUID.randomUUID();
        Policy policy = new Policy();
        policy.setCustomer(new Customer());
        policy.setStatus(PolicyStatus.EXPIRED);
        policy.setEndDate(LocalDate.now().minusDays(60));
        when(policyRepository.findById(policyId)).thenReturn(Optional.of(policy));

        RenewalRequestDto request = new RenewalRequestDto(policyId, LocalDate.now().plusYears(1), new BigDecimal("500"));

        assertThatThrownBy(() -> renewalService.create(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("grace period");
    }

    @Test
    void create_throwsWhenNewEndDateNotAfterCurrentEndDate() {
        UUID policyId = UUID.randomUUID();
        Policy policy = new Policy();
        policy.setCustomer(new Customer());
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setEndDate(LocalDate.now().plusMonths(1));
        when(policyRepository.findById(policyId)).thenReturn(Optional.of(policy));

        RenewalRequestDto request = new RenewalRequestDto(policyId, LocalDate.now(), new BigDecimal("500"));

        assertThatThrownBy(() -> renewalService.create(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("must be after");
    }
}
