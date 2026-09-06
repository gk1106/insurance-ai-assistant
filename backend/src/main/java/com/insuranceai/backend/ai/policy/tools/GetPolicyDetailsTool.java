package com.insuranceai.backend.ai.policy.tools;

import com.insuranceai.backend.ai.policy.PolicyAgentService.ToolContext;
import com.insuranceai.backend.policy.dto.PolicyResponseDto;
import com.insuranceai.backend.policy.service.PolicyService;
import com.insuranceai.backend.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.UUID;

/**
 * Wraps {@link PolicyService#getById} / {@link PolicyService#getByPolicyNumber} only. Both already
 * enforce ownership (CurrentUserProvider.assertOwnerOrElevated) inside the service itself, from the
 * ambient SecurityContext of the current request -- so a CUSTOMER-role caller asking about a policy
 * that isn't theirs is rejected there, not by any logic in this tool.
 */
public class GetPolicyDetailsTool {

    private static final Logger log = LoggerFactory.getLogger(GetPolicyDetailsTool.class);

    private final PolicyService policyService;
    private final ToolContext context;

    public GetPolicyDetailsTool(PolicyService policyService, ToolContext context) {
        this.policyService = policyService;
        this.context = context;
    }

    @Tool(name = "get_policy_details", description = "Get full details for one policy by its id or its policy "
            + "number. Provide exactly one of policyId or policyNumber. Call this when the user asks about a "
            + "specific, already-identified policy.")
    public PolicyResponseDto getPolicyDetails(
            @ToolParam(description = "Policy id (UUID)", required = false) String policyId,
            @ToolParam(description = "Policy number, e.g. POL-2026-ABC12345", required = false) String policyNumber) {

        UserPrincipal principal = context.getPrincipal();
        log.info("tool=get_policy_details caller={} role={} args=[policyId={}, policyNumber={}]",
                principal.getUsername(), principal.getRole(), policyId, policyNumber);

        try {
            PolicyResponseDto result;
            if (policyId != null && !policyId.isBlank()) {
                result = policyService.getById(UUID.fromString(policyId));
            } else if (policyNumber != null && !policyNumber.isBlank()) {
                result = policyService.getByPolicyNumber(policyNumber);
            } else {
                throw new IllegalArgumentException("Provide either policyId or policyNumber");
            }

            context.setLastPolicy(result);
            log.info("tool=get_policy_details caller={} outcome=success policyNumber={}",
                    principal.getUsername(), result.policyNumber());
            return result;
        } catch (RuntimeException ex) {
            log.warn("tool=get_policy_details caller={} outcome=error message={}",
                    principal.getUsername(), ex.getMessage());
            throw ex;
        }
    }
}
