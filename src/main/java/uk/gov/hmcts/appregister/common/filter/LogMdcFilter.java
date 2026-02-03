package uk.gov.hmcts.appregister.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import uk.gov.hmcts.appregister.common.security.UserProvider;

import java.io.IOException;

/**
 * A filter that allows us to setup an MDC with the user, the method name and the url context
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LogMdcFilter extends OncePerRequestFilter {
    /** The user name */
    public static final String USER = "user";

    /** The method */
    public static final String METHOD = "method";

    /** The Path */
    public static final String PATH = "path";

    private final UserProvider userProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,

                                    HttpServletResponse response,
                                    FilterChain filterChain)
        throws ServletException, IOException {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                MDC.put(USER, userProvider.getUserId());
            } else {
                MDC.put(USER, "anonymous");
            }
            log.debug("Extracted the user name");

            // add the context path
            String contextPath = request.getContextPath();
            if (contextPath.length() > 0) {
                MDC.put(PATH, contextPath);
            } else {
                MDC.put(PATH, "/");
            }
            log.debug("Extracted the context of the request {}", contextPath);

            // add the method
            String method = request.getMethod();
            MDC.put(METHOD, method);
            log.debug("Extracted the method of the request {}", method);

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

}
