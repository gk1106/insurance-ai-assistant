package com.insuranceai.backend.ai.guardrail;

/**
 * Raised by {@link InputGuardrailService} when a message is rejected before it ever reaches an
 * agent's ChatClient -- kept distinct from {@link com.insuranceai.backend.common.exception.BusinessRuleViolationException}
 * so guardrail rejections are separately identifiable in logs and in the evaluation suite, even
 * though both map to the same 400 response shape for the caller.
 */
public class GuardrailViolationException extends RuntimeException {

    public GuardrailViolationException(String message) {
        super(message);
    }
}
