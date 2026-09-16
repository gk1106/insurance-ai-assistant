package com.insuranceai.backend.ai.observability;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void nonAiRequest_isPassedThroughWithoutTouchingMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/policies");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isNull();
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void aiRequestWithNoHeader_getsAGeneratedCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ai/chat");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        String header = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertThat(header).isNotBlank();
        // MDC is cleared again once the filter returns -- assert it was set *during* the chain by
        // capturing it inside a chain implementation instead of checking after the fact.
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void aiRequest_honorsAValidCallerSuppliedCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ai/chat");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "caller-supplied-id-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("caller-supplied-id-123");
    }

    @Test
    void aiRequest_rejectsAnUnsafeCallerSuppliedHeaderAndGeneratesItsOwn() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ai/chat");
        // Newline would let a caller forge extra log lines if it reached MDC/the log pattern
        // unsanitized.
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "evil\nid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        String header = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertThat(header).isNotEqualTo("evil\nid");
        assertThat(header).matches("^[A-Za-z0-9-]{1,64}$");
    }

    @Test
    void mdcIsSetDuringTheChainAndClearedAfterward() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ai/policy-agent/chat");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] mdcValueDuringChain = new String[1];
        FilterChain chain = (req, res) -> mdcValueDuringChain[0] = MDC.get(CorrelationIdFilter.MDC_KEY);

        filter.doFilter(request, response, chain);

        assertThat(mdcValueDuringChain[0]).isNotBlank();
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
