package com.insuranceai.backend.ai.renewal;

import com.insuranceai.backend.ai.guardrail.InputGuardrailService;
import com.insuranceai.backend.ai.guardrail.OutputGuardrailService;
import com.insuranceai.backend.ai.observability.AiCallObservability;
import com.insuranceai.backend.ai.renewal.dto.AgentChatResponseDto;
import com.insuranceai.backend.ai.renewal.tools.CheckRenewalStatusTool;
import com.insuranceai.backend.ai.renewal.tools.CreateRenewalTool;
import com.insuranceai.backend.ai.renewal.tools.GetRenewalDetailsTool;
import com.insuranceai.backend.ai.renewal.tools.SearchRenewalsTool;
import com.insuranceai.backend.ai.renewal.tools.UpdateRenewalTool;
import com.insuranceai.backend.renewal.dto.RenewalResponseDto;
import com.insuranceai.backend.renewal.service.RenewalService;
import com.insuranceai.backend.security.UserPrincipal;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Owns the Claude/GPT tool-use loop for the Renewal Agent. Mirrors {@code PolicyAgentService} /
 * {@code ClaimsAgentService} exactly: Spring AI's ChatClient executes the loop (call the model,
 * run whichever tool it requests, feed the result back, repeat until the model stops calling
 * tools), and this class scopes that loop to the calling user by building a fresh
 * {@link ToolContext} and fresh tool instances per request, so every tool call runs with the
 * authenticated caller's identity, never a shared/ambient one.
 */
@Service
public class RenewalAgentService {

    private static final Logger log = LoggerFactory.getLogger(RenewalAgentService.class);

    private final ChatClient renewalAgentChatClient;
    private final RenewalService renewalService;
    private final Validator validator;
    private final InputGuardrailService inputGuardrailService;
    private final OutputGuardrailService outputGuardrailService;
    private final AiCallObservability aiCallObservability;

    public RenewalAgentService(ChatClient renewalAgentChatClient, RenewalService renewalService, Validator validator,
                                InputGuardrailService inputGuardrailService,
                                OutputGuardrailService outputGuardrailService,
                                AiCallObservability aiCallObservability) {
        this.renewalAgentChatClient = renewalAgentChatClient;
        this.renewalService = renewalService;
        this.validator = validator;
        this.inputGuardrailService = inputGuardrailService;
        this.outputGuardrailService = outputGuardrailService;
        this.aiCallObservability = aiCallObservability;
    }

    public AgentChatResponseDto chat(UserPrincipal principal, String message) {
        inputGuardrailService.assertSafe(message);

        ToolContext context = new ToolContext(principal);

        log.info("renewal-agent chat start caller={} role={}", principal.getUsername(), principal.getRole());

        String reply = aiCallObservability.timed("renewal-agent-chat", () -> renewalAgentChatClient.prompt()
                .user(message)
                .tools(
                        new SearchRenewalsTool(renewalService, context),
                        new GetRenewalDetailsTool(renewalService, context),
                        new CreateRenewalTool(renewalService, validator, context),
                        new UpdateRenewalTool(renewalService, context),
                        new CheckRenewalStatusTool(renewalService, context)
                )
                .call()
                .content());
        reply = outputGuardrailService.sanitize(reply);

        log.info("renewal-agent chat end caller={} touchedRenewal={} searchResultCount={}",
                principal.getUsername(),
                context.getLastRenewal() != null ? context.getLastRenewal().id() : null,
                context.getLastSearchResults() != null ? context.getLastSearchResults().size() : null);

        return new AgentChatResponseDto(reply, context.getLastRenewal(), context.getLastSearchResults());
    }

    /**
     * Per-request state shared by this turn's tool instances: the authenticated caller (so every
     * tool enforces/logs against the real user, never an LLM-supplied identity) and the last
     * structured result a tool produced (so the controller can return real data alongside the
     * model's prose instead of relying on it to transcribe things correctly).
     */
    public static final class ToolContext {

        private final UserPrincipal principal;
        private RenewalResponseDto lastRenewal;
        private List<RenewalResponseDto> lastSearchResults;

        public ToolContext(UserPrincipal principal) {
            this.principal = principal;
        }

        public UserPrincipal getPrincipal() {
            return principal;
        }

        public RenewalResponseDto getLastRenewal() {
            return lastRenewal;
        }

        public void setLastRenewal(RenewalResponseDto lastRenewal) {
            this.lastRenewal = lastRenewal;
        }

        public List<RenewalResponseDto> getLastSearchResults() {
            return lastSearchResults;
        }

        public void setLastSearchResults(List<RenewalResponseDto> lastSearchResults) {
            this.lastSearchResults = lastSearchResults;
        }
    }
}
