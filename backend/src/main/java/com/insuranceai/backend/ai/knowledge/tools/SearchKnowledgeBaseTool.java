package com.insuranceai.backend.ai.knowledge.tools;

import com.insuranceai.backend.ai.knowledge.KnowledgeAgentService.ToolContext;
import com.insuranceai.backend.ai.observability.ToolTimer;
import com.insuranceai.backend.ai.rag.RagSearchService;
import com.insuranceai.backend.ai.rag.dto.RagSearchResultDto;
import com.insuranceai.backend.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;

/**
 * Wraps {@link RagSearchService#search(String, Integer, double)} only -- this is the sole path
 * from the LLM to the vector store; the model never sees a VectorStore, JDBC, or SQL, only this
 * tool's structured return value. topK and the similarity cutoff are fixed here rather than left
 * for the model to choose, so results stay predictable regardless of how the question is phrased.
 */
public class SearchKnowledgeBaseTool {

    private static final Logger log = LoggerFactory.getLogger(SearchKnowledgeBaseTool.class);

    private static final int TOP_K = 5;
    // text-embedding-3-small cosine scores for this corpus cluster around 0.40-0.55 even for a
    // clearly-correct match (and can dip lower still if the model pads/rephrases the query) --
    // 0.5 was cutting off good matches. 0.3 comfortably keeps genuine matches while still
    // dropping unrelated passages.
    private static final double SIMILARITY_THRESHOLD = 0.3;

    private final RagSearchService ragSearchService;
    private final ToolContext context;

    public SearchKnowledgeBaseTool(RagSearchService ragSearchService, ToolContext context) {
        this.ragSearchService = ragSearchService;
        this.context = context;
    }

    @Tool(name = "search_knowledge_base", description = "Search the insurance knowledge base (policy handbooks, "
            + "FAQs, coverage guides, claims-process and renewal-rules documents) for passages relevant to the "
            + "user's question. Always call this before answering a general, document-based insurance question -- "
            + "never answer such a question from memory. Returns the most relevant passages, each tagged with its "
            + "source document name and page; may return an empty list if nothing in the knowledge base is "
            + "relevant enough.")
    public List<RagSearchResultDto> searchKnowledgeBase(
            @ToolParam(description = "The search terms. Keep this close to the user's own wording -- a short, "
                    + "natural phrase or question works better than an expanded or repeated one; do not pad it "
                    + "with synonyms or restate it multiple times") String query) {

        UserPrincipal principal = context.getPrincipal();
        // The query text itself isn't logged -- it's free-form user input that could echo back
        // customer-specific content; only its length is, which is enough to spot suspiciously
        // large inputs. RagSearchService logs the retrieval outcome (chunks, sources, timing).
        log.info("tool=search_knowledge_base caller={} args=[queryLength={}]",
                principal.getUsername(), query != null ? query.length() : 0);

        long startNanos = ToolTimer.start();
        List<RagSearchResultDto> results = ragSearchService.search(query, TOP_K, SIMILARITY_THRESHOLD);
        context.setLastResults(results);

        log.info("tool=search_knowledge_base caller={} durationMs={} outcome=success chunksReturned={}",
                principal.getUsername(), ToolTimer.elapsedMs(startNanos), results.size());
        return results;
    }
}
