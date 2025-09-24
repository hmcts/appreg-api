package uk.gov.hmcts.appregister.controller;

import io.restassured.response.Response;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import uk.gov.hmcts.appregister.audit.AuditEventEnum;
import uk.gov.hmcts.appregister.criminaljusticearea.exception.CriminalJusticeAreaError;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;
import uk.gov.hmcts.appregister.testutils.client.RoleEnum;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.AuditAssertUtil;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

public class CriminalJusticeAreaControllerTest extends AbstractSecurityControllerTest {
    private static final String WEB_CONTEXT = "criminal-justice-areas";

    // expectations based on the flyway test data
    private static final String EXPECTED_CODE = "CJ";
    private static final String EXPECTED_DESCRIPTION = "CJA_DESCRIPTION";
    private static final String EXPECTED_GET_CRIMINAL_JUSTICE_AREA_AUDIT_ACTION =
            AuditEventEnum.GET_CRIMINAL_JUSTICE_AUDIT_EVENT.getEventName();

    @Test
    public void givenValidRequest_whenGetCriminalJusticeAreaWithValidCode_thenReturn200()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + EXPECTED_CODE),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(200);

        CriminalJusticeAreaGetDto responseContent =
                responseSpec.as(CriminalJusticeAreaGetDto.class);

        // assert the core payload
        CriminalJusticeAreaGetDto criminalJusticeAreaDto =
                generateDefaultCriminalJusticeDtoAssertionPayload(
                        EXPECTED_CODE, EXPECTED_DESCRIPTION);

        // our generated dtos contain an implemented logical equivalence
        Assertions.assertEquals(criminalJusticeAreaDto, responseContent);

        // assert the audit log message
        AuditAssertUtil.assertStart(
                EXPECTED_GET_CRIMINAL_JUSTICE_AREA_AUDIT_ACTION, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertCompleted(
                EXPECTED_GET_CRIMINAL_JUSTICE_AREA_AUDIT_ACTION, logCaptor.getInfoLogs().get(1));
    }

    @Test
    public void givenValidRequest_whenGetCriminalJusticeAreaWithInvalidCode_thenReturn400()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        String invalidCode = "invalidCode";

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + invalidCode),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(404);
        ProblemAssertUtil.assertEquals(
                CriminalJusticeAreaError.CODE_NOT_FOUND.getCode(), responseSpec);

        // assert the audit log message
        AuditAssertUtil.assertStart(
                EXPECTED_GET_CRIMINAL_JUSTICE_AREA_AUDIT_ACTION, logCaptor.getInfoLogs().get(0));
        AuditAssertUtil.assertFailCompleted(
                EXPECTED_GET_CRIMINAL_JUSTICE_AREA_AUDIT_ACTION, logCaptor.getInfoLogs().get(1));
    }

    private CriminalJusticeAreaGetDto generateDefaultCriminalJusticeDtoAssertionPayload(
            String code, String description) {
        return new CriminalJusticeAreaGetDto(code, description);
    }

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrlWithDate(
                                        WEB_CONTEXT + "/" + EXPECTED_CODE, OffsetDateTime.now()))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }
}
