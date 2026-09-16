package com.insuranceai.backend.ai.observability;

/**
 * Execution-time measurement for the chat-agent tool classes ({@code ai.policy.tools},
 * {@code ai.claims.tools}, {@code ai.renewal.tools}) -- unlike the MCP tool beans, these are
 * plain {@code new}'d per chat turn (see each tool's {@code ToolContext} parameter), never
 * Spring-managed, so {@link ToolObservabilityAspect}'s proxy-based AOP advice cannot see their
 * calls at all. This tiny stopwatch is the deliberately low-tech alternative: each tool calls
 * {@link #start()} once and passes the result to {@link #elapsedMs} when logging its outcome,
 * the same two-line addition everywhere rather than a partial, silently-non-functional aspect.
 */
public final class ToolTimer {

    private ToolTimer() {
    }

    public static long start() {
        return System.nanoTime();
    }

    public static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
