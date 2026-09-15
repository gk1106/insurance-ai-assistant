package com.insuranceai.backend.ai.knowledge;

import com.insuranceai.backend.ai.knowledge.dto.AgentChatResponseDto;
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

    public KnowledgeAgentService(ChatClient knowledgeAgentChatClient, RagSearchService ragSearchService) {
        this.knowledgeAgentChatClient = knowledgeAgentChatClient;
        this.ragSearchService = ragSearchService;
    }

    public AgentChatResponseDto chat(UserPrincipal principal, String message) {
        ToolContext context = new ToolContext(principal);

        log.info("knowledge-agent chat start caller={} role={}", principal.getUsername(), principal.getRole());

        String reply = knowledgeAgentChatClient.prompt()
                .user(message)
                .tools(new SearchKnowledgeBaseTool(ragSearchService, context))
                .call()
                .content();

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
     * return the real retrieved chunks/sources alongside the model's prose.
     */
    public static final class ToolContext {

        private final UserPrincipal principal;
        private List<RagSearchResultDto> lastResults = List.of();

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
