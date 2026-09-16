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
import java.util.stream.Collectors;

/**
 * Wraps {@link PolicyService#create} only. Arguments are parsed into the real
 * {@link PolicyRequestDto} and validated with the same Bean Validation annotations the REST
 * endpoint relies on, before the existing service (with all its business rules -- server-generated
 * policy number, endDate-after-startDate, etc.) ever sees them. Authorization (ADMIN/AGENT only) is
 * enforced by {@code @PreAuthorize} on {@link PolicyService#create} itself, so a CUSTOMER-role
 * caller is rejected by the service, not by anything in this tool.
 */
public class CreatePolicyTool {

    private static final Logger log = LoggerFactory.getLogger(CreatePolicyTool.class);

    private final PolicyService policyService;
    private final Validator validator;
    private final ToolContext context;

    public CreatePolicyTool(PolicyService policyService, Validator validator, ToolContext context) {
        this.policyService = policyService;
        this.validator = validator;
        this.context = context;
    }

    @Tool(name = "create_policy", description = "Create a new insurance policy for a customer. Only staff "
            + "(ADMIN/AGENT) accounts may create policies; a CUSTOMER-role caller will receive an authorization "
            + "error. Dates must be ISO-8601 (YYYY-MM-DD) and endDate must be after startDate.")
    public PolicyResponseDto createPolicy(
            @ToolParam(description = "Customer id (UUID) this policy belongs to") String customerId,
            @ToolParam(description = "Policy type: AUTO, HEALTH, HOME, LIFE, or TRAVEL") String policyType,
            @ToolParam(description = "Coverage amount, a positive number") Double coverageAmount,
            @ToolParam(description = "Premium amount, a positive number") Double premiumAmount,
            @ToolParam(description = "Policy start date, ISO-8601 (YYYY-MM-DD)") String startDate,
            @ToolParam(description = "Policy end date, ISO-8601 (YYYY-MM-DD), must be after startDate") String endDate) {

        UserPrincipal principal = context.getPrincipal();
        log.info("tool=create_policy caller={} role={} args=[customerId={}, policyType={}, coverageAmount={}, "
                        + "premiumAmount={}, startDate={}, endDate={}]",
                principal.getUsername(), principal.getRole(), customerId, policyType, coverageAmount,
                premiumAmount, startDate, endDate);

        long startNanos = ToolTimer.start();
        try {
            PolicyRequestDto request = new PolicyRequestDto(
                    ToolArgumentGuard.requireUuid(customerId, "customerId"),
                    parsePolicyType(policyType),
                    BigDecimal.valueOf(coverageAmount),
                    BigDecimal.valueOf(premiumAmount),
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate)
            );
            validate(request);

            PolicyResponseDto result = policyService.create(request);
            context.setLastPolicy(result);
            log.info("tool=create_policy caller={} durationMs={} outcome=success policyNumber={}",
                    principal.getUsername(), ToolTimer.elapsedMs(startNanos), result.policyNumber());
            return result;
        } catch (RuntimeException ex) {
            log.warn("tool=create_policy caller={} durationMs={} outcome=error message={}",
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
