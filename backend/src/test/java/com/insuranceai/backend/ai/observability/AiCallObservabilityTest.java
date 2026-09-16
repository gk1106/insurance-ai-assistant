package com.insuranceai.backend.ai.observability;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiCallObservabilityTest {

    private final AiCallObservability aiCallObservability = new AiCallObservability("test-model");

    @Test
    void timed_returnsTheCallsResultUnchanged() {
        String result = aiCallObservability.timed("test-call", () -> "the-reply");
        assertThat(result).isEqualTo("the-reply");
    }

    @Test
    void timed_actuallyInvokesTheSupplierExactlyOnce() {
        AtomicInteger invocations = new AtomicInteger();
        aiCallObservability.timed("test-call", () -> {
            invocations.incrementAndGet();
            return null;
        });
        assertThat(invocations.get()).isEqualTo(1);
    }

    @Test
    void timed_propagatesExceptionsRatherThanSwallowingThem() {
        RuntimeException failure = new IllegalStateException("model unavailable");
        assertThatThrownBy(() -> aiCallObservability.timed("test-call", () -> {
            throw failure;
        })).isSameAs(failure);
    }
}
