package com.insuranceai.backend.ai.policy.tools;

import com.insuranceai.backend.ai.guardrail.ToolArgumentGuard;
import com.insuranceai.backend.ai.observability.ToolTimer;
import com.insuranceai.backend.ai.policy.PolicyAgentService.ToolContext;
import com.insuranceai.backend.policy.dto.PolicyResponseDto;
import com.insuranceai.backend.policy.entity.PolicyStatus;
import com.insuranceai.backend.policy.service.PolicyService;
import com.insuranceai.backend.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A narrow, high-frequency projection over {@link PolicyService#getById} /
 * {@link PolicyService#getByPolicyNumber} -- "is my policy still active / how long until it
 * expires" is common enough, and specific enough, to deserve its own tool rather than relying on
 * the model to compute it from get_policy_details' full response.
 */
public class CheckPolicyStatusTool {

    private static final Logger log = LoggerFactory.getLogger(CheckPolicyStatusTool.class);

    private final PolicyService policyService;
    private final ToolContext context;

    public CheckPolicyStatusTool(PolicyService policyService, ToolContext context) {
        this.policyService = policyService;
        this.context = context;
    }

    @Tool(name = "check_policy_status", description = "Check whether a policy is active, and how many days remain "
            + "until it expires. Provide exactly one of policyId or policyNumber. Call this when the user asks if "
            + "a policy is active, expired, or about to expire.")
    public PolicyStatusSummary checkPolicyStatus(
            @ToolParam(description = "Policy id (UUID)", required = false) String policyId,
            @ToolParam(description = "Policy number, e.g. POL-2026-ABC12345", required = false) String policyNumber) {

        UserPrincipal principal = context.getPrincipal();
        log.info("tool=check_policy_status caller={} role={} args=[policyId={}, policyNumber={}]",
                principal.getUsername(), principal.getRole(), policyId, policyNumber);

        long startNanos = ToolTimer.start();
        try {
            PolicyResponseDto policy;
            if (policyId != null && !policyId.isBlank()) {
                policy = policyService.getById(ToolArgumentGuard.requireUuid(policyId, "policyId"));
            } else if (policyNumber != null && !policyNumber.isBlank()) {
                policy = policyService.getByPolicyNumber(policyNumber);
            } else {
                throw new IllegalArgumentException("Provide either policyId or policyNumber");
            }

            context.setLastPolicy(policy);
            long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), policy.endDate());
            log.info("tool=check_policy_status caller={} durationMs={} outcome=success policyNumber={} status={}",
                    principal.getUsername(), ToolTimer.elapsedMs(startNanos), policy.policyNumber(), policy.status());
            return new PolicyStatusSummary(policy.policyNumber(), policy.status(), policy.endDate(), daysUntilExpiry);
        } catch (RuntimeException ex) {
            log.warn("tool=check_policy_status caller={} durationMs={} outcome=error message={}",
                    principal.getUsername(), ToolTimer.elapsedMs(startNanos), ex.getMessage());
            throw ex;
        }
    }

    public record PolicyStatusSummary(String policyNumber, PolicyStatus status, LocalDate endDate,
                                       long daysUntilExpiry) {
    }
}
