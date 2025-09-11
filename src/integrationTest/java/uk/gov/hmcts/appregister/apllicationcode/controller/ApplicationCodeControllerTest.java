package uk.gov.hmcts.appregister.apllicationcode.controller;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.net.MalformedURLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
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
import uk.gov.hmcts.appregister.testutils.DateUtil;
import uk.gov.hmcts.appregister.testutils.client.RoleEnum;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.stubs.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.PagingUtil;

public class ApplicationCodeControllerTest extends AbstractSecurityControllerTest {
    private static final String WEB_CONTEXT = "application-codes";

    @Autowired private ApplicationCodeRepository applicationCodeRepository;

    @Autowired private DataAuditRepository dataAuditRepository;

    @Value("${spring.sql.init.schema-locations}")
    private String sqlInitSchemaLocations;

    @Value("${spring.data.web.pageable.default-page-size}")
    private Integer defaultPageSize;

    @Value("${spring.data.web.pageable.max-page-size}")
    private Integer maxPageSize;

    @Test
    public void givenValidRequest_whenGetApplicationCodes_thenReturn200() throws Exception {

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionaity
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT), tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(200);

        PagingUtil.assertPageDetails(responseSpec, defaultPageSize, 0, 5, 41);
        ApplicationCodeDto[] responseContent =
                PagingUtil.getResponseContentFromPagingResponse(
                        responseSpec, ApplicationCodeDto[].class);
        Assertions.assertEquals(defaultPageSize, responseContent.length);

        // assert the second auth code record
        ApplicationCodeDto firstEntry = responseContent[2];

        Assertions.assertEquals("AD99003", firstEntry.applicationCode());
        Assertions.assertEquals("Extract from the Court Register", firstEntry.title());
        Assertions.assertEquals("Certified extract from the court register", firstEntry.wording());
        Assertions.assertTrue(firstEntry.feeDue());
        Assertions.assertFalse(firstEntry.requiresRespondent());
        Assertions.assertEquals(OffsetDateTime.parse("2016-01-01T00:00Z"), firstEntry.startDate());
        Assertions.assertFalse(firstEntry.bulkRespondentAllowed());
        Assertions.assertEquals("CO1.1", firstEntry.feeReference());
        Assertions.assertEquals(
                "JP perform function away from court", firstEntry.mainFeeDescription());
        Assertions.assertEquals(50.0, firstEntry.mainFeeAmount());
        Assertions.assertEquals(
                "JP perform function away from court", firstEntry.offsetFeeDescription());
        Assertions.assertEquals(30.0, firstEntry.offsetFeeAmount());
        Assertions.assertTrue(
                DateUtil.equalsIgnoreMillis(
                        OffsetDateTime.parse("2022-01-30T10:00Z"), firstEntry.lodgementDate()));
        Assertions.assertEquals("Jane Doe", firstEntry.applicantName());

