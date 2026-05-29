package uk.gov.hmcts.appregister.common.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import uk.gov.hmcts.appregister.common.filter.LogMdcFilter;
import uk.gov.hmcts.appregister.common.security.UserProvider;

class SecurityEndpointFailureLoggerTest {

    private static final String SENSITIVE_TOKEN = "sensitive-token";
    private static final String SENSITIVE_SESSION = "sensitive-session";
    private static final String SAFE_USER_ID = "tenant-id:object-id";

    private LogCaptor logCaptor;

    @BeforeEach
    void beforeEach() {
        logCaptor = LogCaptor.forClass(SecurityEndpointFailureLogger.class);
        logCaptor.clearLogs();
    }

    @AfterEach
    void afterEach() {
        MDC.clear();
        logCaptor.clearLogs();
    }

    @Test
    void givenAuthenticationFailure_whenLogged_thenLogContainsSafeEndpointContext() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/hello");
        request.setQueryString("token=" + SENSITIVE_TOKEN);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + SENSITIVE_TOKEN);
        request.addHeader(HttpHeaders.COOKIE, "APPREG_SESSION=" + SENSITIVE_SESSION);

        SecurityEndpointFailureLogger logger =
                new SecurityEndpointFailureLogger(emptyUserProvider());
        logger.logFailure(request, 401, SecurityEndpointFailureLogger.AUTHENTICATION_FAILURE);

        String log = logCaptor.getWarnLogs().getFirst();
        assertThat(log)
                .contains(
                        "method=GET",
                        "path=/admin/hello",
                        "status=401",
                        "category=authentication_failure",
                        "user=anonymous")
                .doesNotContain(
                        "Bearer", SENSITIVE_TOKEN, "APPREG_SESSION", SENSITIVE_SESSION, "token=");
    }

    @Test
    void givenSafeUserAvailable_whenLogged_thenLogContainsUserIdentifier() {
        UserProvider userProvider = mock(UserProvider.class);
        when(userProvider.getUserId()).thenReturn(SAFE_USER_ID);

        ObjectProvider<UserProvider> userProviderProvider = mockUserProvider(userProvider);
        SecurityEndpointFailureLogger logger =
                new SecurityEndpointFailureLogger(userProviderProvider);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/hello");
        MDC.put(LogMdcFilter.USER, "anonymous");

        logger.logFailure(request, 403, SecurityEndpointFailureLogger.ACCESS_DENIED);

        assertThat(logCaptor.getWarnLogs().getFirst())
                .contains(
                        "method=GET",
                        "path=/admin/hello",
                        "status=403",
                        "category=access_denied",
                        "user=" + SAFE_USER_ID);
    }

    @Test
    void givenRequestUnavailable_whenLogged_thenFallsBackToMdcContext() {
        MDC.put(LogMdcFilter.METHOD, "POST");
        MDC.put(LogMdcFilter.PATH, "/from-mdc");
        MDC.put(LogMdcFilter.USER, "anonymous");

        SecurityEndpointFailureLogger logger =
                new SecurityEndpointFailureLogger(emptyUserProvider());
        logger.logFailure(null, 401, SecurityEndpointFailureLogger.AUTHENTICATION_FAILURE);

        assertThat(logCaptor.getWarnLogs().getFirst())
                .contains(
                        "method=POST",
                        "path=/from-mdc",
                        "status=401",
                        "category=authentication_failure",
                        "user=anonymous");
    }

    private static ObjectProvider<UserProvider> emptyUserProvider() {
        return mockUserProvider(null);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<UserProvider> mockUserProvider(UserProvider userProvider) {
        ObjectProvider<UserProvider> userProviderProvider = mock(ObjectProvider.class);
        when(userProviderProvider.getIfAvailable()).thenReturn(userProvider);
        return userProviderProvider;
    }
}
