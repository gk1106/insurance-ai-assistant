package com.insuranceai.backend.ai.claims.tools;

import com.insuranceai.backend.ai.claims.ClaimsAgentService.ToolContext;
import com.insuranceai.backend.auth.entity.Role;
import com.insuranceai.backend.claim.dto.ClaimResponseDto;
import com.insuranceai.backend.claim.service.ClaimService;
import com.insuranceai.backend.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Wraps {@link ClaimService#getAll} / {@link ClaimService#getByPolicy} only -- never the
 * repository. Unlike policies, claims have no "by customer" listing on the service, so a
 * CUSTOMER-role caller must supply a policyId (ownership is then enforced inside
 * {@code ClaimServiceImpl.getByPolicy} itself, from the ambient SecurityContext, exactly like
 * {@code GetPolicyDetailsTool} relies on for policies).
 */
public class SearchClaimsTool {

    private static final Logger log = LoggerFactory.getLogger(SearchClaimsTool.class);

    private final ClaimService claimService;
    private final ToolContext context;

    public SearchClaimsTool(ClaimService claimService, ToolContext context) {
        this.claimService = claimService;
        this.context = context;
    }

    @Tool(name = "search_claims", description = "Search insurance claims. If policyId is given, returns that "
            + "policy's claims. If policyId is omitted, staff (ADMIN/AGENT) callers get all claims; a customer "
            + "caller will instead get an error asking them to specify a policy, since there is no combined "
            + "'all my claims' view yet -- if that happens, ask the user which policy and call this again with "
            + "policyId set. Call this when the user asks to list, browse, or find claims; try it without "
            + "policyId first unless the user already named a specific policy.")
    public List<ClaimResponseDto> searchClaims(
            @ToolParam(description = "Policy id (UUID) to filter claims by. Required for CUSTOMER-role callers.", required = false)
            String policyId,
            @ToolParam(description = "Zero-based page number, defaults to 0", required = false) Integer page,
            @ToolParam(description = "Page size, defaults to 20", required = false) Integer size) {

        UserPrincipal principal = context.getPrincipal();
        log.info("tool=search_claims caller={} role={} args=[policyId={}, page={}, size={}]",
                principal.getUsername(), principal.getRole(), policyId, page, size);

        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20);

        try {
            Page<ClaimResponseDto> result;
            if (policyId != null && !policyId.isBlank()) {
                result = claimService.getByPolicy(UUID.fromString(policyId), pageable);
            } else if (principal.getRole() == Role.CUSTOMER) {
                throw new IllegalArgumentException(
                        "Please specify which policy's claims you want to see -- there isn't a combined view "
                                + "across all of a customer's policies yet.");
            } else {
                result = claimService.getAll(pageable);
            }

            context.setLastSearchResults(result.getContent());
            log.info("tool=search_claims caller={} outcome=success count={}",
                    principal.getUsername(), result.getContent().size());
            return result.getContent();
        } catch (RuntimeException ex) {
            log.warn("tool=search_claims caller={} outcome=error message={}",
                    principal.getUsername(), ex.getMessage());
            throw ex;
        }
    }
}
