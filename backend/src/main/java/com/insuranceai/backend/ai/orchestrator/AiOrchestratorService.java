package com.insuranceai.backend.ai.orchestrator;

import com.insuranceai.backend.ai.claims.ClaimsAgentService;
import com.insuranceai.backend.ai.orchestrator.dto.AgentChatResponseDto;
import com.insuranceai.backend.ai.orchestrator.dto.RouteDecision;
import com.insuranceai.backend.ai.policy.PolicyAgentService;
import com.insuranceai.backend.ai.renewal.RenewalAgentService;
import com.insuranceai.backend.claim.dto.ClaimResponseDto;
import com.insuranceai.backend.policy.dto.PolicyResponseDto;
import com.insuranceai.backend.renewal.dto.RenewalResponseDto;
import com.insuranceai.backend.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Top-level entry point for the AI Assistant: figures out which domain agent(s) a request needs,
 * delegates to the existing {@link PolicyAgentService} / {@link ClaimsAgentService} /
 * {@link RenewalAgentService} unchanged, and composes their replies into one response. This class
 * never touches a domain repository/service directly -- it only ever calls the three existing
 * agent services, each of which already owns its own tool-use loop, authorization, and structured
 * result. Routing and (when more than one agent is involved) final-answer composition are each a
 * single, separate LLM call using the shared {@code orchestratorChatClient} -- two small, focused
 * prompts rather than one that tries to do everything.
 */
