package com.insuranceai.backend.ai.policy;

import com.insuranceai.backend.ai.policy.dto.AgentChatRequestDto;
import com.insuranceai.backend.ai.policy.dto.AgentChatResponseDto;
import com.insuranceai.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/policy-agent")
public class PolicyAgentController {

    private final PolicyAgentService policyAgentService;

    public PolicyAgentController(PolicyAgentService policyAgentService) {
        this.policyAgentService = policyAgentService;
    }

    @PostMapping("/chat")
    public AgentChatResponseDto chat(@Valid @RequestBody AgentChatRequestDto request,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return policyAgentService.chat(principal, request.message());
    }
}
