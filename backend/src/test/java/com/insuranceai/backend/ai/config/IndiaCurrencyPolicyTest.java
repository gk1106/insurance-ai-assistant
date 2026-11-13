package com.insuranceai.backend.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IndiaCurrencyPolicyTest {
    @Test
    void instructionIncludesActualRupeeAndPreservesNumericAndSourceCurrencies() {
        String prompt = IndiaCurrencyPolicy.systemPrompt("Domain-specific instructions");
        assertThat(prompt).startsWith("Domain-specific instructions\n");
        assertThat(prompt).contains("INR or " + (char) 0x20B9,
                "coverageAmount", "premiumAmount", "claimAmount", "approvedAmount", "revisedPremiumAmount",
                "do not perform FX conversion or multiply amounts", "remain numeric",
                "retain the source's", "ask for an INR amount");
    }

    @Test
    void everyDomainClientSendsSharedCurrencyInstructionToModel() {
        AiConfig config = new AiConfig();
        ChatModel model = mock(ChatModel.class);
        when(model.call(any(Prompt.class))).thenReturn(
                new ChatResponse(List.of(new Generation(new AssistantMessage("INR 100.00")))));

        var clients = List.of(config.policyAgentChatClient(model), config.claimsAgentChatClient(model),
                config.renewalAgentChatClient(model), config.knowledgeAgentChatClient(model));
        for (var client : clients) {
            client.prompt().user("Show the amount").call().content();
        }
        var prompts = org.mockito.ArgumentCaptor.forClass(Prompt.class);
        verify(model, times(4)).call(prompts.capture());
        assertThat(prompts.getAllValues()).allSatisfy(prompt ->
                assertThat(prompt.getSystemMessage().getText()).contains(IndiaCurrencyPolicy.INSTRUCTION));
    }
}
