package uk.gov.hmcts.appregister.common.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Servlet filter that ensures every incoming request has a correlation ID.
 *
 * <p>The filter does the following:
 *
 * <ul>
 *   <li>Reads an {@code X-Correlation-Id} header from the request, if present.
 *   <li>If missing, generates a new UUID to act as the correlation ID.
 *   <li>Stores the correlation ID on the request attributes (under {@code "correlationId"}) so
 *       downstream components can access it.
 *   <li>Puts the correlation ID into SLF4J's {@link MDC} so it appears in log statements for the
 *       current thread during request processing.
 *   <li>Removes the MDC entry in a {@code finally} block to avoid leaking values across threads.
 * </ul>
 *
 * This enables end-to-end request tracing across services and consistent log correlation.
 */
@Component
public class CorrelationFilter implements Filter {

    /**
     * Adds/propagates a correlation ID for the current request and ensures it is present in logs.
     *
     * @param request the incoming servlet request
     * @param response the outgoing servlet response
     * @param chain the remaining filter chain
     * @throws IOException if an I/O error occurs during filtering
     * @throws ServletException if the filter chain throws a servlet exception
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // Cast to HttpServletRequest to access HTTP-specific methods such as headers.
        HttpServletRequest req = (HttpServletRequest) request;

        // Try to read an existing correlation ID from the standard header.
        // If absent, generate a new random UUID so every request is traceable.
        String cid =
                Optional.ofNullable(req.getHeader("X-Correlation-Id"))
                        .orElse(UUID.randomUUID().toString());

        // Expose the correlation ID to downstream code via a request attribute.
        // (e.g., controllers or other filters can read it with
        // request.getAttribute("correlationId")).
        req.setAttribute("correlationId", cid);

        // Put the correlation ID into MDC so it is automatically included in log patterns
        // configured to print MDC keys (e.g., %X{correlationId}).
        MDC.put("correlationId", cid);

        try {
            // Continue processing the rest of the filter chain.
            chain.doFilter(request, response);
        } finally {
            // Always remove the MDC value to prevent it leaking into subsequent requests
            // handled by the same thread in a thread pool.
            MDC.remove("correlationId");
        }
    }
}
