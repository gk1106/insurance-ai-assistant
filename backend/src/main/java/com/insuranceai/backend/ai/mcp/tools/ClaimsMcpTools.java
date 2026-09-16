package com.insuranceai.backend.ai.mcp.tools;

import com.insuranceai.backend.ai.guardrail.ToolArgumentGuard;
import com.insuranceai.backend.auth.entity.Role;
import com.insuranceai.backend.claim.dto.ClaimResponseDto;
import com.insuranceai.backend.claim.service.ClaimService;
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

/**
 * Read-only MCP tools over {@link ClaimService} -- the same two calls
 * {@code SearchClaimsTool}/{@code GetClaimDetailsTool} make for the Claims Agent, never the
 * repository. See {@link PolicyMcpTools} for why this is a singleton bean (no per-turn
 * ToolContext) reading the caller from {@link CurrentUserProvider} directly. Authorization and
 * ownership are entirely ClaimService's and CurrentUserProvider's.
 */
@Component
public class ClaimsMcpTools {

    private static final Logger log = LoggerFactory.getLogger(ClaimsMcpTools.class);

    private final ClaimService claimService;
    private final CurrentUserProvider currentUserProvider;

    public ClaimsMcpTools(ClaimService claimService, CurrentUserProvider currentUserProvider) {
        this.claimService = claimService;
        this.currentUserProvider = currentUserProvider;
    }

    @Tool(name = "search_claims", description = "Search insurance claims. If policyId is given, returns that "
            + "policy's claims. If policyId is omitted, staff (ADMIN/AGENT) callers get all claims; a "
            + "CUSTOMER-role caller must supply a policyId, since there is no combined 'all my claims' view.")
    public List<ClaimResponseDto> searchClaims(
            @ToolParam(description = "Policy id (UUID) to filter claims by. Required for CUSTOMER-role callers.", required = false)
            String policyId,
            @ToolParam(description = "Zero-based page number, defaults to 0", required = false) Integer page,
            @ToolParam(description = "Page size, defaults to 20", required = false) Integer size) {

        UserPrincipal principal = currentPrincipal();
        log.info("mcp-tool=search_claims caller={} role={} args=[policyId={}, page={}, size={}]",
                principal.getUsername(), principal.getRole(), policyId, page, size);

        Pageable pageable = ToolArgumentGuard.pageable(page, size);

        try {
            Page<ClaimResponseDto> result;
            if (policyId != null && !policyId.isBlank()) {
                result = claimService.getByPolicy(ToolArgumentGuard.requireUuid(policyId, "policyId"), pageable);
            } else if (principal.getRole() == Role.CUSTOMER) {
                throw new IllegalArgumentException(
                        "Please specify which policy's claims you want to see -- there isn't a combined view "
                                + "across all of a customer's policies yet.");
            } else {
                result = claimService.getAll(pageable);
            }

            log.info("mcp-tool=search_claims caller={} outcome=success count={}",
                    principal.getUsername(), result.getContent().size());
            return result.getContent();
        } catch (RuntimeException ex) {
            log.warn("mcp-tool=search_claims caller={} outcome=error message={}",
                    principal.getUsername(), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "get_claim_details", description = "Get full details for one claim by its id.")
    public ClaimResponseDto getClaimDetails(
            @ToolParam(description = "Claim id (UUID)") String claimId) {

        UserPrincipal principal = currentPrincipal();
        log.info("mcp-tool=get_claim_details caller={} role={} args=[claimId={}]",
                principal.getUsername(), principal.getRole(), claimId);

        try {
            ClaimResponseDto result = claimService.getById(ToolArgumentGuard.requireUuid(claimId, "claimId"));
            log.info("mcp-tool=get_claim_details caller={} outcome=success claimNumber={}",
                    principal.getUsername(), result.claimNumber());
            return result;
        } catch (RuntimeException ex) {
            log.warn("mcp-tool=get_claim_details caller={} outcome=error message={}",
                    principal.getUsername(), ex.getMessage());
            throw ex;
        }
    }

    private UserPrincipal currentPrincipal() {
        return currentUserProvider.getCurrentPrincipal()
                .orElseThrow(() -> new AccessDeniedException("Authentication required"));
    }
}
