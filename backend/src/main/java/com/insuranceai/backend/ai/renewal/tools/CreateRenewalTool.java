package com.insuranceai.backend.ai.renewal.tools;

import com.insuranceai.backend.ai.renewal.RenewalAgentService.ToolContext;
import com.insuranceai.backend.common.exception.BusinessRuleViolationException;
import com.insuranceai.backend.renewal.dto.RenewalRequestDto;
import com.insuranceai.backend.renewal.dto.RenewalResponseDto;
import com.insuranceai.backend.renewal.service.RenewalService;
import com.insuranceai.backend.security.UserPrincipal;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Wraps {@link RenewalService#create} only. Arguments are parsed into the real
 * {@link RenewalRequestDto} and validated with the same Bean Validation annotations the REST
 * endpoint relies on, before the existing service (with all its business rules -- policy must be
 * ACTIVE or EXPIRED within the 30-day grace period, newEndDate after the current end date, etc.)
 * ever sees them. Renewal requests have no role restriction (any authenticated caller may request
 * one), but ownership is still enforced inside {@link RenewalService#create} itself -- so, as with
 * the read tools, this tool adds no authorization logic of its own.
 */
public class CreateRenewalTool {

    private static final Logger log = LoggerFactory.getLogger(CreateRenewalTool.class);

    private final RenewalService renewalService;
    private final Validator validator;
    private final ToolContext context;

    public CreateRenewalTool(RenewalService renewalService, Validator validator, ToolContext context) {
        this.renewalService = renewalService;
        this.validator = validator;
        this.context = context;
    }

    @Tool(name = "create_renewal", description = "Request a renewal for a policy. The policy must be ACTIVE, or "
            + "EXPIRED within a 30-day grace period. newEndDate must be after the policy's current end date. "
            + "Dates must be ISO-8601 (YYYY-MM-DD).")
    public RenewalResponseDto createRenewal(
            @ToolParam(description = "Policy id (UUID) to renew") String policyId,
            @ToolParam(description = "Requested new end date, ISO-8601 (YYYY-MM-DD), must be after the policy's "
                    + "current end date") String newEndDate,
            @ToolParam(description = "Revised premium amount for the renewed term, a positive number")
            Double revisedPremiumAmount) {

        UserPrincipal principal = context.getPrincipal();
        log.info("tool=create_renewal caller={} role={} args=[policyId={}, newEndDate={}, revisedPremiumAmount={}]",
                principal.getUsername(), principal.getRole(), policyId, newEndDate, revisedPremiumAmount);

        try {
            RenewalRequestDto request = new RenewalRequestDto(
                    UUID.fromString(policyId),
                    LocalDate.parse(newEndDate),
                    BigDecimal.valueOf(revisedPremiumAmount)
            );
            validate(request);

            RenewalResponseDto result = renewalService.create(request);
            context.setLastRenewal(result);
            log.info("tool=create_renewal caller={} outcome=success renewalId={}",
                    principal.getUsername(), result.id());
            return result;
        } catch (RuntimeException ex) {
            log.warn("tool=create_renewal caller={} outcome=error message={}",
                    principal.getUsername(), ex.getMessage());
            throw ex;
        }
    }

    private <T> void validate(T request) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new BusinessRuleViolationException("Invalid renewal data: " + message);
        }
    }
}
