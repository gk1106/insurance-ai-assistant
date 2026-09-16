package com.insuranceai.backend.ai.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Wraps a single call to the (shared, model-agnostic) OpenAI ChatModel -- a router decision, a
 * synthesis pass, or a full agent tool-use loop -- with request/response timing and outcome
 * logging. Every domain agent's {@code chat()} and the Orchestrator's router/synthesis calls go
 * through {@link #timed}, so "which model, how long, did it succeed" is logged the same way
 * everywhere rather than each call site rolling its own {@code System.nanoTime()} bookkeeping.
 * <p>
 * Only the model name and duration are logged -- never the prompt or the response content, which
 * may contain customer data; that's the job of each call site's own (already-existing, already
 * output-guardrailed) logging.
 */
@Component
public class AiCallObservability {

    private static final Logger log = LoggerFactory.getLogger(AiCallObservability.class);

    private final String modelName;

    public AiCallObservability(@Value("${spring.ai.openai.chat.options.model}") String modelName) {
        this.modelName = modelName;
    }

    /**
     * @param callLabel identifies which call this is (e.g. "policy-agent-chat",
     *                   "orchestrator-route") -- always a fixed, static string; the caller must
     *                   never pass user input or prompt content here.
     */
    public <T> T timed(String callLabel, Supplier<T> aiCall) {
        long startNanos = System.nanoTime();
        log.info("ai-request label={} model={}", callLabel, modelName);
        try {
            T result = aiCall.get();
            log.info("ai-response label={} model={} durationMs={} outcome=success",
                    callLabel, modelName, elapsedMs(startNanos));
            return result;
        } catch (RuntimeException ex) {
            log.warn("ai-response label={} model={} durationMs={} outcome=error errorType={}",
                    callLabel, modelName, elapsedMs(startNanos), ex.getClass().getSimpleName());
            throw ex;
        }
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
