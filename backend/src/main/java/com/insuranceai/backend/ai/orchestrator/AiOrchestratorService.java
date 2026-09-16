package com.insuranceai.backend.ai.orchestrator;

import com.insuranceai.backend.ai.claims.ClaimsAgentService;
import com.insuranceai.backend.ai.guardrail.InputGuardrailService;
import com.insuranceai.backend.ai.guardrail.OutputGuardrailService;
import com.insuranceai.backend.ai.knowledge.KnowledgeAgentService;
import com.insuranceai.backend.ai.observability.AiCallObservability;
import com.insuranceai.backend.ai.orchestrator.dto.AgentChatResponseDto;
import com.insuranceai.backend.ai.orchestrator.dto.RouteDecision;
import com.insuranceai.backend.ai.policy.PolicyAgentService;
import com.insuranceai.backend.ai.rag.dto.RagSearchResultDto;
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
 * {@link RenewalAgentService} / {@link KnowledgeAgentService} unchanged, and composes their
 * replies into one response. This class never touches a domain repository/service (or the vector
 * store) directly -- it only ever calls the four existing agent services, each of which already
 * owns its own tool-use loop, authorization, and structured result. Routing and (when more than
 * one agent is involved) final-answer composition are each a single, separate LLM call using the
 * shared {@code orchestratorChatClient} -- two small, focused prompts rather than one that tries
 * to do everything.
 */
@Service
public class AiOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(AiOrchestratorService.class);

    private static final String ROUTER_SYSTEM_PROMPT = """
            You are the routing layer for an insurance platform's AI assistant. Given the user's
            message, decide which specialist agent(s) must handle it:

            - POLICY: the caller's own policy records -- searching, details, creating, updating,
              status.
            - CLAIMS: the caller's own claim records -- searching, details, filing, reviewing/
              approving/rejecting/paying, status.
            - RENEWAL: the caller's own renewal records -- searching, details, requesting,
              confirming/rejecting, status.
            - KNOWLEDGE: general, document-based insurance questions that are not about the
              caller's own specific records -- coverage explanations, what's required to file a
              claim, renewal rules, FAQs, how a process works. Use this whenever the question is
              "how does X work" / "what is covered" / "what documents are needed" rather than "show
              MY policy/claim/renewal".

            Return only the agent(s) actually needed to satisfy the request, in the order they
            should run. Most requests need exactly one agent. Return more than one only when the
            request genuinely spans multiple domains in the same message (for example, asking about
            a policy AND a pending claim together, or asking a general coverage question alongside a
            request to see their own policy). If the request is a greeting, small talk, or unrelated
            to insurance, or you genuinely cannot tell which domain it belongs to, return an empty
            list. Always give a one-sentence reason for your choice.
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
    private final KnowledgeAgentService knowledgeAgentService;
    private final InputGuardrailService inputGuardrailService;
    private final OutputGuardrailService outputGuardrailService;
    private final AiCallObservability aiCallObservability;

    public AiOrchestratorService(ChatClient orchestratorChatClient,
                                  PolicyAgentService policyAgentService,
                                  ClaimsAgentService claimsAgentService,
                                  RenewalAgentService renewalAgentService,
                                  KnowledgeAgentService knowledgeAgentService,
                                  InputGuardrailService inputGuardrailService,
                                  OutputGuardrailService outputGuardrailService,
                                  AiCallObservability aiCallObservability) {
        this.orchestratorChatClient = orchestratorChatClient;
        this.policyAgentService = policyAgentService;
        this.claimsAgentService = claimsAgentService;
        this.renewalAgentService = renewalAgentService;
        this.knowledgeAgentService = knowledgeAgentService;
        this.inputGuardrailService = inputGuardrailService;
        this.outputGuardrailService = outputGuardrailService;
        this.aiCallObservability = aiCallObservability;
    }

    public AgentChatResponseDto chat(UserPrincipal principal, String message) {
        inputGuardrailService.assertSafe(message);

        log.info("orchestrator chat start caller={} role={}", principal.getUsername(), principal.getRole());

        List<DomainAgent> agents = route(principal, message);
        if (agents.isEmpty()) {
            log.info("orchestrator chat end caller={} outcome=no-agent-matched", principal.getUsername());
            return new AgentChatResponseDto(
                    "I can help with policies, claims, renewals, or general insurance questions -- "
                            + "could you let me know which one this is about?",
                    null, null, null, null, null, null, null, null);
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
        reply = outputGuardrailService.sanitize(reply);

        log.info("orchestrator chat end caller={} agentsUsed={} succeeded={}",
                principal.getUsername(), agents, successes.stream().map(AgentOutcome::agent).toList());
        return merge(reply, successes);
    }

    private List<DomainAgent> route(UserPrincipal principal, String message) {
        RouteDecision decision;
        try {
            decision = aiCallObservability.timed("orchestrator-route", () -> orchestratorChatClient.prompt()
                    .system(ROUTER_SYSTEM_PROMPT)
                    .user(message)
                    .call()
                    .entity(RouteDecision.class));
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
                case KNOWLEDGE -> {
                    var r = knowledgeAgentService.chat(principal, message);
                    yield AgentOutcome.ofKnowledge(r.reply(), r.sources(), r.chunks());
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
            return aiCallObservability.timed("orchestrator-synthesize", () -> orchestratorChatClient.prompt()
                    .system(SYNTHESIS_SYSTEM_PROMPT)
                    .user(sb.toString())
                    .call()
                    .content());
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
        List<String> sources = null;
        List<RagSearchResultDto> chunks = null;

        for (AgentOutcome outcome : successes) {
            policy = policy != null ? policy : outcome.policy();
            policies = policies != null ? policies : outcome.policies();
            claim = claim != null ? claim : outcome.claim();
            claims = claims != null ? claims : outcome.claims();
            renewal = renewal != null ? renewal : outcome.renewal();
            renewals = renewals != null ? renewals : outcome.renewals();
            sources = sources != null ? sources : outcome.sources();
            chunks = chunks != null ? chunks : outcome.chunks();
        }

        return new AgentChatResponseDto(reply, policy, policies, claim, claims, renewal, renewals, sources, chunks);
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
            List<String> sources,
            List<RagSearchResultDto> chunks,
            RuntimeException error
    ) {
        static AgentOutcome ofPolicy(String reply, PolicyResponseDto policy, List<PolicyResponseDto> policies) {
            return new AgentOutcome(DomainAgent.POLICY, true, reply, policy, policies, null, null, null, null, null,
                    null, null);
        }

        static AgentOutcome ofClaims(String reply, ClaimResponseDto claim, List<ClaimResponseDto> claims) {
            return new AgentOutcome(DomainAgent.CLAIMS, true, reply, null, null, claim, claims, null, null, null,
                    null, null);
        }

        static AgentOutcome ofRenewal(String reply, RenewalResponseDto renewal, List<RenewalResponseDto> renewals) {
            return new AgentOutcome(DomainAgent.RENEWAL, true, reply, null, null, null, null, renewal, renewals,
                    null, null, null);
        }

        static AgentOutcome ofKnowledge(String reply, List<String> sources, List<RagSearchResultDto> chunks) {
            return new AgentOutcome(DomainAgent.KNOWLEDGE, true, reply, null, null, null, null, null, null, sources,
                    chunks, null);
        }

        static AgentOutcome failed(DomainAgent agent, RuntimeException error) {
            return new AgentOutcome(agent, false, null, null, null, null, null, null, null, null, null, error);
        }
    }
}
