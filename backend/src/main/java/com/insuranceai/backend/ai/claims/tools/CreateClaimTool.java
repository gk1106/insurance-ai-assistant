package com.insuranceai.backend.ai.claims.tools;

import com.insuranceai.backend.ai.claims.ClaimsAgentService.ToolContext;
import com.insuranceai.backend.claim.dto.ClaimRequestDto;
import com.insuranceai.backend.claim.dto.ClaimResponseDto;
import com.insuranceai.backend.claim.service.ClaimService;
import com.insuranceai.backend.common.exception.BusinessRuleViolationException;
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
 * Wraps {@link ClaimService#create} only. Arguments are parsed into the real
 * {@link ClaimRequestDto} and validated with the same Bean Validation annotations the REST
 * endpoint relies on, before the existing service (with all its business rules -- server-generated
 * claim number, policy-must-be-ACTIVE, claim-amount-within-coverage, etc.) ever sees them. Claim
 * filing has no role restriction (any authenticated caller may file one), but ownership is still
 * enforced inside {@link ClaimService#create} itself (a CUSTOMER can only file against their own
 * policy) -- so, as with the read tools, this tool adds no authorization logic of its own.
 */
public class CreateClaimTool {

    private static final Logger log = LoggerFactory.getLogger(CreateClaimTool.class);

    private final ClaimService claimService;
    private final Validator validator;
    private final ToolContext context;

    public CreateClaimTool(ClaimService claimService, Validator validator, ToolContext context) {
        this.claimService = claimService;
        this.validator = validator;
        this.context = context;
    }

    @Tool(name = "create_claim", description = "File a new insurance claim against a policy. The policy must be "
            + "ACTIVE and the claim amount cannot exceed the policy's coverage amount. Incident date must be "
            + "ISO-8601 (YYYY-MM-DD) and not in the future.")
    public ClaimResponseDto createClaim(
            @ToolParam(description = "Policy id (UUID) this claim is filed against") String policyId,
            @ToolParam(description = "Claim amount, a positive number, must not exceed the policy's coverage amount")
            Double claimAmount,
            @ToolParam(description = "Date the incident occurred, ISO-8601 (YYYY-MM-DD), must not be in the future")
            String incidentDate,
            @ToolParam(description = "Description of the incident, up to 2000 characters") String description) {

        UserPrincipal principal = context.getPrincipal();
        log.info("tool=create_claim caller={} role={} args=[policyId={}, claimAmount={}, incidentDate={}]",
                principal.getUsername(), principal.getRole(), policyId, claimAmount, incidentDate);

        try {
            ClaimRequestDto request = new ClaimRequestDto(
                    UUID.fromString(policyId),
                    BigDecimal.valueOf(claimAmount),
                    LocalDate.parse(incidentDate),
                    description
            );
            validate(request);

            ClaimResponseDto result = claimService.create(request);
            context.setLastClaim(result);
            log.info("tool=create_claim caller={} outcome=success claimNumber={}",
                    principal.getUsername(), result.claimNumber());
            return result;
        } catch (RuntimeException ex) {
            log.warn("tool=create_claim caller={} outcome=error message={}",
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
            throw new BusinessRuleViolationException("Invalid claim data: " + message);
        }
    }
}