@Service
public class AiOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(AiOrchestratorService.class);

    private static final String ROUTER_SYSTEM_PROMPT = """
            You are the routing layer for an insurance platform's AI assistant. Given the user's
            message, decide which specialist agent(s) must handle it:

            - POLICY: insurance policies -- searching, details, creating, updating, status.
            - CLAIMS: insurance claims -- searching, details, filing, reviewing/approving/rejecting/
              paying, status.
            - RENEWAL: policy renewals -- searching, details, requesting, confirming/rejecting,
              status.

            Return only the agent(s) actually needed to satisfy the request, in the order they
            should run. Most requests need exactly one agent. Return more than one only when the
            request genuinely spans multiple domains in the same message (for example, asking about
            a policy AND a pending claim together). If the request is a greeting, small talk, or
            unrelated to insurance, or you genuinely cannot tell which domain it belongs to, return
            an empty list. Always give a one-sentence reason for your choice.
            """;

    private static final String SYNTHESIS_SYSTEM_PROMPT = """
            You are the final response composer for an insurance platform's AI assistant. You are
            given the user's original request and the raw replies already produced by one or more
            specialist agents, each of which already performed any necessary lookups or actions and
            already enforced its own authorization -- do not second-guess permissions or invent new
            information beyond what the agents reported. Combine their replies into a single, clear,
            well-organized response for the user in plain language, without repeating overlapping
            information. If a note says part of the request could not be completed, mention that
            briefly and helpfully, without technical jargon. Keep it concise.
            """;

    private final ChatClient orchestratorChatClient;
    private final PolicyAgentService policyAgentService;
    private final ClaimsAgentService claimsAgentService;
    private final RenewalAgentService renewalAgentService;

    public AiOrchestratorService(ChatClient orchestratorChatClient,
                                  PolicyAgentService policyAgentService,
                                  ClaimsAgentService claimsAgentService,
                                  RenewalAgentService renewalAgentService) {
        this.orchestratorChatClient = orchestratorChatClient;
        this.policyAgentService = policyAgentService;
        this.claimsAgentService = claimsAgentService;
        this.renewalAgentService = renewalAgentService;
    }

    public AgentChatResponseDto chat(UserPrincipal principal, String message) {
        log.info("orchestrator chat start caller={} role={}", principal.getUsername(), principal.getRole());

        List<DomainAgent> agents = route(principal, message);
        if (agents.isEmpty()) {
            log.info("orchestrator chat end caller={} outcome=no-agent-matched", principal.getUsername());
            return new AgentChatResponseDto(
                    "I can help with policies, claims, or renewals -- could you let me know which one "
                            + "this is about?",
                    null, null, null, null, null, null);
        }

        List<AgentOutcome> outcomes = new ArrayList<>();
        for (DomainAgent agent : agents) {
            outcomes.add(invoke(agent, principal, message));
        }

        List<AgentOutcome> successes = outcomes.stream().filter(AgentOutcome::succeeded).toList();
        if (successes.isEmpty()) {
            // Every delegated agent failed -- propagate the first failure as-is so the caller gets
            // the same error handling (GlobalExceptionHandler) a single-agent endpoint would give.
            log.warn("orchestrator chat end caller={} outcome=all-agents-failed", principal.getUsername());
            throw outcomes.get(0).error();
        }

        String reply = (outcomes.size() == 1)
                ? successes.get(0).reply()
                : synthesize(principal, message, outcomes);

        log.info("orchestrator chat end caller={} agentsUsed={} succeeded={}",
                principal.getUsername(), agents, successes.stream().map(AgentOutcome::agent).toList());
        return merge(reply, successes);
    }

    private List<DomainAgent> route(UserPrincipal principal, String message) {
        RouteDecision decision;
        try {
            decision = orchestratorChatClient.prompt()
                    .system(ROUTER_SYSTEM_PROMPT)
                    .user(message)
                    .call()
                    .entity(RouteDecision.class);
        } catch (RuntimeException ex) {
            log.warn("orchestrator route caller={} outcome=error message={}",
                    principal.getUsername(), ex.getMessage());
            return List.of();
        }

        if (decision == null || decision.agents() == null) {
            return List.of();
        }

        List<DomainAgent> agents = decision.agents().stream()
                .map(this::parseAgent)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        log.info("orchestrator route caller={} agents={} reasoning={}",
                principal.getUsername(), agents, decision.reasoning());
        return agents;
    }

    private DomainAgent parseAgent(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return DomainAgent.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("orchestrator route unrecognized agent value={}", value);
            return null;
        }
    }

    private AgentOutcome invoke(DomainAgent agent, UserPrincipal principal, String message) {
        try {
            AgentOutcome outcome = switch (agent) {
                case POLICY -> {
                    var r = policyAgentService.chat(principal, message);
                    yield AgentOutcome.ofPolicy(r.reply(), r.policy(), r.policies());
                }
                case CLAIMS -> {
                    var r = claimsAgentService.chat(principal, message);
                    yield AgentOutcome.ofClaims(r.reply(), r.claim(), r.claims());
                }
                case RENEWAL -> {
                    var r = renewalAgentService.chat(principal, message);
                    yield AgentOutcome.ofRenewal(r.reply(), r.renewal(), r.renewals());
                }
            };
            log.info("orchestrator delegate caller={} agent={} outcome=success", principal.getUsername(), agent);
            return outcome;
        } catch (RuntimeException ex) {
            log.warn("orchestrator delegate caller={} agent={} outcome=error message={}",
                    principal.getUsername(), agent, ex.getMessage());
            return AgentOutcome.failed(agent, ex);
        }
    }

    private String synthesize(UserPrincipal principal, String message, List<AgentOutcome> outcomes) {
        StringBuilder sb = new StringBuilder("Original request: ").append(message).append("\n\n");
        for (AgentOutcome outcome : outcomes) {
            if (outcome.succeeded()) {
                sb.append(outcome.agent()).append(" agent said: ").append(outcome.reply()).append("\n\n");
            } else {
                sb.append(outcome.agent()).append(" agent could not complete its part: ")
                        .append(outcome.error().getMessage()).append("\n\n");
            }
        }

        try {
            return orchestratorChatClient.prompt()
                    .system(SYNTHESIS_SYSTEM_PROMPT)
                    .user(sb.toString())
                    .call()
                    .content();
        } catch (RuntimeException ex) {
            log.warn("orchestrator synthesize caller={} outcome=error message={}",
                    principal.getUsername(), ex.getMessage());
            // Fall back to a plain join of the successful replies rather than failing the whole
            // request just because the polishing pass itself failed.
            return outcomes.stream()
                    .filter(AgentOutcome::succeeded)
                    .map(AgentOutcome::reply)
                    .collect(Collectors.joining("\n\n"));
        }
    }

    private AgentChatResponseDto merge(String reply, List<AgentOutcome> successes) {
        PolicyResponseDto policy = null;
        List<PolicyResponseDto> policies = null;
        ClaimResponseDto claim = null;
        List<ClaimResponseDto> claims = null;
        RenewalResponseDto renewal = null;
        List<RenewalResponseDto> renewals = null;

        for (AgentOutcome outcome : successes) {
            policy = policy != null ? policy : outcome.policy();
            policies = policies != null ? policies : outcome.policies();
            claim = claim != null ? claim : outcome.claim();
            claims = claims != null ? claims : outcome.claims();
            renewal = renewal != null ? renewal : outcome.renewal();
            renewals = renewals != null ? renewals : outcome.renewals();
        }

        return new AgentChatResponseDto(reply, policy, policies, claim, claims, renewal, renewals);
    }

    /**
     * One delegated agent's outcome for this turn -- either its structured result, or the
     * exception it threw, never both.
     */
    private record AgentOutcome(
            DomainAgent agent,
            boolean succeeded,
            String reply,
            PolicyResponseDto policy,
            List<PolicyResponseDto> policies,
            ClaimResponseDto claim,
            List<ClaimResponseDto> claims,
            RenewalResponseDto renewal,
            List<RenewalResponseDto> renewals,
            RuntimeException error
    ) {
        static AgentOutcome ofPolicy(String reply, PolicyResponseDto policy, List<PolicyResponseDto> policies) {
            return new AgentOutcome(DomainAgent.POLICY, true, reply, policy, policies, null, null, null, null, null);
        }

        static AgentOutcome ofClaims(String reply, ClaimResponseDto claim, List<ClaimResponseDto> claims) {
            return new AgentOutcome(DomainAgent.CLAIMS, true, reply, null, null, claim, claims, null, null, null);
        }

        static AgentOutcome ofRenewal(String reply, RenewalResponseDto renewal, List<RenewalResponseDto> renewals) {
            return new AgentOutcome(DomainAgent.RENEWAL, true, reply, null, null, null, null, renewal, renewals, null);
        }

        static AgentOutcome failed(DomainAgent agent, RuntimeException error) {
            return new AgentOutcome(agent, false, null, null, null, null, null, null, null, error);
        }
    }
}
