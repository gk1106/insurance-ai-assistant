package com.insuranceai.backend.ai.claims.tools;

import com.insuranceai.backend.ai.claims.ClaimsAgentService.ToolContext;
import com.insuranceai.backend.claim.dto.ClaimResponseDto;
import com.insuranceai.backend.claim.service.ClaimService;
import com.insuranceai.backend.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.UUID;

/**
 * Wraps {@link ClaimService#getById} only. It already enforces ownership
 * (CurrentUserProvider.assertOwnerOrElevated) inside the service itself, from the ambient
 * SecurityContext of the current request -- so a CUSTOMER-role caller asking about a claim that
 * isn't theirs is rejected there, not by any logic in this tool. There is no lookup-by-claim-number
 * on the service (unlike policies' getByPolicyNumber), so only claimId is supported.
 */
public class GetClaimDetailsTool {

    private static final Logger log = LoggerFactory.getLogger(GetClaimDetailsTool.class);

    private final ClaimService claimService;
    private final ToolContext context;

    public GetClaimDetailsTool(ClaimService claimService, ToolContext context) {
        this.claimService = claimService;
        this.context = context;
    }

    @Tool(name = "get_claim_details", description = "Get full details for one claim by its id. Call this when "
            + "the user asks about a specific, already-identified claim.")
    public ClaimResponseDto getClaimDetails(
            @ToolParam(description = "Claim id (UUID)") String claimId) {

        UserPrincipal principal = context.getPrincipal();
        log.info("tool=get_claim_details caller={} role={} args=[claimId={}]",
                principal.getUsername(), principal.getRole(), claimId);

        try {
            ClaimResponseDto result = claimService.getById(UUID.fromString(claimId));
            context.setLastClaim(result);
            log.info("tool=get_claim_details caller={} outcome=success claimNumber={}",
                    principal.getUsername(), result.claimNumber());
            return result;
        } catch (RuntimeException ex) {
            log.warn("tool=get_claim_details caller={} outcome=error message={}",
                    principal.getUsername(), ex.getMessage());
            throw ex;
        }
    }
}
