package com.insuranceai.backend.ai.knowledge;

import com.insuranceai.backend.ai.knowledge.dto.AgentChatRequestDto;
import com.insuranceai.backend.ai.knowledge.dto.AgentChatResponseDto;
import com.insuranceai.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/knowledge-agent")
public class KnowledgeAgentController {

    private final KnowledgeAgentService knowledgeAgentService;

    public KnowledgeAgentController(KnowledgeAgentService knowledgeAgentService) {
        this.knowledgeAgentService = knowledgeAgentService;
    }

    @PostMapping("/chat")
    public AgentChatResponseDto chat(@Valid @RequestBody AgentChatRequestDto request,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return knowledgeAgentService.chat(principal, request.message());
    }
}
