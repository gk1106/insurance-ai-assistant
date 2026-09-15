package com.insuranceai.backend.ai.renewal;

import com.insuranceai.backend.ai.renewal.dto.AgentChatRequestDto;
import com.insuranceai.backend.ai.renewal.dto.AgentChatResponseDto;
import com.insuranceai.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/renewal-agent")
public class RenewalAgentController {

    private final RenewalAgentService renewalAgentService;

    public RenewalAgentController(RenewalAgentService renewalAgentService) {
        this.renewalAgentService = renewalAgentService;
    }

    @PostMapping("/chat")
    public AgentChatResponseDto chat(@Valid @RequestBody AgentChatRequestDto request,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return renewalAgentService.chat(principal, request.message());
    }
}
