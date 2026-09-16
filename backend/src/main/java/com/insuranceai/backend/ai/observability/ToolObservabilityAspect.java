package com.insuranceai.backend.ai.observability;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Cross-cutting execution-time and outcome logging for {@code @Tool}-annotated methods on
 * Spring-managed beans -- concretely, the MCP tool classes ({@code PolicyMcpTools} et al.), which
 * are {@code @Component}s -- without touching those (already reviewed, already tested) classes.
 * <p>
 * This deliberately does <b>not</b> cover the chat-agent tool classes ({@code ai.policy.tools} /
 * {@code ai.claims.tools} / {@code ai.renewal.tools}): those are plain {@code new}'d objects
 * built fresh per chat turn (see each one's {@code ToolContext} constructor parameter), never
 * Spring beans, so they never pass through a proxy this (or any proxy-based Spring AOP) aspect
 * could intercept -- an aspect here would silently do nothing for them. Their timing is measured
 * directly instead, via {@link ToolTimer}, in each tool's own existing log lines.
 * <p>
 * Deliberately logs only the exception's class name on failure, never its message or stack trace
 * -- the tool's own catch block already logs a human-authored, already-safe message; this aspect
 * doesn't need to (and must not risk) repeating raw exception detail.
 */
@Aspect
@Component
public class ToolObservabilityAspect {

    private static final Logger log = LoggerFactory.getLogger(ToolObservabilityAspect.class);

    @Around("@annotation(tool)")
    public Object logToolExecution(ProceedingJoinPoint joinPoint, Tool tool) throws Throwable {
        String toolName = StringUtils.hasText(tool.name()) ? tool.name() : joinPoint.getSignature().getName();
        long startNanos = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            log.info("tool-execution name={} durationMs={} outcome=success", toolName, elapsedMs(startNanos));
            return result;
        } catch (Throwable ex) {
            log.warn("tool-execution name={} durationMs={} outcome=error errorType={}",
                    toolName, elapsedMs(startNanos), ex.getClass().getSimpleName());
            throw ex;
        }
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
