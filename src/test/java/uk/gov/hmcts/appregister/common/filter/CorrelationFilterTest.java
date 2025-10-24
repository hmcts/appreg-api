package uk.gov.hmcts.appregister.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrelationFilterTest {

    @Mock
    HttpServletRequest request;

    @Mock
    ServletResponse response;

    @Mock
    FilterChain chain;

    @InjectMocks
    CorrelationFilter filter;

    @AfterEach
    void tearDown() {
        // Safety: ensure no MDC leakage between tests
        MDC.remove("correlationId");
    }

    @Test
    void usesHeaderCorrelationId_setsAttribute_andPropagatesToMdcDuringChain_thenClears() throws Exception {
        String headerCid = "abc-123";
        when(request.getHeader("X-Correlation-Id")).thenReturn(headerCid);

        // During chain execution, MDC should already contain the correlationId
        doAnswer(invocation -> {
            assertThat(MDC.get("correlationId")).isEqualTo(headerCid);
            return null;
        }).when(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));

        filter.doFilter(request, response, chain);

        // Attribute set on the request
        verify(request).setAttribute("correlationId", headerCid);
        // Chain invoked once
        verify(chain, times(1)).doFilter(request, response);
        // After completion, MDC must be cleared
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void generatesCorrelationId_whenHeaderMissing_setsAttribute_andClearsMdc() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn(null);

        ArgumentCaptor<Object> attrValue = ArgumentCaptor.forClass(Object.class);

        // Verify MDC contains the same ID that was set as attribute during chain
        doAnswer(invocation -> {
            String inMdc = MDC.get("correlationId");
            // We can't predict the UUID, but it must be non-null and look like a UUID
            assertThat(inMdc).isNotNull();
            assertThat(isUuid(inMdc)).isTrue();
            return null;
        }).when(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));

        filter.doFilter(request, response, chain);

        verify(request).setAttribute(eq("correlationId"), attrValue.capture());
        Object generated = attrValue.getValue();
        assertThat(generated).isInstanceOf(String.class);
        assertThat(isUuid((String) generated)).isTrue();

        // MDC cleared afterwards
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void clearsMdc_evenWhenChainThrows() throws Exception {
        String headerCid = "boom-999";
        when(request.getHeader("X-Correlation-Id")).thenReturn(headerCid);

        doAnswer(invocation -> {
            // Inside chain, MDC must be set
            assertThat(MDC.get("correlationId")).isEqualTo(headerCid);
            throw new ServletException("downstream failure");
        }).when(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
            .isInstanceOf(ServletException.class)
            .hasMessageContaining("downstream failure");

        // MDC must be cleared even after exception
        assertThat(MDC.get("correlationId")).isNull();
    }

    private static boolean isUuid(String s) {
        // Simple UUID v4-ish format check (accepts any UUID format with hyphens)
        Pattern p = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
        return p.matcher(s).matches();
    }
}
