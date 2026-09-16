package com.insuranceai.backend.ai.policy;

import com.insuranceai.backend.ai.guardrail.InputGuardrailService;
import com.insuranceai.backend.ai.guardrail.OutputGuardrailService;
import com.insuranceai.backend.ai.observability.AiCallObservability;
import com.insuranceai.backend.ai.policy.dto.AgentChatResponseDto;
import com.insuranceai.backend.ai.policy.tools.CheckPolicyStatusTool;
import com.insuranceai.backend.ai.policy.tools.CreatePolicyTool;
import com.insuranceai.backend.ai.policy.tools.GetPolicyDetailsTool;
import com.insuranceai.backend.ai.policy.tools.SearchPoliciesTool;
import com.insuranceai.backend.ai.policy.tools.UpdatePolicyTool;
import com.insuranceai.backend.policy.dto.PolicyResponseDto;
import com.insuranceai.backend.policy.service.PolicyService;
import com.insuranceai.backend.security.UserPrincipal;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Owns the Claude tool-use loop for the Policy Agent. Spring AI's ChatClient executes that loop
 * (calling Claude, running whichever tool it requests, feeding the result back, repeating until
 * the model stops calling tools) -- this class is responsible for scoping that loop to the calling
 * user: a fresh {@link ToolContext} and fresh tool instances are built per request so every tool
 * call runs with the authenticated caller's identity, never a shared/ambient one.
 */
@Service
public class PolicyAgentService {

    private static final Logger log = LoggerFactory.getLogger(PolicyAgentService.class);

    private final ChatClient policyAgentChatClient;
    private final PolicyService policyService;
    private final Validator validator;
    private final InputGuardrailService inputGuardrailService;
    private final OutputGuardrailService outputGuardrailService;
    private final AiCallObservability aiCallObservability;

    public PolicyAgentService(ChatClient policyAgentChatClient, PolicyService policyService, Validator validator,
                               InputGuardrailService inputGuardrailService,
                               OutputGuardrailService outputGuardrailService,
                               AiCallObservability aiCallObservability) {
        this.policyAgentChatClient = policyAgentChatClient;
        this.policyService = policyService;
        this.validator = validator;
        this.inputGuardrailService = inputGuardrailService;
        this.outputGuardrailService = outputGuardrailService;
        this.aiCallObservability = aiCallObservability;
    }

    public AgentChatResponseDto chat(UserPrincipal principal, String message) {
        inputGuardrailService.assertSafe(message);

        ToolContext context = new ToolContext(principal);

        log.info("policy-agent chat start caller={} role={}", principal.getUsername(), principal.getRole());

        String reply = aiCallObservability.timed("policy-agent-chat", () -> policyAgentChatClient.prompt()
                .user(message)
                .tools(
                        new SearchPoliciesTool(policyService, context),
                        new GetPolicyDetailsTool(policyService, context),
                        new CreatePolicyTool(policyService, validator, context),
                        new UpdatePolicyTool(policyService, validator, context),
                        new CheckPolicyStatusTool(policyService, context)
                )
                .call()
                .content());
        reply = outputGuardrailService.sanitize(reply);

        log.info("policy-agent chat end caller={} touchedPolicy={} searchResultCount={}",
                principal.getUsername(),
                context.getLastPolicy() != null ? context.getLastPolicy().policyNumber() : null,
                context.getLastSearchResults() != null ? context.getLastSearchResults().size() : null);

        return new AgentChatResponseDto(reply, context.getLastPolicy(), context.getLastSearchResults());
    }

    /**
     * Per-request state shared by this turn's tool instances: the authenticated caller (so every
     * tool enforces/logs against the real user, never an LLM-supplied identity) and the last
     * structured result a tool produced (so the controller can return real data alongside Claude's
     * prose instead of relying on the model to transcribe it correctly).
     */
    public static final class ToolContext {

        private final UserPrincipal principal;
        private PolicyResponseDto lastPolicy;
        private List<PolicyResponseDto> lastSearchResults;

        public ToolContext(UserPrincipal principal) {
            this.principal = principal;
        }

        public UserPrincipal getPrincipal() {
            return principal;
        }

        public PolicyResponseDto getLastPolicy() {
            return lastPolicy;
        }

        public void setLastPolicy(PolicyResponseDto lastPolicy) {
            this.lastPolicy = lastPolicy;
        }

        public List<PolicyResponseDto> getLastSearchResults() {
            return lastSearchResults;
        }

        public void setLastSearchResults(List<PolicyResponseDto> lastSearchResults) {
            this.lastSearchResults = lastSearchResults;
        }
    }
}
