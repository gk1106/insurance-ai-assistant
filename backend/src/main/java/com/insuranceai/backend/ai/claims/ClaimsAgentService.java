package com.insuranceai.backend.ai.claims;

import com.insuranceai.backend.ai.claims.dto.AgentChatResponseDto;
import com.insuranceai.backend.ai.claims.tools.CheckClaimStatusTool;
import com.insuranceai.backend.ai.claims.tools.CreateClaimTool;
import com.insuranceai.backend.ai.claims.tools.GetClaimDetailsTool;
import com.insuranceai.backend.ai.claims.tools.SearchClaimsTool;
import com.insuranceai.backend.ai.claims.tools.UpdateClaimTool;
import com.insuranceai.backend.claim.dto.ClaimResponseDto;
import com.insuranceai.backend.claim.service.ClaimService;
import com.insuranceai.backend.security.UserPrincipal;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Owns the Claude/GPT tool-use loop for the Claims Agent. Mirrors {@code PolicyAgentService}
 * exactly: Spring AI's ChatClient executes the loop (call the model, run whichever tool it
 * requests, feed the result back, repeat until the model stops calling tools), and this class
 * scopes that loop to the calling user by building a fresh {@link ToolContext} and fresh tool
 * instances per request, so every tool call runs with the authenticated caller's identity, never
 * a shared/ambient one.
 */
@Service
public class ClaimsAgentService {

    private static final Logger log = LoggerFactory.getLogger(ClaimsAgentService.class);

    private final ChatClient claimsAgentChatClient;
    private final ClaimService claimService;
    private final Validator validator;

    public ClaimsAgentService(ChatClient claimsAgentChatClient, ClaimService claimService, Validator validator) {
        this.claimsAgentChatClient = claimsAgentChatClient;
        this.claimService = claimService;
        this.validator = validator;
    }

    public AgentChatResponseDto chat(UserPrincipal principal, String message) {
        ToolContext context = new ToolContext(principal);

        log.info("claims-agent chat start caller={} role={}", principal.getUsername(), principal.getRole());

        String reply = claimsAgentChatClient.prompt()
                .user(message)
                .tools(
                        new SearchClaimsTool(claimService, context),
                        new GetClaimDetailsTool(claimService, context),
                        new CreateClaimTool(claimService, validator, context),
                        new UpdateClaimTool(claimService, validator, context),
                        new CheckClaimStatusTool(claimService, context)
                )
                .call()
                .content();

        log.info("claims-agent chat end caller={} touchedClaim={} searchResultCount={}",
                principal.getUsername(),
                context.getLastClaim() != null ? context.getLastClaim().claimNumber() : null,
                context.getLastSearchResults() != null ? context.getLastSearchResults().size() : null);

        return new AgentChatResponseDto(reply, context.getLastClaim(), context.getLastSearchResults());
    }

    /**
     * Per-request state shared by this turn's tool instances: the authenticated caller (so every
     * tool enforces/logs against the real user, never an LLM-supplied identity) and the last
     * structured result a tool produced (so the controller can return real data alongside the
     * model's prose instead of relying on it to transcribe things correctly).
     */
    public static final class ToolContext {

        private final UserPrincipal principal;
        private ClaimResponseDto lastClaim;
        private List<ClaimResponseDto> lastSearchResults;

        public ToolContext(UserPrincipal principal) {
            this.principal = principal;
        }

        public UserPrincipal getPrincipal() {
            return principal;
        }

        public ClaimResponseDto getLastClaim() {
            return lastClaim;
        }

        public void setLastClaim(ClaimResponseDto lastClaim) {
            this.lastClaim = lastClaim;
        }

        public List<ClaimResponseDto> getLastSearchResults() {
            return lastSearchResults;
        }

        public void setLastSearchResults(List<ClaimResponseDto> lastSearchResults) {
            this.lastSearchResults = lastSearchResults;
        }
    }
}
