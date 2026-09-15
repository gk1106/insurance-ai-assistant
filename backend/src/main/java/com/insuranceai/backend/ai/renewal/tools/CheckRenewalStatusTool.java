package com.insuranceai.backend.ai.renewal.tools;

import com.insuranceai.backend.ai.renewal.RenewalAgentService.ToolContext;
import com.insuranceai.backend.renewal.dto.RenewalResponseDto;
import com.insuranceai.backend.renewal.entity.RenewalStatus;
import com.insuranceai.backend.renewal.service.RenewalService;
import com.insuranceai.backend.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A narrow, high-frequency projection over {@link RenewalService#getById} -- "has my renewal been
 * confirmed / how long has it been pending" is common enough, and specific enough, to deserve its
 * own tool rather than relying on the model to compute it from get_renewal_details' full response.
 */
public class CheckRenewalStatusTool {

    private static final Logger log = LoggerFactory.getLogger(CheckRenewalStatusTool.class);

    private final RenewalService renewalService;
    private final ToolContext context;

    public CheckRenewalStatusTool(RenewalService renewalService, ToolContext context) {
        this.renewalService = renewalService;
        this.context = context;
    }

    @Tool(name = "check_renewal_status", description = "Check a renewal's current status and whether it has been "
            + "decided yet. Call this when the user asks if a renewal has been confirmed, rejected, or is still "
            + "pending.")
    public RenewalStatusSummary checkRenewalStatus(
            @ToolParam(description = "Renewal id (UUID)") String renewalId) {

        UserPrincipal principal = context.getPrincipal();
        log.info("tool=check_renewal_status caller={} role={} args=[renewalId={}]",
                principal.getUsername(), principal.getRole(), renewalId);

        try {
            RenewalResponseDto renewal = renewalService.getById(UUID.fromString(renewalId));
            context.setLastRenewal(renewal);

            long daysSinceRequested = Duration.between(renewal.requestedAt(), Instant.now()).toDays();
            boolean decided = renewal.decidedAt() != null;

            log.info("tool=check_renewal_status caller={} outcome=success renewalId={} status={}",
                    principal.getUsername(), renewal.id(), renewal.status());
            return new RenewalStatusSummary(renewal.policyNumber(), renewal.status(), renewal.previousEndDate(),
                    renewal.newEndDate(), renewal.revisedPremiumAmount(), decided, daysSinceRequested);
        } catch (RuntimeException ex) {
            log.warn("tool=check_renewal_status caller={} outcome=error message={}",
                    principal.getUsername(), ex.getMessage());
            throw ex;
        }
    }

    public record RenewalStatusSummary(String policyNumber, RenewalStatus status, LocalDate previousEndDate,
                                        LocalDate newEndDate, BigDecimal revisedPremiumAmount, boolean decided,
                                        long daysSinceRequested) {
    }
}
