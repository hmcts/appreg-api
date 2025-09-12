package uk.gov.hmcts.appregister.apllicationcode.controller;

import static org.mockito.Mockito.when;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    @MockitoBean private Clock clock; // replaces Clock bean in Spring context

    @BeforeEach
    public void before() {
        // a date that is without range for the main but out of range for the offsite fee
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    @Test
    public void
            givenValidRequest_whenGetApplicationCodesWithWithMultipleFeesForMainAndOffsite_thenReturn200()
                    throws Exception {
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

        // assert
        ApplicationCodeDto applicationCodeDto =
                generateDefaultApplicationCodeDtoAssertionPayload(
                        Optional.of("JP perform function away from court"),
                        Optional.of(200.0),
                        Optional.of("Offsite: JP perform function away from court"),
                        Optional.of(40.0));

        assertApplicationCode(responseContent[1], applicationCodeDto);

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
    public void givenValidRequest_whenGetApplicationCodesWithoutOffsiteFee_thenReturn200()
            throws Exception {
        // a date that is within range for the main but out of range for the offsite fee
        when(clock.instant()).thenReturn(Instant.parse("2014-07-25T10:15:30Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT), tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);

        PagingUtil.assertPageDetails(responseSpec, defaultPageSize, 0, 5, 41);
        ApplicationCodeDto[] responseContent =
                PagingUtil.getResponseContentFromPagingResponse(
                        responseSpec, ApplicationCodeDto[].class);
        Assertions.assertEquals(defaultPageSize, responseContent.length);

        // assert
        ApplicationCodeDto applicationCodeDto =
                generateDefaultApplicationCodeDtoAssertionPayload(
                        Optional.of("JP perform function away from court"),
                        Optional.of(50.0),
                        Optional.empty(),
                        Optional.empty());

        assertApplicationCode(responseContent[1], applicationCodeDto);

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
    public void givenValidRequest_whenGetApplicationCodesWithOffsiteFeeButNoMain_thenReturn200()
            throws Exception {
        // a date that is within range for the offset but out of range for the main fee
        when(clock.instant()).thenReturn(Instant.parse("2020-07-25T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT), tokenGenerator.fetchTokenForRole());
        responseSpec.then().statusCode(200);

        // assert
        PagingUtil.assertPageDetails(responseSpec, defaultPageSize, 0, 5, 41);
        ApplicationCodeDto[] responseContent =
                PagingUtil.getResponseContentFromPagingResponse(
                        responseSpec, ApplicationCodeDto[].class);
        Assertions.assertEquals(defaultPageSize, responseContent.length);

        ApplicationCodeDto applicationCodeDto =
                generateDefaultApplicationCodeDtoAssertionPayload(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("Offsite: JP perform function away from court"),
                        Optional.of(70.0));

        assertApplicationCode(responseContent[1], applicationCodeDto);

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
            givenValidRequest_whenGetApplicationCodesForCodeWithMultipleFeesForMainAndOffsite_thenReturn200()
                    throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        String id = "AD99002";
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + id), tokenGenerator.fetchTokenForRole());

        // make the assertions
        responseSpec.then().statusCode(200);

        ApplicationCodeDto responseContent = responseSpec.as(ApplicationCodeDto.class);

        ApplicationCodeDto applicationCodeDto =
                generateDefaultApplicationCodeDtoAssertionPayload(
                        Optional.of("JP perform function away from court"),
                        Optional.of(200.0),
                        Optional.of("Offsite: JP perform function away from court"),
                        Optional.of(40.0));

        assertApplicationCode(responseContent, applicationCodeDto);

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
    public void givenValidRequest_whenGetApplicationCodesForCodeWithoutOffsite_thenReturn200()
            throws Exception {
        // a date that is within range for the main but out of range for the offsite fee
        when(clock.instant()).thenReturn(Instant.parse("2014-07-25T10:15:30Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        String id = "AD99002";
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + id), tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);

        ApplicationCodeDto responseContent = responseSpec.as(ApplicationCodeDto.class);

        // assert
        ApplicationCodeDto applicationCodeDto =
                generateDefaultApplicationCodeDtoAssertionPayload(
                        Optional.of("JP perform function away from court"),
                        Optional.of(50.0),
                        Optional.empty(),
                        Optional.empty());

        assertApplicationCode(responseContent, applicationCodeDto);

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
            givenValidRequest_whenGetApplicationCodesForCodeWithOffsiteFeeButNoMain_thenReturn200()
                    throws Exception {
        // a date that is within range for the offset but out of range for the main fee
        when(clock.instant()).thenReturn(Instant.parse("2020-07-25T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        String id = "AD99002";
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + id), tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);

        // assert
        ApplicationCodeDto responseContent = responseSpec.as(ApplicationCodeDto.class);

        ApplicationCodeDto applicationCodeDto =
                generateDefaultApplicationCodeDtoAssertionPayload(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("Offsite: JP perform function away from court"),
                        Optional.of(70.0));

        assertApplicationCode(responseContent, applicationCodeDto);

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
        Assertions.assertEquals(200.0, firstEntry.mainFeeAmount());
        Assertions.assertEquals(
                "Offsite: JP perform function away from court", firstEntry.offsetFeeDescription());
        Assertions.assertEquals(40.0, firstEntry.offsetFeeAmount());
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
        Optional<ApplicationCode> expectedRecord = applicationCodeRepository.findByCode(id);
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

    private ApplicationCodeDto generateDefaultApplicationCodeDtoAssertionPayload(
            Optional<String> mainFeeDesc,
            Optional<Double> mainFeeAmt,
            Optional<String> offsiteFeeDesc,
            Optional<Double> offsiteFeeAmt) {
        return new ApplicationCodeDto(
                1L,
                "AD99002",
                "Copy documents (electronic)",
                "Request for copy documents on computer" + " disc or in electronic form",
                "",
                true,
                false,
                "address1@cgi.com",
                "address2@cgi.com",
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                false,
                "CO1.1",
                mainFeeDesc.orElse(null),
                mainFeeAmt.orElse(null),
                offsiteFeeDesc.orElse(null),
                offsiteFeeAmt.orElse(null),
                OffsetDateTime.now(),
                "Jane Doe",
                "Code wording");
    }

    private void assertApplicationCode(ApplicationCodeDto actual, ApplicationCodeDto expected) {
        Assertions.assertEquals(expected.applicationCode(), actual.applicationCode());
        Assertions.assertEquals(expected.title(), actual.title());
        Assertions.assertEquals(expected.wording(), actual.wording());
        Assertions.assertEquals(expected.feeDue(), actual.feeDue());
        Assertions.assertEquals(expected.requiresRespondent(), actual.requiresRespondent());

        if (expected.startDate() == null) {
            Assertions.assertNull(actual.startDate());
        } else {
            Assertions.assertNotNull(expected.startDate());
        }

        if (expected.endDate() == null) {
            Assertions.assertNull(actual.endDate());
        } else {
            Assertions.assertNotNull(expected.endDate());
        }

        Assertions.assertEquals(expected.bulkRespondentAllowed(), actual.bulkRespondentAllowed());
        Assertions.assertEquals(expected.feeReference(), actual.feeReference());
        Assertions.assertEquals(expected.mainFeeAmount(), actual.mainFeeAmount());
        Assertions.assertEquals(expected.offsetFeeDescription(), actual.offsetFeeDescription());
        Assertions.assertEquals(expected.offsetFeeAmount(), actual.offsetFeeAmount());

        if (expected.lodgementDate() == null) {
            Assertions.assertNull(actual.lodgementDate());
        } else {
            Assertions.assertNotNull(expected.lodgementDate());
        }
        Assertions.assertEquals(expected.applicantName(), actual.applicantName());
        Assertions.assertEquals(expected.destinationEmail1(), actual.destinationEmail1());
        Assertions.assertEquals(expected.destinationEmail2(), actual.destinationEmail2());
    }

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
                RestEndpointDescription.builder()
                        .url(getLocalUrl("application-codes"))
                        .method(HttpMethod.GET)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrl("application-codes/2"))
                        .method(HttpMethod.GET)
                        .build());
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
