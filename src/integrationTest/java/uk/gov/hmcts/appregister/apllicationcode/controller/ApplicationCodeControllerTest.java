package uk.gov.hmcts.appregister.apllicationcode.controller;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.stubs.RoleEnum;
import uk.gov.hmcts.appregister.testutils.stubs.TokenClient;
import uk.gov.hmcts.appregister.testutils.stubs.TokenGenerator;

public class ApplicationCodeControllerTest extends BaseIntegration {
    private static final String WEB_CONTEXT = "application-codes";

    @Test
    public void givenValidRequest_whenGetApplicationCodes_thenReturn200() throws Exception {
        createRequest(
                        WEB_CONTEXT,
                        TokenGenerator.builder()
                                .roles(List.of(RoleEnum.ADMIN.getRole()))
                                .build()
                                .fetchTokenForRole())
                .exchange()
                .expectStatus()
                .isCreated();
    }

    @Test
    public void givenValidRequest_whenGetApplicationCodes_thenReturn401() throws Exception {
        createRequest(
                        WEB_CONTEXT,
                        TokenGenerator.builder()
                                .expiredDate(
                                        Date.from(Instant.now().minusSeconds(TokenClient.SECONDS)))
                                .roles(List.of(RoleEnum.ADMIN.getRole()))
                                .build()
                                .fetchTokenForRole())
                .exchange()
                .expectStatus()
                .isCreated();
    }

    @Test
    public void givenValidRequest_whenGetApplicationCodes_thenReturn403() throws Exception {
        createRequest(
                        WEB_CONTEXT,
                        TokenGenerator.builder()
                                .roles(List.of(TokenClient.INVALID_ROLE))
                                .build()
                                .fetchTokenForRole())
                .exchange()
                .expectStatus()
                .isCreated();
    }
}
