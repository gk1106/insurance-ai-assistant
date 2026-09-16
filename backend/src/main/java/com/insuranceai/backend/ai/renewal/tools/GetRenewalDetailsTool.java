package com.insuranceai.backend.ai.renewal.tools;

import com.insuranceai.backend.ai.guardrail.ToolArgumentGuard;
import com.insuranceai.backend.ai.observability.ToolTimer;
import com.insuranceai.backend.ai.renewal.RenewalAgentService.ToolContext;
import com.insuranceai.backend.renewal.dto.RenewalResponseDto;
import com.insuranceai.backend.renewal.service.RenewalService;
import com.insuranceai.backend.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Wraps {@link RenewalService#getById} only. It already enforces ownership
 * (CurrentUserProvider.assertOwnerOrElevated) inside the service itself, from the ambient
 * SecurityContext of the current request. Renewals have no business-key lookup (no "renewal
 * number" the way policies/claims have), so only the id (UUID) is supported.
 */
public class GetRenewalDetailsTool {

    private static final Logger log = LoggerFactory.getLogger(GetRenewalDetailsTool.class);

    private final RenewalService renewalService;
    private final ToolContext context;

    public GetRenewalDetailsTool(RenewalService renewalService, ToolContext context) {
        this.renewalService = renewalService;
        this.context = context;
    }

    @Tool(name = "get_renewal_details", description = "Get full details for one renewal by its id. Call this "
            + "when the user asks about a specific, already-identified renewal.")
    public RenewalResponseDto getRenewalDetails(
            @ToolParam(description = "Renewal id (UUID)") String renewalId) {

        UserPrincipal principal = context.getPrincipal();
        log.info("tool=get_renewal_details caller={} role={} args=[renewalId={}]",
                principal.getUsername(), principal.getRole(), renewalId);

        long startNanos = ToolTimer.start();
        try {
            RenewalResponseDto result = renewalService.getById(ToolArgumentGuard.requireUuid(renewalId, "renewalId"));
            context.setLastRenewal(result);
            log.info("tool=get_renewal_details caller={} durationMs={} outcome=success renewalId={} status={}",
                    principal.getUsername(), ToolTimer.elapsedMs(startNanos), result.id(), result.status());
            return result;
        } catch (RuntimeException ex) {
            log.warn("tool=get_renewal_details caller={} durationMs={} outcome=error message={}",
                    principal.getUsername(), ToolTimer.elapsedMs(startNanos), ex.getMessage());
            throw ex;
        }
    }
}
