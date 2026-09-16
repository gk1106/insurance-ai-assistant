package com.insuranceai.backend.ai.claims.tools;

import com.insuranceai.backend.ai.claims.ClaimsAgentService.ToolContext;
import com.insuranceai.backend.ai.guardrail.ToolArgumentGuard;
import com.insuranceai.backend.ai.observability.ToolTimer;
import com.insuranceai.backend.claim.dto.ClaimResponseDto;
import com.insuranceai.backend.claim.entity.ClaimStatus;
import com.insuranceai.backend.claim.service.ClaimService;
import com.insuranceai.backend.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/**
 * A narrow, high-frequency projection over {@link ClaimService#getById} -- "has my claim been
 * approved / how long has it been open" is common enough, and specific enough, to deserve its own
 * tool rather than relying on the model to compute it from get_claim_details' full response.
 */
public class CheckClaimStatusTool {

    private static final Logger log = LoggerFactory.getLogger(CheckClaimStatusTool.class);

    private final ClaimService claimService;
    private final ToolContext context;

    public CheckClaimStatusTool(ClaimService claimService, ToolContext context) {
        this.claimService = claimService;
        this.context = context;
    }

    @Tool(name = "check_claim_status", description = "Check a claim's current status, whether it has been "
            + "resolved yet, and how many days it has been open. Call this when the user asks if a claim has "
            + "been approved, rejected, paid, or is still being processed.")
    public ClaimStatusSummary checkClaimStatus(
            @ToolParam(description = "Claim id (UUID)") String claimId) {

        UserPrincipal principal = context.getPrincipal();
        log.info("tool=check_claim_status caller={} role={} args=[claimId={}]",
                principal.getUsername(), principal.getRole(), claimId);

        long startNanos = ToolTimer.start();
        try {
            ClaimResponseDto claim = claimService.getById(ToolArgumentGuard.requireUuid(claimId, "claimId"));
            context.setLastClaim(claim);

            long daysSinceFiled = Duration.between(claim.filedAt(), Instant.now()).toDays();
            boolean resolved = claim.resolvedAt() != null;

            log.info("tool=check_claim_status caller={} durationMs={} outcome=success claimNumber={} status={}",
                    principal.getUsername(), ToolTimer.elapsedMs(startNanos), claim.claimNumber(), claim.status());
            return new ClaimStatusSummary(claim.claimNumber(), claim.status(), claim.claimAmount(),
                    claim.approvedAmount(), daysSinceFiled, resolved);
        } catch (RuntimeException ex) {
            log.warn("tool=check_claim_status caller={} durationMs={} outcome=error message={}",
                    principal.getUsername(), ToolTimer.elapsedMs(startNanos), ex.getMessage());
            throw ex;
        }
    }

    public record ClaimStatusSummary(String claimNumber, ClaimStatus status, BigDecimal claimAmount,
                                      BigDecimal approvedAmount, long daysSinceFiled, boolean resolved) {
    }
}
