package uk.gov.hmcts.appregister.common.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.security.UserProvider;

class LogMdcFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void doFilterInternal_populatesMdcForAuthenticatedUser() throws ServletException, IOException {
        var userProvider = mock(UserProvider.class);
        when(userProvider.getUserId()).thenReturn("tenant:user");
        var authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        var request = new MockHttpServletRequest("POST", "/reports");
        var response = new MockHttpServletResponse();
        var filter = new LogMdcFilter(userProvider);
        FilterChain chain =
                (req, res) -> {
                    assertEquals("tenant:user", MDC.get(LogMdcFilter.USER));
                    assertEquals("POST", MDC.get(LogMdcFilter.METHOD));
                    assertEquals("/reports", MDC.get(LogMdcFilter.PATH));
                };

        filter.doFilterInternal(request, response, chain);

        assertNull(MDC.get(LogMdcFilter.USER));
        assertNull(MDC.get(LogMdcFilter.METHOD));
        assertNull(MDC.get(LogMdcFilter.PATH));
    }

    @Test
    void doFilterInternal_fallsBackToAnonymousAndDefaultPath()
            throws ServletException, IOException {
        SecurityContextHolder.clearContext();

        var request = new MockHttpServletRequest();
        request.setMethod("GET");
        var response = new MockHttpServletResponse();
        var filter = new LogMdcFilter(mock(UserProvider.class));
        FilterChain chain =
                (req, res) -> {
                    assertEquals("anonymous", MDC.get(LogMdcFilter.USER));
                    assertEquals("GET", MDC.get(LogMdcFilter.METHOD));
                    assertEquals("/", MDC.get(LogMdcFilter.PATH));
                };

        filter.doFilterInternal(request, response, chain);
    }

    @Test
    void doFilterInternal_fallsBackToAnonymousWhenUserLookupFails()
            throws ServletException, IOException {
        var userProvider = mock(UserProvider.class);
        when(userProvider.getUserId())
                .thenThrow(new AppRegistryException(mock(ErrorCodeEnum.class), "bad token"));
        var authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        var request = new MockHttpServletRequest("GET", " ");
        var response = new MockHttpServletResponse();
        var filter = new LogMdcFilter(userProvider);
        FilterChain chain =
                (req, res) -> {
                    assertEquals("anonymous", MDC.get(LogMdcFilter.USER));
                    assertEquals("GET", MDC.get(LogMdcFilter.METHOD));
                    assertEquals("/", MDC.get(LogMdcFilter.PATH));
                };

        filter.doFilterInternal(request, response, chain);
    }

    @Test
    void doFilterInternal_fallsBackToAnonymousWhenAuthenticationIsNotAuthenticated()
            throws ServletException, IOException {
        var authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var request = new MockHttpServletRequest("PATCH", "/jobs");
        var response = new MockHttpServletResponse();
        var filter = new LogMdcFilter(mock(UserProvider.class));
        FilterChain chain =
                (req, res) -> {
                    assertEquals("anonymous", MDC.get(LogMdcFilter.USER));
                    assertEquals("PATCH", MDC.get(LogMdcFilter.METHOD));
                    assertEquals("/jobs", MDC.get(LogMdcFilter.PATH));
                };

        filter.doFilterInternal(request, response, chain);
    }
}
