package uk.gov.hmcts.appregister.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Slf4j
@Component
public class HttpRequestLoggingFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID = "x-correlation-id";
    private static final String REQUEST_ID = "request_id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        long startNs = System.nanoTime();

        // Ensure we always have a request id
        String requestId = request.getHeader(CORRELATION_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(REQUEST_ID, requestId);

        // echo the header so downstream and responses carry it
        response.setHeader(CORRELATION_ID, requestId);

        // Route the template if available (e.g., "/orders/{id}")
        String routeTemplate =
                (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        // Optional DEBUG “started”
        if (log.isDebugEnabled()) {
            String route = routeTemplate != null ? routeTemplate : request.getRequestURI();
            log.debug("http_request_started route={} method={}", route, request.getMethod());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = Math.round((System.nanoTime() - startNs) / 1_000_000.0);

            // Prefer route template, fall back to raw URI if not resolved (static resources, 404)
            String route =
                    (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            if (route == null) {
                route = request.getRequestURI();
            }

            // Single INFO line on completion (per your guide)
            log.info(
                    "http_request_completed route={} method={} status={} duration_ms={} request_id={}",
                    route,
                    request.getMethod(),
                    response.getStatus(),
                    durationMs,
                    requestId);

            MDC.remove(REQUEST_ID);
        }
    }
}
