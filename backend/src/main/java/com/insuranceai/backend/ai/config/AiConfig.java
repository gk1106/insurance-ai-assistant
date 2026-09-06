package com.insuranceai.backend.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    private static final String POLICY_AGENT_SYSTEM_PROMPT = """
            You are the Policy Agent for an insurance platform. You can search policies, get policy
            details, create a policy, update a policy, and check a policy's status -- but only by
            calling the tools provided to you. Never invent a policy id, policy number, or customer
            id; always look one up via a tool first if you don't already have it from the
            conversation. Dates must be ISO-8601 (YYYY-MM-DD).

            You do not have any special privileges of your own -- every tool call runs as the
            person you are talking to, so some actions (like creating or updating a policy) will be
            rejected for customer accounts. If a tool call fails, explain the failure to the user in
            plain language instead of retrying blindly.

            You can only manage insurance policies. If asked about claims or renewals, say that
            capability isn't available yet.
            """;

    @Bean
    public ChatClient policyAgentChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem(POLICY_AGENT_SYSTEM_PROMPT)
                .build();
    }
}
