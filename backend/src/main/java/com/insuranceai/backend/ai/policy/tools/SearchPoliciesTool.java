package com.insuranceai.backend.ai.policy.tools;

import com.insuranceai.backend.ai.guardrail.ToolArgumentGuard;
import com.insuranceai.backend.ai.observability.ToolTimer;
import com.insuranceai.backend.ai.policy.PolicyAgentService.ToolContext;
import com.insuranceai.backend.auth.entity.Role;
import com.insuranceai.backend.policy.dto.PolicyResponseDto;
import com.insuranceai.backend.policy.service.PolicyService;
import com.insuranceai.backend.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Wraps {@link PolicyService#getAll} / {@link PolicyService#getByCustomer} only -- never the
 * repository. Which one runs, and for which customer, is decided here from the authenticated
 * caller, never from an LLM-supplied override: a CUSTOMER-role caller always gets their own
 * policies regardless of what customerId argument the model passes.
 */
public class SearchPoliciesTool {

    private static final Logger log = LoggerFactory.getLogger(SearchPoliciesTool.class);

    private final PolicyService policyService;
    private final ToolContext context;

    public SearchPoliciesTool(PolicyService policyService, ToolContext context) {
        this.policyService = policyService;
        this.context = context;
    }

    @Tool(name = "search_policies", description = "Search insurance policies. Staff (ADMIN/AGENT) callers see all "
            + "policies, or one customer's policies when customerId is supplied. A CUSTOMER-role caller always "
            + "sees only their own policies, regardless of any customerId argument. Call this when the user asks "
            + "to list, browse, or find policies.")
    public List<PolicyResponseDto> searchPolicies(
            @ToolParam(description = "Customer id (UUID) to filter by. Ignored for CUSTOMER-role callers.", required = false)
            String customerId,
            @ToolParam(description = "Zero-based page number, defaults to 0", required = false) Integer page,
            @ToolParam(description = "Page size, defaults to 20", required = false) Integer size) {

        UserPrincipal principal = context.getPrincipal();
        log.info("tool=search_policies caller={} role={} args=[customerId={}, page={}, size={}]",
                principal.getUsername(), principal.getRole(), customerId, page, size);

        Pageable pageable = ToolArgumentGuard.pageable(page, size);
        long startNanos = ToolTimer.start();

        try {
            Page<PolicyResponseDto> result;
            if (principal.getRole() == Role.CUSTOMER) {
                result = policyService.getByCustomer(principal.getCustomerId(), pageable);
            } else if (customerId != null && !customerId.isBlank()) {
                result = policyService.getByCustomer(ToolArgumentGuard.requireUuid(customerId, "customerId"), pageable);
            } else {
                result = policyService.getAll(pageable);
            }

            context.setLastSearchResults(result.getContent());
            log.info("tool=search_policies caller={} durationMs={} outcome=success count={}",
                    principal.getUsername(), ToolTimer.elapsedMs(startNanos), result.getContent().size());
            return result.getContent();
        } catch (RuntimeException ex) {
            log.warn("tool=search_policies caller={} durationMs={} outcome=error message={}",
                    principal.getUsername(), ToolTimer.elapsedMs(startNanos), ex.getMessage());
            throw ex;
        }
    }
}
