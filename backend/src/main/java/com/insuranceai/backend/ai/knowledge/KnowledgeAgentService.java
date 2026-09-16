package com.insuranceai.backend.ai.knowledge;

import com.insuranceai.backend.ai.guardrail.InputGuardrailService;
import com.insuranceai.backend.ai.guardrail.OutputGuardrailService;
import com.insuranceai.backend.ai.knowledge.dto.AgentChatResponseDto;
import com.insuranceai.backend.ai.observability.AiCallObservability;
import com.insuranceai.backend.ai.knowledge.tools.SearchKnowledgeBaseTool;
import com.insuranceai.backend.ai.rag.RagSearchService;
import com.insuranceai.backend.ai.rag.dto.RagSearchResultDto;
import com.insuranceai.backend.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Owns the tool-use loop for the Knowledge Agent -- the same shape as
 * {@code PolicyAgentService}/{@code ClaimsAgentService}/{@code RenewalAgentService}, but backed by
 * the local RAG pipeline ({@link RagSearchService}) instead of a JPA-backed domain service. A
 * fresh {@link ToolContext} and tool instance are built per request, same as the other agents,
 * even though knowledge-base reads carry no per-caller authorization today -- keeping the shape
 * identical means this agent can pick up caller-aware behavior later without a rewrite.
 */
@Service
public class KnowledgeAgentService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeAgentService.class);

    private final ChatClient knowledgeAgentChatClient;
    private final RagSearchService ragSearchService;
    private final InputGuardrailService inputGuardrailService;
    private final OutputGuardrailService outputGuardrailService;
    private final AiCallObservability aiCallObservability;

    public KnowledgeAgentService(ChatClient knowledgeAgentChatClient, RagSearchService ragSearchService,
                                  InputGuardrailService inputGuardrailService,
                                  OutputGuardrailService outputGuardrailService,
                                  AiCallObservability aiCallObservability) {
        this.knowledgeAgentChatClient = knowledgeAgentChatClient;
        this.ragSearchService = ragSearchService;
        this.inputGuardrailService = inputGuardrailService;
        this.outputGuardrailService = outputGuardrailService;
        this.aiCallObservability = aiCallObservability;
    }

    public AgentChatResponseDto chat(UserPrincipal principal, String message) {
        inputGuardrailService.assertSafe(message);

        ToolContext context = new ToolContext(principal);

        log.info("knowledge-agent chat start caller={} role={}", principal.getUsername(), principal.getRole());

        String reply = aiCallObservability.timed("knowledge-agent-chat", () -> knowledgeAgentChatClient.prompt()
                .user(message)
                .tools(new SearchKnowledgeBaseTool(ragSearchService, context))
                .call()
                .content());

        if (context.getLastResults() == null) {
            // RAG grounding guardrail: the model replied without ever calling
            // search_knowledge_base, so there is nothing retrieved to ground that reply in.
            // Refuse to trust it rather than let an ungrounded answer through, regardless of
            // whether the system prompt's "always search first" instruction was followed.
            log.warn("knowledge-agent chat caller={} outcome=blocked-ungrounded-reply", principal.getUsername());
            return new AgentChatResponseDto(
                    "I can only answer questions I've verified against the knowledge base, and I wasn't able to "
                            + "search it for this one. Could you rephrase it as a specific insurance question?",
                    List.of(), List.of());
        }

        reply = outputGuardrailService.sanitize(reply);

        List<String> sources = context.getLastResults().stream()
                .map(RagSearchResultDto::source)
                .distinct()
                .toList();

        log.info("knowledge-agent chat end caller={} chunksRetrieved={} sources={}",
                principal.getUsername(), context.getLastResults().size(), sources);

        return new AgentChatResponseDto(reply, sources, context.getLastResults());
    }

    /**
     * Per-request state shared with this turn's tool instance: the authenticated caller (for
     * logging, matching the other agents) and the last search results, so the controller can
     * return the real retrieved chunks/sources alongside the model's prose. {@code lastResults}
     * starts as {@code null} -- distinct from an empty list -- so the RAG grounding guardrail in
     * {@code chat()} can tell "searched and found nothing" apart from "never searched at all".
     */
    public static final class ToolContext {

        private final UserPrincipal principal;
        private List<RagSearchResultDto> lastResults;

        public ToolContext(UserPrincipal principal) {
            this.principal = principal;
        }

        public UserPrincipal getPrincipal() {
            return principal;
        }

        public List<RagSearchResultDto> getLastResults() {
            return lastResults;
        }

        public void setLastResults(List<RagSearchResultDto> lastResults) {
            this.lastResults = lastResults;
        }
    }
}
