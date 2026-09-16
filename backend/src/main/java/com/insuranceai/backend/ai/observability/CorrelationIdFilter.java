package com.insuranceai.backend.ai.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Assigns every {@code /api/ai/**} request a correlation id, stamped into SLF4J's MDC for the
 * lifetime of the request so it automatically prefixes every log line the request touches --
 * Orchestrator, agent, tool, RAG, and any domain service it calls into -- without changing a
 * single one of those classes' own logging. Also echoed back as a response header so a client (or
 * a support ticket) can hand it back for log correlation.
 * <p>
 * A caller-supplied {@code X-Correlation-Id} is honored (useful when this app is one hop in a
 * larger traced system) but only after validation: MDC values land directly in log output, so an
 * unvalidated header would let a caller inject characters (newlines, control sequences) into the
 * log stream. An invalid or absent header gets a fresh id instead of being rejected -- this is
 * observability, not a security boundary.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String MDC_KEY = "correlationId";
    public static final String HEADER_NAME = "X-Correlation-Id";
    private static final String AI_PATH_PREFIX = "/api/ai/";
    private static final Pattern VALID_CORRELATION_ID = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(AI_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String correlationId = resolveCorrelationId(request);
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String header = request.getHeader(HEADER_NAME);
        if (StringUtils.hasText(header) && VALID_CORRELATION_ID.matcher(header).matches()) {
            return header;
        }
        return UUID.randomUUID().toString();
    }
}
