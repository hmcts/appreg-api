package uk.gov.hmcts.appregister.common.log;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;

class RequestCachingFilterTest {

    private final RequestCachingFilter filter = new RequestCachingFilter();

    @Test void givenNormalRequest_whenFiltered_thenRequestIsWrappedAndPayloadCanBeCached()
        throws Exception {
        byte[] payload = """
             {
             "courtCode": "LOC123"
             }
             """
            .getBytes(StandardCharsets.UTF_8);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(payload);
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<ServletRequest> filteredRequest = new AtomicReference<>();

        filter.doFilter(
            request,
            response,
            (ServletRequest servletRequest, ServletResponse servletResponse) -> {
                filteredRequest.set(servletRequest);

                servletRequest.getInputStream().readAllBytes();
            });

        assertTrue(filteredRequest.get() instanceof ContentCachingRequestWrapper);

        ContentCachingRequestWrapper wrappedRequest =
            (ContentCachingRequestWrapper) filteredRequest.get();

        assertArrayEquals(payload, wrappedRequest.getContentAsByteArray());
    }

    @Test void givenAlreadyCachedRequest_whenFiltered_thenSameRequestIsUsed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request,0);

        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<ServletRequest> filteredRequest = new AtomicReference<>();

        filter.doFilter(
            wrappedRequest,
            response,
            (ServletRequest servletRequest, ServletResponse servletResponse) ->
                filteredRequest.set(servletRequest));

        assertSame(wrappedRequest, filteredRequest.get());
    }
}

