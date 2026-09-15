package com.insuranceai.backend.ai.orchestrator.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.insuranceai.backend.ai.rag.dto.RagSearchResultDto;
import com.insuranceai.backend.claim.dto.ClaimResponseDto;
import com.insuranceai.backend.policy.dto.PolicyResponseDto;
import com.insuranceai.backend.renewal.dto.RenewalResponseDto;

import java.util.List;

/**
 * The final, possibly-synthesized reply plus whatever structured data the delegated agent(s)
 * produced. Shares the same field names as the individual agents' response DTOs so the frontend
 * can reuse the exact same result cards -- a request routed to a single agent looks identical to
 * calling that agent directly; a multi-domain request may populate more than one of these at once.
 * {@code sources}/{@code chunks} are populated when the Knowledge Agent was used -- the source
 * document names are also expected inline in {@code reply} itself (the Knowledge Agent is
 * instructed to cite them), but these are the structured, trustworthy copy of the same fact.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentChatResponseDto(
        String reply,
        PolicyResponseDto policy,
        List<PolicyResponseDto> policies,
        ClaimResponseDto claim,
        List<ClaimResponseDto> claims,
        RenewalResponseDto renewal,
        List<RenewalResponseDto> renewals,
        List<String> sources,
        List<RagSearchResultDto> chunks
) {
}
