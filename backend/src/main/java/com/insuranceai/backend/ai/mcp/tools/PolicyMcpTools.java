package com.insuranceai.backend.ai.mcp.tools;

import com.insuranceai.backend.ai.guardrail.ToolArgumentGuard;
import com.insuranceai.backend.auth.entity.Role;
import com.insuranceai.backend.policy.dto.PolicyResponseDto;
import com.insuranceai.backend.policy.service.PolicyService;
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
 * Read-only MCP tools over {@link PolicyService} -- the same two calls
 * {@code SearchPoliciesTool}/{@code GetPolicyDetailsTool} make for the Policy Agent, never the
 * repository. Unlike those, this is a plain singleton bean rather than a per-chat-turn instance:
 * an MCP tool call has no equivalent of the chat agents' per-request {@code ToolContext}, so the
 * caller is read directly from {@link CurrentUserProvider} (the same ambient SecurityContext
 * {@link PolicyService} itself relies on for ownership checks) at call time instead. This class
 * adds no authorization logic beyond that -- role/ownership enforcement is entirely
 * PolicyService's and CurrentUserProvider's, exactly as for every other caller of this service.
 */
@Component
public class PolicyMcpTools {

    private static final Logger log = LoggerFactory.getLogger(PolicyMcpTools.class);

    private final PolicyService policyService;
    private final CurrentUserProvider currentUserProvider;

    public PolicyMcpTools(PolicyService policyService, CurrentUserProvider currentUserProvider) {
        this.policyService = policyService;
        this.currentUserProvider = currentUserProvider;
    }

    @Tool(name = "search_policies", description = "Search insurance policies. Staff (ADMIN/AGENT) callers see all "
            + "policies, or one customer's policies when customerId is supplied. A CUSTOMER-role caller always "
            + "sees only their own policies, regardless of any customerId argument.")
    public List<PolicyResponseDto> searchPolicies(
            @ToolParam(description = "Customer id (UUID) to filter by. Ignored for CUSTOMER-role callers.", required = false)
            String customerId,
            @ToolParam(description = "Zero-based page number, defaults to 0", required = false) Integer page,
            @ToolParam(description = "Page size, defaults to 20", required = false) Integer size) {

        UserPrincipal principal = currentPrincipal();
        log.info("mcp-tool=search_policies caller={} role={} args=[customerId={}, page={}, size={}]",
                principal.getUsername(), principal.getRole(), customerId, page, size);

        Pageable pageable = ToolArgumentGuard.pageable(page, size);

        try {
            Page<PolicyResponseDto> result;
            if (principal.getRole() == Role.CUSTOMER) {
                result = policyService.getByCustomer(principal.getCustomerId(), pageable);
            } else if (customerId != null && !customerId.isBlank()) {
                result = policyService.getByCustomer(ToolArgumentGuard.requireUuid(customerId, "customerId"), pageable);
            } else {
                result = policyService.getAll(pageable);
            }

            log.info("mcp-tool=search_policies caller={} outcome=success count={}",
                    principal.getUsername(), result.getContent().size());
            return result.getContent();
        } catch (RuntimeException ex) {
            log.warn("mcp-tool=search_policies caller={} outcome=error message={}",
                    principal.getUsername(), ex.getMessage());
            throw ex;
        }
    }

    @Tool(name = "get_policy_details", description = "Get full details for one policy by its id or its policy "
            + "number. Provide exactly one of policyId or policyNumber.")
    public PolicyResponseDto getPolicyDetails(
            @ToolParam(description = "Policy id (UUID)", required = false) String policyId,
            @ToolParam(description = "Policy number, e.g. POL-2026-ABC12345", required = false) String policyNumber) {

        UserPrincipal principal = currentPrincipal();
        log.info("mcp-tool=get_policy_details caller={} role={} args=[policyId={}, policyNumber={}]",
                principal.getUsername(), principal.getRole(), policyId, policyNumber);

        try {
            PolicyResponseDto result;
            if (policyId != null && !policyId.isBlank()) {
                result = policyService.getById(ToolArgumentGuard.requireUuid(policyId, "policyId"));
            } else if (policyNumber != null && !policyNumber.isBlank()) {
                result = policyService.getByPolicyNumber(policyNumber);
            } else {
                throw new IllegalArgumentException("Provide either policyId or policyNumber");
            }

            log.info("mcp-tool=get_policy_details caller={} outcome=success policyNumber={}",
                    principal.getUsername(), result.policyNumber());
            return result;
        } catch (RuntimeException ex) {
            log.warn("mcp-tool=get_policy_details caller={} outcome=error message={}",
                    principal.getUsername(), ex.getMessage());
            throw ex;
        }
    }

    private UserPrincipal currentPrincipal() {
        return currentUserProvider.getCurrentPrincipal()
                .orElseThrow(() -> new AccessDeniedException("Authentication required"));
    }
}
