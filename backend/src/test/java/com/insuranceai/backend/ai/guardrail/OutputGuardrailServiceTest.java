package com.insuranceai.backend.ai.guardrail;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutputGuardrailServiceTest {

    private final OutputGuardrailService outputGuardrailService = new OutputGuardrailService();

    @Test
    void sanitize_leavesOrdinaryReplyUnchanged() {
        String reply = "Your policy POL-2026-ABC12345 is ACTIVE and expires on 2027-01-15.";
        assertThat(outputGuardrailService.sanitize(reply)).isEqualTo(reply);
    }

    @Test
    void sanitize_redactsOpenAiStyleApiKey() {
        String reply = "Here is the key: sk-abcdefghijklmnopqrstuvwxyz123456";
        assertThat(outputGuardrailService.sanitize(reply))
                .doesNotContain("sk-abcdefghijklmnopqrstuvwxyz123456")
                .contains("[redacted]");
    }

    @Test
    void sanitize_redactsBearerToken() {
        String reply = "Use header Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.abc123def456";
        assertThat(outputGuardrailService.sanitize(reply))
                .doesNotContain("eyJhbGciOiJIUzI1NiJ9")
                .contains("[redacted]");
    }

    @Test
    void sanitize_redactsPasswordLikeContent() {
        String reply = "The database config is password=SuperSecret123";
        assertThat(outputGuardrailService.sanitize(reply)).contains("[redacted]");
    }

    @Test
    void sanitize_redactsJavaStackTraceFrames() {
        String reply = """
                Something went wrong:
                \tat com.insuranceai.backend.policy.service.impl.PolicyServiceImpl.getById(PolicyServiceImpl.java:64)
                \tat java.base/java.lang.Thread.run(Thread.java:840)
                """;
        String sanitized = outputGuardrailService.sanitize(reply);
        assertThat(sanitized).doesNotContain("PolicyServiceImpl.java");
        assertThat(sanitized).contains("[internal detail removed]");
    }

    @Test
    void sanitize_redactsRawExceptionClassNames() {
        String reply = "Failed with java.lang.IllegalArgumentException: Invalid UUID string: abc";
        assertThat(outputGuardrailService.sanitize(reply))
                .doesNotContain("IllegalArgumentException")
                .contains("[internal detail removed]");
    }
}
