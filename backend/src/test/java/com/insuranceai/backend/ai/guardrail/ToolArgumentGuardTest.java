package com.insuranceai.backend.ai.guardrail;

import com.insuranceai.backend.common.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolArgumentGuardTest {

    @Test
    void requireUuid_parsesAValidUuid() {
        UUID id = UUID.randomUUID();
        assertThat(ToolArgumentGuard.requireUuid(id.toString(), "policyId")).isEqualTo(id);
    }

    @Test
    void requireUuid_rejectsMalformedValueWithCleanMessage() {
        assertThatThrownBy(() -> ToolArgumentGuard.requireUuid("not-a-uuid", "policyId"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("policyId")
                .hasMessageContaining("not-a-uuid")
                .hasMessageNotContaining("IllegalArgumentException");
    }

    @Test
    void requireUuid_rejectsBlankValue() {
        assertThatThrownBy(() -> ToolArgumentGuard.requireUuid("  ", "claimId"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("claimId is required");
    }

    @Test
    void requireUuid_rejectsNullValue() {
        assertThatThrownBy(() -> ToolArgumentGuard.requireUuid(null, "renewalId"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("renewalId is required");
    }

    @Test
    void optionalUuid_returnsEmptyForBlank() {
        assertThat(ToolArgumentGuard.optionalUuid(null, "customerId")).isEqualTo(Optional.empty());
        assertThat(ToolArgumentGuard.optionalUuid("", "customerId")).isEqualTo(Optional.empty());
    }

    @Test
    void optionalUuid_parsesWhenPresent() {
        UUID id = UUID.randomUUID();
        assertThat(ToolArgumentGuard.optionalUuid(id.toString(), "customerId")).contains(id);
    }

    @Test
    void pageable_usesDefaultsWhenArgumentsAreNull() {
        Pageable pageable = ToolArgumentGuard.pageable(null, null);
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
    }

    @Test
    void pageable_clampsOversizedPageSize() {
        Pageable pageable = ToolArgumentGuard.pageable(0, 999_999);
        assertThat(pageable.getPageSize()).isEqualTo(100);
    }

    @Test
    void pageable_ignoresNegativePageAndSize() {
        Pageable pageable = ToolArgumentGuard.pageable(-5, -5);
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
    }
}
