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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Wraps {@link RenewalService#getByPolicy} only -- never the repository. Unlike policies and
 * claims, renewals have no "list all" on the service at all (not even for staff), so policyId is
 * always required here. Ownership is enforced inside {@code RenewalServiceImpl.getByPolicy} itself,
 * from the ambient SecurityContext, exactly like the other agents' per-policy read tools.
 */
public class SearchRenewalsTool {

    private static final Logger log = LoggerFactory.getLogger(SearchRenewalsTool.class);

    private final RenewalService renewalService;
    private final ToolContext context;

    public SearchRenewalsTool(RenewalService renewalService, ToolContext context) {
        this.renewalService = renewalService;
        this.context = context;
    }

    @Tool(name = "search_renewals", description = "Search renewal requests for a specific policy. policyId is "
            + "required -- there is no combined 'all renewals' listing for any caller, renewals are always "
            + "looked up per policy. If you don't have a policyId, ask the user which policy. Call this when "
            + "the user asks to list, browse, or find renewals.")
    public List<RenewalResponseDto> searchRenewals(
            @ToolParam(description = "Policy id (UUID) to list renewals for") String policyId,
            @ToolParam(description = "Zero-based page number, defaults to 0", required = false) Integer page,
            @ToolParam(description = "Page size, defaults to 20", required = false) Integer size) {

        UserPrincipal principal = context.getPrincipal();
        log.info("tool=search_renewals caller={} role={} args=[policyId={}, page={}, size={}]",
                principal.getUsername(), principal.getRole(), policyId, page, size);

        long startNanos = ToolTimer.start();
        try {
            UUID id = ToolArgumentGuard.requireUuid(policyId, "policyId");
            Pageable pageable = ToolArgumentGuard.pageable(page, size);
            Page<RenewalResponseDto> result = renewalService.getByPolicy(id, pageable);

            context.setLastSearchResults(result.getContent());
            log.info("tool=search_renewals caller={} durationMs={} outcome=success count={}",
                    principal.getUsername(), ToolTimer.elapsedMs(startNanos), result.getContent().size());
            return result.getContent();
        } catch (RuntimeException ex) {
            log.warn("tool=search_renewals caller={} durationMs={} outcome=error message={}",
                    principal.getUsername(), ToolTimer.elapsedMs(startNanos), ex.getMessage());
            throw ex;
        }
    }
}
