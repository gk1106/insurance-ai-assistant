package com.insuranceai.backend.ai.observability;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises {@link ToolObservabilityAspect} the same way Spring's auto-proxying applies it in
 * production for a Spring-managed {@code @Tool} bean (e.g. an MCP tool class): via a real AOP
 * proxy, not a direct method call.
 */
class ToolObservabilityAspectTest {

    interface Greeter {
        String greet(String name);

        String explode();
    }

    static class GreeterImpl implements Greeter {
        @Override
        @Tool(name = "test_greet", description = "greets someone")
        public String greet(String name) {
            return "hello " + name;
        }

        @Override
        @Tool(name = "test_explode", description = "always fails")
        public String explode() {
            throw new IllegalStateException("boom");
        }
    }

    private Greeter proxied() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new GreeterImpl());
        factory.addAspect(new ToolObservabilityAspect());
        return factory.getProxy();
    }

    @Test
    void aroundAdvice_returnsTheUnderlyingMethodsResultUnchanged() {
        assertThat(proxied().greet("world")).isEqualTo("hello world");
    }

    @Test
    void aroundAdvice_propagatesExceptionsRatherThanSwallowingThem() {
        assertThatThrownBy(() -> proxied().explode())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
    }
}
