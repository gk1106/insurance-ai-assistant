package com.insuranceai.backend.ai.guardrail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * The output guardrail: the last thing that touches an agent's reply text before it's returned to
 * the caller. Every domain agent's {@code chat()} and the Orchestrator's {@code chat()} run their
 * final reply through {@link #sanitize} -- a deterministic, pattern-based redaction pass, not a
 * second model call, so it can never itself leak the thing it's trying to remove and adds no
 * latency worth measuring.
 * <p>
 * This only ever replaces a matched span with a neutral placeholder; it never rewrites or
 * shortens text that doesn't match, so a normal reply is returned byte-for-byte unchanged.
 */
@Service
public class OutputGuardrailService {

    private static final Logger log = LoggerFactory.getLogger(OutputGuardrailService.class);

    private static final String REDACTED = "[redacted]";
    private static final String INTERNAL_DETAIL_REMOVED = "[internal detail removed]";

    // Credential/secret-shaped content that must never reach the client, however it got into the
    // reply (a misbehaving tool, a copied error message, etc.).
    private static final List<Pattern> SENSITIVE_PATTERNS = List.of(
            Pattern.compile("sk-[A-Za-z0-9_-]{16,}"),                 // OpenAI-style API keys
            Pattern.compile("Bearer\\s+[A-Za-z0-9._-]{20,}"),          // bearer tokens / JWTs
            Pattern.compile("(?i)\\bpassword\\b\\s*[:=]\\s*\\S+"),
            Pattern.compile("(?i)\\b(api[_-]?key|secret)\\b\\s*[:=]\\s*\\S+")
    );

    // Internal implementation detail that should never reach a user-facing reply: raw exception
    // class names, stack trace frames, or a bare Java/Spring package path.
    private static final List<Pattern> INTERNAL_LEAK_PATTERNS = List.of(
            Pattern.compile("(?m)^\\s*at\\s+[\\w.$]+\\([^)]*\\)\\s*$"),
            Pattern.compile("Exception in thread"),
            Pattern.compile("\\b(?:java|javax|jakarta|org\\.springframework|org\\.hibernate|com\\.insuranceai)"
                    + "(?:\\.[\\w$]+)*(?:Exception|Error)\\b")
    );

    public String sanitize(String reply) {
        if (reply == null || reply.isBlank()) {
            return reply;
        }

        String result = reply;
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            if (pattern.matcher(result).find()) {
                log.warn("output-guardrail redacted sensitive content, matched pattern='{}'", pattern.pattern());
                result = pattern.matcher(result).replaceAll(REDACTED);
            }
        }
        for (Pattern pattern : INTERNAL_LEAK_PATTERNS) {
            if (pattern.matcher(result).find()) {
                log.warn("output-guardrail redacted internal detail, matched pattern='{}'", pattern.pattern());
                result = pattern.matcher(result).replaceAll(INTERNAL_DETAIL_REMOVED);
            }
        }
        return result;
    }
}
