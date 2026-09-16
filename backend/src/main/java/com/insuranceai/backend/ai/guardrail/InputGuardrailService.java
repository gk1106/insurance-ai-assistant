package com.insuranceai.backend.ai.guardrail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * The input guardrail: a deterministic, pre-LLM check every chat entry point (each domain agent's
 * {@code chat()} and {@link com.insuranceai.backend.ai.orchestrator.AiOrchestratorService#chat})
 * runs before the message ever reaches a ChatClient. It catches prompt-injection / jailbreak
 * attempts with plain pattern matching -- no model call, no cost, no latency, and no way for the
 * LLM itself to be talked into ignoring it, since it never sees the message if this rejects it.
 * <p>
 * This is deliberately narrow: it does not decide whether a message is "on topic" for insurance --
 * that's the Orchestrator's own router (an off-topic message already gets a graceful decline, see
 * {@code AiOrchestratorService.chat}) and each domain agent's system prompt (which already refuses
 * out-of-scope requests in its own domain). Duplicating that as a second, cruder keyword filter
 * here would risk rejecting legitimate questions the router or system prompt already handle
 * correctly.
 */
@Service
public class InputGuardrailService {

    private static final Logger log = LoggerFactory.getLogger(InputGuardrailService.class);

    private static final int MAX_MESSAGE_LENGTH = 4000;

    // Deliberately broad, deliberately simple: these exist to catch common jailbreak/prompt-
    // injection phrasing, not to be a general-purpose content classifier.
    private static final List<Pattern> BLOCKED_PATTERNS = List.of(
            Pattern.compile("ignore\\s+(all|any|the)?\\s*(previous|prior|above)\\s+instructions", Pattern.CASE_INSENSITIVE),
            Pattern.compile("disregard\\s+(all|any|the)?\\s*(previous|prior|above)\\s+instructions", Pattern.CASE_INSENSITIVE),
            Pattern.compile("reveal\\s+(your|the)\\s+(system\\s+)?prompt", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(show|print|what\\s+is)\\s+your\\s+system\\s+prompt", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you\\s+are\\s+now\\s+(no\\s+longer\\s+)?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bjailbreak\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bDAN\\s+mode\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("developer\\s+mode", Pattern.CASE_INSENSITIVE),
            Pattern.compile("pretend\\s+(you|to)\\s+(are|be)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("act\\s+as\\s+(if\\s+)?you\\s+(are|were)", Pattern.CASE_INSENSITIVE)
    );

    /**
     * @throws GuardrailViolationException if the message is empty, too long, or matches a known
     *                                      prompt-injection / jailbreak pattern.
     */
    public void assertSafe(String message) {
        if (message == null || message.isBlank()) {
            throw new GuardrailViolationException("Message must not be blank");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new GuardrailViolationException("Message is too long (max " + MAX_MESSAGE_LENGTH + " characters)");
        }
        for (Pattern pattern : BLOCKED_PATTERNS) {
            if (pattern.matcher(message).find()) {
                log.warn("input-guardrail blocked message, matched pattern='{}'", pattern.pattern());
                throw new GuardrailViolationException(
                        "This request can't be processed. Please ask a direct question about your policies, "
                                + "claims, or renewals.");
            }
        }
    }
}
