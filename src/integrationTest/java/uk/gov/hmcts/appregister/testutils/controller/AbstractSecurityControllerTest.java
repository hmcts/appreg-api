package uk.gov.hmcts.appregister.testutils.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;

/**
 * Tests a set of negative security scenarios for a specific API controller:-
 *
 * <p>- Authentication fails with a 401 when we find an incorrect issuer, audience, invalid
 * signature - Authorisation fals with a 403 when the wrong role is presented.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractSecurityControllerTest extends BaseIntegration {

    /** The stream of negative security contexts to be tested. */
    protected abstract Stream<RestEndpointDescription> getDescriptions() throws Exception;

    @ParameterizedTest
    @MethodSource("getDescriptions")
    public void givenValidRequest_whenCalledWithAnExpiredToken_thenReturn401(
            RestEndpointDescription restEndpointDescription) throws Exception {
        restEndpointDescription
                .process(
                        restAssuredClient,
                        getATokenWithValidCredentials()
                                .expiredDate(
                                        Date.from(
                                                Instant.now().minusSeconds(20 * 60L))) // expired 20
                                // minutes ago
                                .build()
                                .fetchTokenForRole())
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @ParameterizedTest
    @MethodSource("getDescriptions")
    public void givenValidRequest_whenCalledWithAnInvalidSignature_thenReturn401(
            RestEndpointDescription restEndpointDescription) throws Exception {
        restEndpointDescription
                .process(
                        restAssuredClient,
                        getATokenWithValidCredentials()
                                .invalidToken(true)
                                .build()
                                .fetchTokenForRole())
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @ParameterizedTest
    @MethodSource("getDescriptions")
    public void givenValidRequest_whenCalledWithAnInvalidIssuer_thenReturn401(
            RestEndpointDescription restEndpointDescription) throws Exception {
        restEndpointDescription
                .process(
                        restAssuredClient,
                        getATokenWithValidCredentials()
                                .issuer("invalid-issuer")
                                .build()
                                .fetchTokenForRole())
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @ParameterizedTest
    @MethodSource("getDescriptions")
    public void givenValidRequest_whenCalledWithAnInvalidAudience_thenReturn401(
            RestEndpointDescription restEndpointDescription) throws Exception {
        restEndpointDescription
                .process(
                        restAssuredClient,
                        getATokenWithValidCredentials()
                                .audience("invalid-audience")
                                .build()
                                .fetchTokenForRole())
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @ParameterizedTest
    @MethodSource("getDescriptions")
    public void givenValidRequest_whenGetIncorrectRole_thenReturn403(
            RestEndpointDescription restEndpointDescription) throws Exception {
        restEndpointDescription
                .process(
                        restAssuredClient,
                        getATokenWithValidCredentials()
                                .roles(List.of(restEndpointDescription.getInvalidRole().getRole()))
                                .build()
                                .fetchTokenForRole())
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @ParameterizedTest
    @MethodSource("getDescriptions")
    public void givenValidRequest_whenCalledWithAnValidSignature_thenReturn200(
            RestEndpointDescription restEndpointDescription) throws Exception {
        restEndpointDescription
                .process(
                        restAssuredClient,
                        getATokenWithValidCredentials()
                                .roles(List.of(restEndpointDescription.getSuccessRole().getRole()))
                                .build()
                                .fetchTokenForRole())
                .then()
                .statusCode(is(not(HttpStatus.UNAUTHORIZED)))
                .statusCode(is(not(HttpStatus.FORBIDDEN)));
    }
}