        // assert the first record
        ApplicationCodeDto secondEntry = responseContent[3];
        Assertions.assertEquals("AD99004", secondEntry.applicationCode());
        Assertions.assertEquals("Certificate of Satisfaction", secondEntry.title());
        Assertions.assertEquals(
                "Request for a certificate of satisfaction of debt registered "
                        + "in the register of judgements, orders and fines",
                secondEntry.wording());
        Assertions.assertFalse(secondEntry.feeDue());
        Assertions.assertFalse(secondEntry.requiresRespondent());
        Assertions.assertEquals(OffsetDateTime.parse("2016-01-01T00:00Z"), secondEntry.startDate());
        Assertions.assertFalse(secondEntry.bulkRespondentAllowed());
        Assertions.assertNull(secondEntry.feeReference());
        Assertions.assertNull(secondEntry.mainFeeDescription());
        Assertions.assertNull(secondEntry.mainFeeAmount());
        Assertions.assertNull(secondEntry.offsetFeeDescription());
        Assertions.assertNull(secondEntry.offsetFeeAmount());
        Assertions.assertTrue(
                DateUtil.equalsIgnoreMillis(
                        OffsetDateTime.parse("2021-01-01T00:00Z"), secondEntry.lodgementDate()));
        Assertions.assertEquals("John Smith", secondEntry.applicantName());

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

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        String id = "AD99002";
        Optional<ApplicationCode> expectedRecord =
                applicationCodeRepository.findByApplicationCode(id);
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + id), tokenGenerator.fetchTokenForRole());

        // make the assertions
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
    public void
            givenValidRequest_whenGetApplicationCodesWithPagingCriteriaWithoutExplicitSort_thenReturn200()
                    throws Exception {

        // create the token to send
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute the functionality
        int pageSize = 2;
        int pageNumber = 1;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        Optional.empty(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());
        responseSpec.then().statusCode(200);

        // make the assertions
        PagingUtil.assertPageDetails(responseSpec, pageSize, pageNumber, 21, 41);

        ApplicationCodeDto[] responseContent =
                responseSpec.jsonPath().getObject("content", ApplicationCodeDto[].class);
        Assertions.assertEquals(pageSize, responseContent.length);

        // assert the first auth code record
        ApplicationCodeDto firstEntry = responseContent[0];

        Assertions.assertEquals("AD99003", firstEntry.applicationCode());
        Assertions.assertEquals("Extract from the Court Register", firstEntry.title());
        Assertions.assertEquals("Certified extract from the court register", firstEntry.wording());
        Assertions.assertTrue(firstEntry.feeDue());
        Assertions.assertFalse(firstEntry.requiresRespondent());
        Assertions.assertEquals(OffsetDateTime.parse("2016-01-01T00:00Z"), firstEntry.startDate());
        Assertions.assertFalse(firstEntry.bulkRespondentAllowed());
        Assertions.assertEquals("CO1.1", firstEntry.feeReference());
        Assertions.assertEquals(
                "JP perform function away from court", firstEntry.mainFeeDescription());
        Assertions.assertEquals(50.0, firstEntry.mainFeeAmount());
        Assertions.assertEquals(
                "JP perform function away from court", firstEntry.offsetFeeDescription());
        Assertions.assertEquals(30.0, firstEntry.offsetFeeAmount());
        Assertions.assertTrue(
                DateUtil.equalsIgnoreMillis(
                        OffsetDateTime.parse("2022-01-30T10:00Z"), firstEntry.lodgementDate()));
        Assertions.assertEquals("Jane Doe", firstEntry.applicantName());

        // assert the second record
        ApplicationCodeDto secondEntry = responseContent[1];
        Assertions.assertEquals("AD99004", secondEntry.applicationCode());
        Assertions.assertEquals("Certificate of Satisfaction", secondEntry.title());
        Assertions.assertEquals(
                "Request for a certificate of satisfaction of debt registered in the register "
                        + "of judgements, orders and fines",
                secondEntry.wording());
        Assertions.assertFalse(secondEntry.feeDue());
        Assertions.assertFalse(secondEntry.requiresRespondent());
        Assertions.assertEquals(OffsetDateTime.parse("2016-01-01T00:00Z"), secondEntry.startDate());
        Assertions.assertFalse(secondEntry.bulkRespondentAllowed());
        Assertions.assertNull(secondEntry.feeReference());
        Assertions.assertNull(secondEntry.mainFeeDescription());
        Assertions.assertNull(secondEntry.mainFeeAmount());
        Assertions.assertNull(secondEntry.offsetFeeDescription());
        Assertions.assertNull(secondEntry.offsetFeeAmount());
        Assertions.assertTrue(
                DateUtil.equalsIgnoreMillis(
                        OffsetDateTime.parse("2021-01-01T00:00Z"), secondEntry.lodgementDate()));
        Assertions.assertEquals("John Smith", secondEntry.applicantName());

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
    public void
            givenValidRequest_whenGetApplicationCodesWithPagingCriteriaWithExplicitSort_thenReturn200()
                    throws Exception {

        // create the token to send
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute the functionality
        int pageSize = 2;
        int pageNumber = 1;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        Optional.of("title"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());
        responseSpec.then().statusCode(200);

        // assert the response
        PagingUtil.assertPageDetails(responseSpec, pageSize, pageNumber, 21, 41);

        ApplicationCodeDto[] responseContent =
                responseSpec.jsonPath().getObject("content", ApplicationCodeDto[].class);
        Assertions.assertEquals(pageSize, responseContent.length);

        // assert records are sorted based on the title of the auth codes
        ApplicationCodeDto firstEntry = responseContent[0];
        ApplicationCodeDto secondEntry = responseContent[1];

        Assertions.assertEquals("AP99001", firstEntry.applicationCode());
        Assertions.assertEquals("SW99009", secondEntry.applicationCode());

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
    public void givenValidRequest_whenGetApplicationCodesWithPagingNoResult_thenReturn200()
            throws Exception {

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        int pageSize = 2;
        int pageNumber = 1;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        Optional.of("title"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(
                                Optional.of("does not exist"),
                                Optional.of("does not exist"),
                                Optional.of(OffsetDateTime.now().minusYears(20).toString())));

        // assert the response is successful with no content
        responseSpec.then().statusCode(200);
        PagingUtil.assertPageDetails(responseSpec, pageSize, pageNumber, 0, 0);
    }

    @Test
    public void
            givenValidRequest_whenGetApplicationCodesWithPagingApplicationCodeFilter_thenReturn200()
                    throws Exception {

        // create a token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        int pageSize = 1;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        Optional.of("title"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(
                                Optional.of("CT99002"), Optional.empty(), Optional.empty()));

        // assert the response
        responseSpec.then().statusCode(200);
        PagingUtil.assertPageDetails(responseSpec, pageSize, pageNumber, 1, 1);
        ApplicationCodeDto[] responseContent =
                responseSpec.jsonPath().getObject("content", ApplicationCodeDto[].class);
        ApplicationCodeDto firstEntry = responseContent[0];
        Assertions.assertEquals("CT99002", firstEntry.applicationCode());
    }

    @Test
    public void givenValidRequest_whenGetApplicationCodesWithPagingTitleFilter_thenReturn200()
            throws Exception {

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute functionality
        int pageSize = 1;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        Optional.of("title"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(
                                Optional.empty(),
                                Optional.of("Certificate of Satisfaction"),
                                Optional.empty()));

        // assert the response
        responseSpec.then().statusCode(200);
        PagingUtil.assertPageDetails(responseSpec, pageSize, pageNumber, 1, 1);
        ApplicationCodeDto[] responseContent =
                PagingUtil.getResponseContentFromPagingResponse(
                        responseSpec, ApplicationCodeDto[].class);
        ApplicationCodeDto firstEntry = responseContent[0];
        Assertions.assertEquals("AD99004", firstEntry.applicationCode());
    }

    @Test
    public void
            givenValidRequest_whenGetApplicationCodesWithPagingLodgementDateFilter_thenReturn200()
                    throws Exception {

        // create token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute the functionality
        int pageSize = 1;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        Optional.of("title"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(OffsetDateTime.parse("2024-04-01T00:00Z").toString())));

        // assert
        responseSpec.then().statusCode(200);
        PagingUtil.assertPageDetails(responseSpec, pageSize, pageNumber, 1, 1);
        ApplicationCodeDto[] responseContent =
                PagingUtil.getResponseContentFromPagingResponse(
                        responseSpec, ApplicationCodeDto[].class);
        ApplicationCodeDto firstEntry = responseContent[0];
        Assertions.assertEquals("AP99002", firstEntry.applicationCode());
    }

    @Test
    public void givenValidRequest_whenGetApplicationCodesWithPagingAllFilter_thenReturn200()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute the functionality
        int pageSize = 1;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        Optional.of("title"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(
                                Optional.of("AP99004"),
                                Optional.of(
                                        "Request for Certificate of Refusal to State a Case (Civil)"),
                                Optional.of(OffsetDateTime.parse("2006-02-01T00:00Z").toString())));

        // assert the response
        responseSpec.then().statusCode(200);
        PagingUtil.assertPageDetails(responseSpec, pageSize, pageNumber, 1, 1);
        ApplicationCodeDto[] responseContent =
                PagingUtil.getResponseContentFromPagingResponse(
                        responseSpec, ApplicationCodeDto[].class);
        ApplicationCodeDto firstEntry = responseContent[0];
        Assertions.assertEquals("AP99004", firstEntry.applicationCode());
    }

    @Test
    public void
            givenValidRequest_whenGetApplicationCodesWithPageNumberBeyondResultBoundary_thenReturn200()
                    throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute the functionality
        int pageSize = 1;
        int pageNumber = 200;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        Optional.of("title"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(
                                Optional.of("AP99004"),
                                Optional.of(
                                        "Request for Certificate of Refusal to State a Case (Civil)"),
                                Optional.of(OffsetDateTime.parse("2006-02-01T00:00Z").toString())));

        // assert the response
        responseSpec.then().statusCode(200);
        PagingUtil.assertPageDetails(responseSpec, pageSize, pageNumber, 1, 1);
        ApplicationCodeDto[] responseContent =
                PagingUtil.getResponseContentFromPagingResponse(
                        responseSpec, ApplicationCodeDto[].class);
        Assertions.assertEquals(0, responseContent.length);
    }

    @Test
    public void givenValidRequest_whenGetApplicationCodesWithPagingInvalidSortQuery_thenReturn400()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute the functionality
        int pageSize = 1;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        Optional.of("incorrect"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(
                                Optional.of("AP99004"),
                                Optional.of(
                                        "Request for Certificate of Refusal to State a Case (Civil)"),
                                Optional.of(OffsetDateTime.parse("2006-02-01T00:00Z").toString())));
        // assert the response
        responseSpec.then().statusCode(400);
    }

    @Test
    public void givenValidRequest_whenGetApplicationCodesWithPagingInvalidPageNumber_thenReturn400()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute the functionality
        int pageSize = -1;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        Optional.empty(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(
                                Optional.of("AP99004"),
                                Optional.of(
                                        "Request for Certificate of Refusal to State a Case (Civil)"),
                                Optional.of(OffsetDateTime.parse("2006-02-01T00:00Z").toString())));
        // assert the response
        responseSpec.then().statusCode(200);

        // The page size defaults if it is incorrect in the request
        PagingUtil.assertPageDetails(responseSpec, defaultPageSize, pageNumber, 1, 1);
    }

    @Test
    public void
            givenValidRequest_whenGetApplicationCodesWithPagingInvalidPageSizeBeyondDefault_thenReturn200()
                    throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute the functionality
        int pageSize = maxPageSize + 1;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        Optional.empty(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(
                                Optional.of("AP99004"),
                                Optional.of(
                                        "Request for Certificate of Refusal to State a Case (Civil)"),
                                Optional.of(OffsetDateTime.parse("2006-02-01T00:00Z").toString())));

        // assert the response
        responseSpec.then().statusCode(200);

        // The page size response defaults to the max size if we try and increase it beyond
        PagingUtil.assertPageDetails(responseSpec, pageSize - 1, pageNumber, 1, 1);
    }

    @Test
    public void givenValidRequest_whenGetApplicationCodesForCodeNotValid_thenReturn404()
            throws Exception {

        // execute the functionality
        String id = "doesntexist";
        Optional<ApplicationCode> expectedRecord =
                applicationCodeRepository.findByApplicationCode(id);
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + id),
                        getATokenWithValidCredentials()
                                .roles(List.of(RoleEnum.ADMIN))
                                .build()
                                .fetchTokenForRole());

        // assert the response
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

    /**
     * A request specification that knows what query filters can be applied to get application
     * codes.
     */
    @RequiredArgsConstructor
    static class ApplicationCodeRequestFilter
            implements Function<RequestSpecification, RequestSpecification> {
        private final Optional<String> appCode;
        private final Optional<String> appTitle;
        private final Optional<String> lodgementDate;

        @Override
        public RequestSpecification apply(RequestSpecification rs) {
            if (appCode.isPresent()) {
                rs = rs.queryParam("appCode", appCode.get());
            }

            if (appTitle.isPresent()) {
                rs = rs.queryParam("appTitle", appTitle.get());
            }

            if (lodgementDate.isPresent()) {
                rs = rs.queryParam("lodgementDate", lodgementDate.get());
            }

            return rs;
        }
    }
}
