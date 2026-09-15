package com.insuranceai.backend.ai.claims;

import com.insuranceai.backend.ai.claims.dto.AgentChatRequestDto;
import com.insuranceai.backend.ai.claims.dto.AgentChatResponseDto;
import com.insuranceai.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/claims-agent")
public class ClaimsAgentController {

    private final ClaimsAgentService claimsAgentService;

    public ClaimsAgentController(ClaimsAgentService claimsAgentService) {
        this.claimsAgentService = claimsAgentService;
    }

    @PostMapping("/chat")
    public AgentChatResponseDto chat(@Valid @RequestBody AgentChatRequestDto request,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return claimsAgentService.chat(principal, request.message());
    }
}
