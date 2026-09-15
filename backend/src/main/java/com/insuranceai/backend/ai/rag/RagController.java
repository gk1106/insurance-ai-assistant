package com.insuranceai.backend.ai.rag;

import com.insuranceai.backend.ai.rag.dto.IngestionResultDto;
import com.insuranceai.backend.ai.rag.dto.RagSearchResultDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Operational/test endpoints for the RAG ingestion and retrieval pipeline. Not used by any agent
 * or the Orchestrator yet -- ADMIN-only, since this is internal tooling for loading and
 * inspecting the knowledge base rather than a customer-facing feature at this stage.
 */
@RestController
@RequestMapping("/api/ai/rag")
public class RagController {

    private final RagIngestionService ragIngestionService;
    private final RagSearchService ragSearchService;

    public RagController(RagIngestionService ragIngestionService, RagSearchService ragSearchService) {
        this.ragIngestionService = ragIngestionService;
        this.ragSearchService = ragSearchService;
    }

    @PostMapping("/ingest")
    @PreAuthorize("hasRole('ADMIN')")
    public IngestionResultDto ingest() {
        return ragIngestionService.ingestAll();
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public List<RagSearchResultDto> search(@RequestParam String query,
                                            @RequestParam(required = false) Integer topK) {
        return ragSearchService.search(query, topK);
    }
}
