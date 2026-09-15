package com.insuranceai.backend.ai.orchestrator;

import com.insuranceai.backend.ai.orchestrator.dto.AgentChatRequestDto;
import com.insuranceai.backend.ai.orchestrator.dto.AgentChatResponseDto;
import com.insuranceai.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiOrchestratorController {

    private final AiOrchestratorService aiOrchestratorService;

    public AiOrchestratorController(AiOrchestratorService aiOrchestratorService) {
        this.aiOrchestratorService = aiOrchestratorService;
    }

    @PostMapping("/chat")
    public AgentChatResponseDto chat(@Valid @RequestBody AgentChatRequestDto request,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return aiOrchestratorService.chat(principal, request.message());
    }
}
