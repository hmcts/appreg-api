package uk.gov.hmcts.appregister.testutils.controller;

import java.net.MalformedURLException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;

/**
 * Tests a set of negative security scenarios for a specific API controller:-
 *
 * <p>- Authentication fails with a 401 when we find an incorrect issuer, audience, invalid
 * signature - Authorisation fals with a 403 when the wrong role is presented.
 */
public abstract class AbstractSecurityControllerTest extends BaseIntegration {

    /** The set of negative security contexts to be tested. */
    protected abstract RestEndpointDescription[] getRestDescriptions() throws MalformedURLException;

    @Test
    public void givenValidRequest_whenCalledWithAnExpiredToken_thenReturn401() throws Exception {
        for (RestEndpointDescription context : getRestDescriptions()) {
            context.process(
                            restAssuredClient,
                            getATokenWithValidCredentials()
                                    .expiredDate(
                                            Date.from(
                                                    Instant.now()
                                                            .minusSeconds(20 * 60L))) // expired 20
                                    // minutes ago
                                    .build()
                                    .fetchTokenForRole())
                    .then()
                    .statusCode(401);
        }
    }

    @Test
    public void givenValidRequest_whenCalledWithAnInvalidSignature_thenReturn401()
            throws Exception {
        for (RestEndpointDescription context : getRestDescriptions()) {
            context.process(
                            restAssuredClient,
                            getATokenWithValidCredentials()
                                    .invalidToken(true)
                                    .build()
                                    .fetchTokenForRole())
                    .then()
                    .statusCode(401);
        }
    }

    @Test
    public void givenValidRequest_whenCalledWithAnInvalidIssuer_thenReturn401() throws Exception {
        for (RestEndpointDescription context : getRestDescriptions()) {
            context.process(
                            restAssuredClient,
                            getATokenWithValidCredentials()
                                    .issuer("invalid-issuer")
                                    .build()
                                    .fetchTokenForRole())
                    .then()
                    .statusCode(401);
        }
    }

    @Test
    public void givenValidRequest_whenCalledWithAnInvalidAudience_thenReturn401() throws Exception {
        for (RestEndpointDescription context : getRestDescriptions()) {
            context.process(
                            restAssuredClient,
                            getATokenWithValidCredentials()
                                    .audience("invalid-audience")
                                    .build()
                                    .fetchTokenForRole())
                    .then()
                    .statusCode(401);
        }
    }

    @Test
    public void givenValidRequest_whenGetIncorrectRole_thenReturn403() throws Exception {
        for (RestEndpointDescription context : getRestDescriptions()) {
            context.process(
                            restAssuredClient,
                            getATokenWithValidCredentials()
                                    .roles(List.of(context.getInvalidRole().getRole()))
                                    .build()
                                    .fetchTokenForRole())
                    .then()
                    .statusCode(403);
        }
    }
}
