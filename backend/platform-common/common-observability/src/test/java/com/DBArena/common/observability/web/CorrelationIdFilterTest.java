package com.DBArena.common.observability.web;

import com.DBArena.common.observability.MdcKeys;
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
    void generatesACorrelationIdWhenNoneIsSupplied() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        String responseHeader = response.getHeader(CorrelationIdFilter.HEADER);
        assertThat(responseHeader).isNotBlank();
        assertThat(MDC.get(MdcKeys.CORRELATION_ID)).isNull(); // cleared after the chain
        verify(chain).doFilter(request, response);
    }

    @Test
    void propagatesAnIncomingCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "incoming-id-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("incoming-id-123");
    }

    @Test
    void mdcIsPopulatedWhileTheChainRuns() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "during-chain-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> assertThat(MDC.get(MdcKeys.CORRELATION_ID)).isEqualTo("during-chain-id");

        filter.doFilter(request, response, chain);
    }
}
