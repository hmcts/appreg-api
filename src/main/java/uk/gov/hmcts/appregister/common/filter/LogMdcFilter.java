package uk.gov.hmcts.appregister.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.security.UserProvider;

/**
 * A filter that allows us to setup an MDC with the user, the method name and the url context.
 */
@Component
@RequiredArgsConstructor
public class LogMdcFilter extends OncePerRequestFilter {
    /** The user name key that is stored in the log MDC. */
    public static final String USER = "user";

    /** The method key that is stored in the log MDC. */
    public static final String METHOD = "method";

    /** The Path key that is stored in the log MDC. */
    public static final String PATH = "path";

    private final UserProvider userProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                try {
                    MDC.put(USER, userProvider.getUserId());
                } catch (AppRegistryException appRegistryException) {
                    MDC.put(USER, "anonymous");
                }
            } else {
                MDC.put(USER, "anonymous");
            }

            // add the context path
            String contextPath = request.getContextPath();
            if (contextPath.length() > 0) {
                MDC.put(PATH, contextPath);
            } else {
                MDC.put(PATH, "/");
            }

            // add the method
            String method = request.getMethod();
            MDC.put(METHOD, method);

            filterChain.doFilter(request, response);
        } finally {
            // remove the method, path and user
            MDC.remove(METHOD);
            MDC.remove(PATH);
            MDC.remove(USER);
        }
    }
}
