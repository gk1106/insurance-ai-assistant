package com.insuranceai.backend.ai.policy.tools;

import com.insuranceai.backend.ai.guardrail.ToolArgumentGuard;
import com.insuranceai.backend.ai.observability.ToolTimer;
import com.insuranceai.backend.ai.policy.PolicyAgentService.ToolContext;
import com.insuranceai.backend.common.exception.BusinessRuleViolationException;
import com.insuranceai.backend.policy.dto.PolicyRequestDto;
import com.insuranceai.backend.policy.dto.PolicyResponseDto;
import com.insuranceai.backend.policy.entity.PolicyType;
import com.insuranceai.backend.policy.service.PolicyService;
import com.insuranceai.backend.security.UserPrincipal;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Wraps {@link PolicyService#update} only. Same parse-then-validate-then-delegate shape as
 * {@link CreatePolicyTool}; authorization (ADMIN/AGENT only) is enforced by {@code @PreAuthorize}
 * on {@link PolicyService#update} itself.
 */
public class UpdatePolicyTool {

    private static final Logger log = LoggerFactory.getLogger(UpdatePolicyTool.class);

    private final PolicyService policyService;
    private final Validator validator;
    private final ToolContext context;

    public UpdatePolicyTool(PolicyService policyService, Validator validator, ToolContext context) {
        this.policyService = policyService;
        this.validator = validator;
        this.context = context;
    }

    @Tool(name = "update_policy", description = "Update an existing insurance policy's type, coverage, premium, "
            + "or dates. Only staff (ADMIN/AGENT) accounts may update policies. All fields are required -- look up "
            + "the policy's current values first with get_policy_details if the user only wants to change one "
            + "field. Dates must be ISO-8601 (YYYY-MM-DD) and endDate must be after startDate.")
    public PolicyResponseDto updatePolicy(
            @ToolParam(description = "Id (UUID) of the policy to update") String policyId,
            @ToolParam(description = "Policy type: AUTO, HEALTH, HOME, LIFE, or TRAVEL") String policyType,
            @ToolParam(description = "Coverage amount, a positive number") Double coverageAmount,
            @ToolParam(description = "Premium amount, a positive number") Double premiumAmount,
            @ToolParam(description = "Policy start date, ISO-8601 (YYYY-MM-DD)") String startDate,
            @ToolParam(description = "Policy end date, ISO-8601 (YYYY-MM-DD), must be after startDate") String endDate) {

        UserPrincipal principal = context.getPrincipal();
        log.info("tool=update_policy caller={} role={} args=[policyId={}, policyType={}, coverageAmount={}, "
                        + "premiumAmount={}, startDate={}, endDate={}]",
                principal.getUsername(), principal.getRole(), policyId, policyType, coverageAmount,
                premiumAmount, startDate, endDate);

        long startNanos = ToolTimer.start();
        try {
            UUID id = ToolArgumentGuard.requireUuid(policyId, "policyId");
            PolicyRequestDto request = new PolicyRequestDto(
                    // customerId isn't mutated by PolicyService.update; the request DTO still
                    // requires the field, so reuse the policy's current owner via a fresh lookup.
                    policyService.getById(id).customerId(),
                    parsePolicyType(policyType),
                    BigDecimal.valueOf(coverageAmount),
                    BigDecimal.valueOf(premiumAmount),
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate)
            );
            validate(request);

            PolicyResponseDto result = policyService.update(id, request);
            context.setLastPolicy(result);
            log.info("tool=update_policy caller={} durationMs={} outcome=success policyNumber={}",
                    principal.getUsername(), ToolTimer.elapsedMs(startNanos), result.policyNumber());
            return result;
        } catch (RuntimeException ex) {
            log.warn("tool=update_policy caller={} durationMs={} outcome=error message={}",
                    principal.getUsername(), ToolTimer.elapsedMs(startNanos), ex.getMessage());
            throw ex;
        }
    }

    private PolicyType parsePolicyType(String value) {
        try {
            return PolicyType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException(
                    "Invalid policy type '" + value + "'. Valid values: " + Arrays.toString(PolicyType.values()));
        }
    }

    private void validate(PolicyRequestDto request) {
        Set<ConstraintViolation<PolicyRequestDto>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new BusinessRuleViolationException("Invalid policy data: " + message);
        }
    }
}
