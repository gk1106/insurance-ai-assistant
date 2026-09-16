package com.insuranceai.backend.ai.mcp.tools;

import com.insuranceai.backend.ai.guardrail.ToolArgumentGuard;
import com.insuranceai.backend.renewal.dto.RenewalResponseDto;
import com.insuranceai.backend.renewal.service.RenewalService;
import com.insuranceai.backend.security.CurrentUserProvider;
import com.insuranceai.backend.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Read-only MCP tools over {@link RenewalService} -- the same two calls
 * {@code SearchRenewalsTool}/{@code GetRenewalDetailsTool} make for the Renewal Agent, never the
 * repository. See {@link PolicyMcpTools} for why this is a singleton bean (no per-turn
 * ToolContext) reading the caller from {@link CurrentUserProvider} directly. Authorization and
 * ownership are entirely RenewalService's and CurrentUserProvider's.
 */
@Component
public class RenewalMcpTools {

    private static final Logger log = LoggerFactory.getLogger(RenewalMcpTools.class);

    private final RenewalService renewalService;
    private final CurrentUserProvider currentUserProvider;

    public RenewalMcpTools(RenewalService renewalService, CurrentUserProvider currentUserProvider) {
        this.renewalService = renewalService;
        this.currentUserProvider = currentUserProvider;
    }

    @Tool(name = "search_renewals", description = "Search renewal requests for a specific policy. policyId is "
            + "required -- renewals are always looked up per policy, there is no combined 'all renewals' listing "
            + "for any caller.")
    public List<RenewalResponseDto> searchRenewals(
            @ToolParam(description = "Policy id (UUID) to list renewals for") String policyId,
            @ToolParam(description = "Zero-based page number, defaults to 0", required = false) Integer page,
            @ToolParam(description = "Page size, defaults to 20", required = false) Integer size) {

        UserPrincipal principal = currentPrincipal();
        log.info("mcp-tool=search_renewals caller={} role={} args=[policyId={}, page={}, size={}]",
                principal.getUsername(), principal.getRole(), policyId, page, size);

        try {
            UUID id = ToolArgumentGuard.requireUuid(policyId, "policyId");
            Pageable pageable = ToolArgumentGuard.pageable(page, size);
            Page<RenewalResponseDto> result = renewalService.getByPolicy(id, pageable);

            log.info("mcp-tool=search_renewals caller={} outcome=success count={}",
                    principal.getUsername(), result.getContent().size());
            return result.getContent();
        } catch (RuntimeException ex) {
            log.warn("mcp-tool=search_renewals caller={} outcome=error message={}",
                    principal.getUsername(), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "get_renewal_details", description = "Get full details for one renewal by its id.")
    public RenewalResponseDto getRenewalDetails(
            @ToolParam(description = "Renewal id (UUID)") String renewalId) {

        UserPrincipal principal = currentPrincipal();
        log.info("mcp-tool=get_renewal_details caller={} role={} args=[renewalId={}]",
                principal.getUsername(), principal.getRole(), renewalId);

        try {
            RenewalResponseDto result = renewalService.getById(ToolArgumentGuard.requireUuid(renewalId, "renewalId"));
            log.info("mcp-tool=get_renewal_details caller={} outcome=success renewalId={} status={}",
                    principal.getUsername(), result.id(), result.status());
            return result;
        } catch (RuntimeException ex) {
            log.warn("mcp-tool=get_renewal_details caller={} outcome=error message={}",
                    principal.getUsername(), ex.getMessage());
            throw ex;
        }
    }

    private UserPrincipal currentPrincipal() {
        return currentUserProvider.getCurrentPrincipal()
                .orElseThrow(() -> new AccessDeniedException("Authentication required"));
    }
}
