package com.insuranceai.backend.ai.claims.tools;

import com.insuranceai.backend.ai.claims.ClaimsAgentService.ToolContext;
import com.insuranceai.backend.ai.guardrail.ToolArgumentGuard;
import com.insuranceai.backend.ai.observability.ToolTimer;
import com.insuranceai.backend.claim.dto.ClaimApproveRequestDto;
import com.insuranceai.backend.claim.dto.ClaimRejectRequestDto;
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
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Wraps {@link ClaimService#review}, {@link ClaimService#approve}, {@link ClaimService#reject},
 * and {@link ClaimService#markPaid} -- the claim domain has no single generic "update" method the
 * way {@code PolicyService#update} does (a filed claim's amount/description/policy can't be
 * edited; it only moves forward through a review lifecycle). This tool is the single "update_claim"
 * entry point the agent design calls for, and dispatches to whichever of those four existing
 * service methods the requested action needs -- no new service logic was added to support it.
 * Authorization (ADMIN/AGENT only) is enforced by {@code @PreAuthorize} on each of those
 * ClaimService methods itself, so a CUSTOMER-role caller is rejected by the service, not by
 * anything in this tool.
 */
public class UpdateClaimTool {

    private static final Logger log = LoggerFactory.getLogger(UpdateClaimTool.class);

    private final ClaimService claimService;
    private final Validator validator;
    private final ToolContext context;

    public UpdateClaimTool(ClaimService claimService, Validator validator, ToolContext context) {
        this.claimService = claimService;
        this.validator = validator;
        this.context = context;
    }

    @Tool(name = "update_claim", description = "Progress a claim through its review lifecycle. Only staff "
            + "(ADMIN/AGENT) accounts may do this; a CUSTOMER-role caller will receive an authorization error. "
            + "action must be one of: REVIEW (move a SUBMITTED claim to UNDER_REVIEW), APPROVE (move an "
            + "UNDER_REVIEW claim to APPROVED -- requires approvedAmount), REJECT (move a SUBMITTED or "
            + "UNDER_REVIEW claim to REJECTED -- requires reason), PAY (move an APPROVED claim to PAID). Claims "
            + "can only move forward through that order -- look up the claim's current status first with "
            + "get_claim_details or check_claim_status if you're not sure which action applies.")
    public ClaimResponseDto updateClaim(
            @ToolParam(description = "Claim id (UUID) to update") String claimId,
            @ToolParam(description = "Action to perform: REVIEW, APPROVE, REJECT, or PAY") String action,
            @ToolParam(description = "Required when action=APPROVE: the approved amount, a positive number not "
                    + "exceeding the claim amount", required = false) Double approvedAmount,
            @ToolParam(description = "Required when action=REJECT: the reason for rejection", required = false)
            String reason) {

        UserPrincipal principal = context.getPrincipal();
        log.info("tool=update_claim caller={} role={} args=[claimId={}, action={}, approvedAmount={}, reason={}]",
                principal.getUsername(), principal.getRole(), claimId, action, approvedAmount, reason);

        long startNanos = ToolTimer.start();
        try {
            UUID id = ToolArgumentGuard.requireUuid(claimId, "claimId");
            ClaimResponseDto result = switch (parseAction(action)) {
                case REVIEW -> claimService.review(id);
                case APPROVE -> {
                    if (approvedAmount == null) {
                        throw new IllegalArgumentException("approvedAmount is required when action=APPROVE");
                    }
                    ClaimApproveRequestDto request = new ClaimApproveRequestDto(BigDecimal.valueOf(approvedAmount));
                    validate(request);
                    yield claimService.approve(id, request);
                }
                case REJECT -> {
                    if (reason == null || reason.isBlank()) {
                        throw new IllegalArgumentException("reason is required when action=REJECT");
                    }
                    ClaimRejectRequestDto request = new ClaimRejectRequestDto(reason);
                    validate(request);
                    yield claimService.reject(id, request);
                }
                case PAY -> claimService.markPaid(id);
            };

            context.setLastClaim(result);
            log.info("tool=update_claim caller={} durationMs={} outcome=success claimNumber={} status={}",
                    principal.getUsername(), ToolTimer.elapsedMs(startNanos), result.claimNumber(), result.status());
            return result;
        } catch (RuntimeException ex) {
            log.warn("tool=update_claim caller={} durationMs={} outcome=error message={}",
                    principal.getUsername(), ToolTimer.elapsedMs(startNanos), ex.getMessage());
            throw ex;
        }
    }

    private enum ClaimAction {
        REVIEW, APPROVE, REJECT, PAY
    }

    private ClaimAction parseAction(String value) {
        try {
            return ClaimAction.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException(
                    "Invalid action '" + value + "'. Valid values: " + Arrays.toString(ClaimAction.values()));
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
