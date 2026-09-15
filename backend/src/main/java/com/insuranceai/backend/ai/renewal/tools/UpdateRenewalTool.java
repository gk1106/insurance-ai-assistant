package com.insuranceai.backend.ai.renewal.tools;

import com.insuranceai.backend.ai.renewal.RenewalAgentService.ToolContext;
import com.insuranceai.backend.common.exception.BusinessRuleViolationException;
import com.insuranceai.backend.renewal.dto.RenewalResponseDto;
import com.insuranceai.backend.renewal.service.RenewalService;
import com.insuranceai.backend.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/**
 * Wraps {@link RenewalService#confirm} and {@link RenewalService#reject} -- the renewal domain has
 * no single generic "update" method the way {@code PolicyService#update} does (a renewal's dates
 * and premium can't be edited once requested; it only ever gets decided). This tool is the single
 * "update_renewal" entry point the agent design calls for, and dispatches to whichever of those two
 * existing service methods the requested action needs -- no new service logic was added to support
 * it. Authorization (ADMIN/AGENT only) is enforced by {@code @PreAuthorize} on both
 * {@link RenewalService#confirm} and {@link RenewalService#reject} themselves, so a CUSTOMER-role
 * caller is rejected by the service, not by anything in this tool.
 */
public class UpdateRenewalTool {

    private static final Logger log = LoggerFactory.getLogger(UpdateRenewalTool.class);

    private final RenewalService renewalService;
    private final ToolContext context;

    public UpdateRenewalTool(RenewalService renewalService, ToolContext context) {
        this.renewalService = renewalService;
        this.context = context;
    }

    @Tool(name = "update_renewal", description = "Decide a pending renewal. Only staff (ADMIN/AGENT) accounts "
            + "may do this; a CUSTOMER-role caller will receive an authorization error. action must be CONFIRM "
            + "(accept the renewal -- applies the new end date and premium to the policy) or REJECT (decline "
            + "it). Only a PENDING renewal can be decided.")
    public RenewalResponseDto updateRenewal(
            @ToolParam(description = "Renewal id (UUID) to decide") String renewalId,
            @ToolParam(description = "Action to perform: CONFIRM or REJECT") String action) {

        UserPrincipal principal = context.getPrincipal();
        log.info("tool=update_renewal caller={} role={} args=[renewalId={}, action={}]",
                principal.getUsername(), principal.getRole(), renewalId, action);

        try {
            UUID id = UUID.fromString(renewalId);
            RenewalResponseDto result = switch (parseAction(action)) {
                case CONFIRM -> renewalService.confirm(id);
                case REJECT -> renewalService.reject(id);
            };

            context.setLastRenewal(result);
            log.info("tool=update_renewal caller={} outcome=success renewalId={} status={}",
                    principal.getUsername(), result.id(), result.status());
            return result;
        } catch (RuntimeException ex) {
            log.warn("tool=update_renewal caller={} outcome=error message={}",
                    principal.getUsername(), ex.getMessage());
            throw ex;
        }
    }

    private enum RenewalAction {
        CONFIRM, REJECT
    }

    private RenewalAction parseAction(String value) {
        try {
            return RenewalAction.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException(
                    "Invalid action '" + value + "'. Valid values: " + Arrays.toString(RenewalAction.values()));
        }
    }
}
