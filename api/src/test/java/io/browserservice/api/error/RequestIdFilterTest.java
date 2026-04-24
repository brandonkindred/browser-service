package io.browserservice.api.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class RequestIdFilterTest {

    @Test
    void generatesIdWhenMissingAndSetsHeader() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        AtomicReference<String> observedDuringChain = new AtomicReference<>();
        doAnswer(inv -> {
            observedDuringChain.set(MDC.get(RequestIdFilter.MDC_KEY));
            return null;
        }).when(chain).doFilter(req, resp);

        new RequestIdFilter().doFilter(req, resp, chain);

        assertThat(observedDuringChain.get()).isNotNull();
        verify(resp).setHeader(eq(RequestIdFilter.HEADER), any());
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void reusesProvidedHeader() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        org.mockito.Mockito.when(req.getHeader(RequestIdFilter.HEADER)).thenReturn("abc-123");

        AtomicReference<String> observed = new AtomicReference<>();
        doAnswer(inv -> {
            observed.set(MDC.get(RequestIdFilter.MDC_KEY));
            return null;
        }).when(chain).doFilter(req, resp);

        new RequestIdFilter().doFilter(req, resp, chain);

        assertThat(observed.get()).isEqualTo("abc-123");
        verify(resp).setHeader(RequestIdFilter.HEADER, "abc-123");
    }

    @Test
    void blankHeaderIsTreatedAsMissing() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        org.mockito.Mockito.when(req.getHeader(RequestIdFilter.HEADER)).thenReturn("  ");
        AtomicReference<String> observed = new AtomicReference<>();
        doAnswer(inv -> {
            observed.set(MDC.get(RequestIdFilter.MDC_KEY));
            return null;
        }).when(chain).doFilter(req, resp);

        new RequestIdFilter().doFilter(req, resp, chain);

        assertThat(observed.get()).isNotBlank();
        assertThat(observed.get()).isNotEqualTo("  ");
    }

    @Test
    void currentRequestIdReadsMdc() {
        MDC.put(RequestIdFilter.MDC_KEY, "xyz");
        try {
            assertThat(RequestIdFilter.currentRequestId()).isEqualTo("xyz");
        } finally {
            MDC.remove(RequestIdFilter.MDC_KEY);
        }
    }

    private static String eq(String expected) {
        return org.mockito.ArgumentMatchers.eq(expected);
    }
}
