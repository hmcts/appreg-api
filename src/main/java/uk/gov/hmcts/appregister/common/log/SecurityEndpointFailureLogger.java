package uk.gov.hmcts.appregister.common.log;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.filter.LogMdcFilter;
import uk.gov.hmcts.appregister.common.security.UserProvider;

/**
 * Logs authentication and authorisation endpoint failures without exposing credentials.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityEndpointFailureLogger {

    public static final String AUTHENTICATION_FAILURE = "authentication_failure";
    public static final String ACCESS_DENIED = "access_denied";

    private static final String ANONYMOUS = "anonymous";
    private static final String ERROR_PATH = "/error";
    private static final String UNKNOWN_METHOD = "UNKNOWN";
    private static final String ROOT_PATH = "/";

    private final ObjectProvider<UserProvider> userProvider;

    public void logFailure(HttpServletRequest request, int statusCode, String category) {
        if (shouldSkip(request)) {
            return;
        }

        log.warn(
                "Endpoint security response method={} path={} status={} category={} user={}",
                resolveMethod(request),
                resolvePath(request),
                statusCode,
                category,
                resolveUser());
    }

    private boolean shouldSkip(HttpServletRequest request) {
        if (request != null) {
            if (request.getDispatcherType() == DispatcherType.ERROR) {
                return true;
            }

            var requestUri = request.getRequestURI();
            return ERROR_PATH.equals(requestUri);
        }

        return ERROR_PATH.equals(MDC.get(LogMdcFilter.PATH));
    }

    private String resolveMethod(HttpServletRequest request) {
        if (request != null && request.getMethod() != null && !request.getMethod().isBlank()) {
            return request.getMethod();
        }

        String method = MDC.get(LogMdcFilter.METHOD);
        return method != null && !method.isBlank() ? method : UNKNOWN_METHOD;
    }

    private String resolvePath(HttpServletRequest request) {
        if (request != null
                && request.getRequestURI() != null
                && !request.getRequestURI().isBlank()) {
            return request.getRequestURI();
        }

        String path = MDC.get(LogMdcFilter.PATH);
        return path != null && !path.isBlank() ? path : ROOT_PATH;
    }

    private String resolveUser() {
        String providerUser = resolveUserFromProvider();
        if (providerUser != null) {
            return providerUser;
        }

        String mdcUser = MDC.get(LogMdcFilter.USER);
        if (mdcUser != null && !mdcUser.isBlank()) {
            return mdcUser;
        }

        return ANONYMOUS;
    }

    private String resolveUserFromProvider() {
        UserProvider provider = userProvider.getIfAvailable();
        if (provider == null) {
            return null;
        }

        try {
            return provider.getUserId();
        } catch (AppRegistryException ignored) {
            return null;
        }
    }
}
