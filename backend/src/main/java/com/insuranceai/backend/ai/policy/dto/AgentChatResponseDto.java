package com.insuranceai.backend.ai.policy.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.insuranceai.backend.policy.dto.PolicyResponseDto;

import java.util.List;

/**
 * The natural-language reply is what the model said; {@code policy} / {@code policies} are the
 * actual structured data returned by whichever tool ran (if any) -- the frontend should render
 * these rather than trust the model's prose as the source of truth for what changed.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentChatResponseDto(
        String reply,
        PolicyResponseDto policy,
        List<PolicyResponseDto> policies
) {
}
