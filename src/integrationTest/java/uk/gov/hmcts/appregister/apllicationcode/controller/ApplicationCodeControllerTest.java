package uk.gov.hmcts.appregister.apllicationcode.controller;

import io.restassured.response.Response;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.applicationcode.dto.ApplicationCodeDto;
import uk.gov.hmcts.appregister.applicationcode.exception.AppCodeError;
import uk.gov.hmcts.appregister.audit.AuditEnum;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.testutils.client.RoleEnum;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.stubs.TokenGenerator;

public class ApplicationCodeControllerTest extends AbstractSecurityControllerTest {
    private static final String WEB_CONTEXT = "application-codes";

    @Autowired private ApplicationCodeRepository applicationCodeRepository;

    @Autowired private DataAuditRepository dataAuditRepository;

    @Value("${spring.sql.init.schema-locations}")
    private String sqlInitSchemaLocations;

    @Test
    public void givenValidRequest_whenGetApplicationCodes_thenReturn200() throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN.getRole())).build();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT), tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);
        ApplicationCodeDto[] codeDto = responseSpec.as(ApplicationCodeDto[].class);
        Assertions.assertEquals(41, codeDto.length);

        // assert the second auth code record
        Assertions.assertEquals("AD99002", codeDto[1].applicationCode());
        Assertions.assertEquals("Copy documents (electronic)", codeDto[1].title());
        Assertions.assertEquals(
                "Request for copy documents on computer" + " disc or in electronic form",
                codeDto[1].wording());
        Assertions.assertTrue(codeDto[1].feeDue());
        Assertions.assertFalse(codeDto[1].requiresRespondent());
        Assertions.assertNotNull(codeDto[1].startDate());
        Assertions.assertFalse(codeDto[1].bulkRespondentAllowed());
        Assertions.assertEquals("CO1.1", codeDto[1].feeReference());
        Assertions.assertEquals(
                "JP perform function away from court", codeDto[1].mainFeeDescription());
        Assertions.assertEquals(50.0, codeDto[1].mainFeeAmount());
        Assertions.assertEquals(
                "JP perform function away from court", codeDto[1].offsetFeeDescription());
        Assertions.assertEquals(30.0, codeDto[1].offsetFeeAmount());
        Assertions.assertNotNull(codeDto[1].lodgementDate());
        Assertions.assertEquals("Jane Doe", codeDto[1].applicantName());
        Assertions.assertEquals(
                "Request for copy documents on computer" + " disc or in electronic form",
                codeDto[1].wording());

        // assert the data audit record has been created
        DataAudit dataAudit = dataAuditRepository.findAll().get(0);
        Assertions.assertEquals(1, dataAuditRepository.findAll().size());
        Assertions.assertEquals(
                AuditEnum.GET_APPLICATION_CODES_AUDIT_EVENT.getEventName(),
                dataAudit.getEventName());
        Assertions.assertEquals(
                AuditEnum.GET_APPLICATION_CODES_AUDIT_EVENT.getColumnName(),
                dataAudit.getColumnName());
        Assertions.assertEquals(tokenGenerator.getEmail(), dataAudit.getCreatedUser());
        Assertions.assertEquals(tokenGenerator.getEmail(), dataAudit.getUserName());
        Assertions.assertTrue(dataAudit.getLink().endsWith(WEB_CONTEXT));
        Assertions.assertEquals(sqlInitSchemaLocations, dataAudit.getSchemaName());
    }

    @Test
    public void givenValidRequest_whenGetApplicationCodesForCode_thenReturn200() throws Exception {

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN.getRole())).build();

        String id = "AD99002";
        Optional<ApplicationCode> expectedRecord = applicationCodeRepository.findByCode(id);
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + id), tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);
        ApplicationCodeDto codeDto = responseSpec.as(ApplicationCodeDto.class);

        // assert the second auth code record
        Assertions.assertEquals(id, codeDto.applicationCode());
        Assertions.assertEquals("Copy documents (electronic)", codeDto.title());
        Assertions.assertEquals(
                "Request for copy documents on computer disc or in electronic form",
                codeDto.wording());
        Assertions.assertTrue(codeDto.feeDue());
        Assertions.assertFalse(codeDto.requiresRespondent());
        Assertions.assertNotNull(codeDto.startDate());
        Assertions.assertFalse(codeDto.bulkRespondentAllowed());
        Assertions.assertEquals("CO1.1", codeDto.feeReference());
        Assertions.assertEquals(
                "JP perform function away from court", codeDto.mainFeeDescription());
        Assertions.assertEquals(50.0, codeDto.mainFeeAmount());
        Assertions.assertEquals(
                "JP perform function away from court", codeDto.offsetFeeDescription());
        Assertions.assertEquals(30.0, codeDto.offsetFeeAmount());
        Assertions.assertNotNull(codeDto.lodgementDate());
        Assertions.assertEquals("Jane Doe", codeDto.applicantName());
        Assertions.assertEquals(
                "Request for copy documents on computer disc or in electronic form",
                codeDto.wording());

        // assert the data audit record has been created
        DataAudit dataAudit = dataAuditRepository.findAll().get(0);
        Assertions.assertEquals(1, dataAuditRepository.findAll().size());
        Assertions.assertEquals(
                AuditEnum.GET_APPLICATION_CODE_AUDIT_EVENT.getEventName(),
                dataAudit.getEventName());
        Assertions.assertEquals(
                AuditEnum.GET_APPLICATION_CODE_AUDIT_EVENT.getColumnName(),
                dataAudit.getColumnName());
        Assertions.assertEquals(tokenGenerator.getEmail(), dataAudit.getCreatedUser());
        Assertions.assertEquals(tokenGenerator.getEmail(), dataAudit.getUserName());
        Assertions.assertTrue(dataAudit.getLink().endsWith(WEB_CONTEXT + "/" + id));
        Assertions.assertTrue(dataAudit.getLink().endsWith(WEB_CONTEXT + "/" + id));
        Assertions.assertEquals(sqlInitSchemaLocations, dataAudit.getSchemaName());
    }

    @Test
    public void givenValidRequest_whenGetApplicationCodesForCodeNotValid_thenReturn404()
            throws Exception {
        String id = "doesntexist";
        Optional<ApplicationCode> expectedRecord = applicationCodeRepository.findByCode(id);
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + id),
                        getATokenWithValidCredentials()
                                .roles(List.of(RoleEnum.ADMIN.getRole()))
                                .build()
                                .fetchTokenForRole());

        responseSpec.then().statusCode(404);
        ProblemDetail codeDto = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(
                AppCodeError.CODE_NOT_FOUND.getCode().getType().get(), codeDto.getType());
        Assertions.assertEquals(
                AppCodeError.CODE_NOT_FOUND.getCode().getMessage(), codeDto.getDetail());
        Assertions.assertEquals(
                AppCodeError.CODE_NOT_FOUND.getCode().getMessage(), codeDto.getTitle());
        Assertions.assertEquals("/" + WEB_CONTEXT + "/" + id, codeDto.getInstance().toString());
    }

    @Override
    protected RestEndpointDescription[] getRestDescriptions() throws MalformedURLException {
        return new RestEndpointDescription[] {
            RestEndpointDescription.builder()
                    .url(getLocalUrl("application-codes"))
                    .method(HttpMethod.GET)
                    .build(),
            RestEndpointDescription.builder()
                    .url(getLocalUrl("application-codes/2"))
                    .method(HttpMethod.GET)
                    .build(),
        };
    }
}
