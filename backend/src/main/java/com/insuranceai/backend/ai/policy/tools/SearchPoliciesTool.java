package com.insuranceai.backend.ai.policy.tools;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

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

        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20);

        try {
            Page<PolicyResponseDto> result;
            if (principal.getRole() == Role.CUSTOMER) {
                result = policyService.getByCustomer(principal.getCustomerId(), pageable);
            } else if (customerId != null && !customerId.isBlank()) {
                result = policyService.getByCustomer(UUID.fromString(customerId), pageable);
            } else {
                result = policyService.getAll(pageable);
            }

            context.setLastSearchResults(result.getContent());
            log.info("tool=search_policies caller={} outcome=success count={}",
                    principal.getUsername(), result.getContent().size());
            return result.getContent();
        } catch (RuntimeException ex) {
            log.warn("tool=search_policies caller={} outcome=error message={}",
                    principal.getUsername(), ex.getMessage());
            throw ex;
        }
    }
}
