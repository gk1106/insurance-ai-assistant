package com.insuranceai.backend.ai.mcp;

import com.insuranceai.backend.ai.mcp.tools.ClaimsMcpTools;
import com.insuranceai.backend.ai.mcp.tools.PolicyMcpTools;
import com.insuranceai.backend.ai.mcp.tools.RenewalMcpTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the read-only MCP tools as a single {@link ToolCallbackProvider} bean. Spring AI's
 * MCP server autoconfiguration (enabled by the spring-ai-starter-mcp-server-webmvc dependency;
 * see application.yml's spring.ai.mcp.server.* properties) discovers this bean automatically and
 * exposes every {@code @Tool}-annotated method on the given objects to any connected MCP client
 * -- the same {@code @Tool}/{@code @ToolParam} annotations the Policy/Claims/Renewal chat agents'
 * own tool classes already use, just picked up by the MCP server instead of a ChatClient.
 */
@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider mcpToolCallbackProvider(PolicyMcpTools policyMcpTools,
                                                          ClaimsMcpTools claimsMcpTools,
                                                          RenewalMcpTools renewalMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(policyMcpTools, claimsMcpTools, renewalMcpTools)
                .build();
    }
}
