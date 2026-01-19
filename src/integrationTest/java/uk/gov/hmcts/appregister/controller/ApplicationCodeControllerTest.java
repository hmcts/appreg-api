package uk.gov.hmcts.appregister.controller;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.appregister.applicationcode.exception.ApplicationCodeError;
import uk.gov.hmcts.appregister.audit.event.OperationStatus;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetSummaryDtoFeeAmount;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodePage;
import uk.gov.hmcts.appregister.generated.model.TemplateConstraint;
import uk.gov.hmcts.appregister.generated.model.TemplateDetail;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.PagingAssertionUtil;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

public class ApplicationCodeControllerTest extends AbstractSecurityControllerTest {
    private static final String WEB_CONTEXT = "application-codes";

    @Value("${spring.sql.init.schema-locations}")
    private String sqlInitSchemaLocations;

    @Value("${spring.data.web.pageable.default-page-size}")
    private Integer defaultPageSize;

    @Value("${spring.data.web.pageable.max-page-size}")
    private Integer maxPageSize;

    @MockitoBean private Clock clock; // replaces Clock bean in Spring context

    // The total app codes inserted by flyway scripts. See V6__InitialTestData.sql
    private static final int TOTAL_APP_CODES_COUNT = 44;

    private static final String FEE_DESCRIPTION = "JP perform function away from court";
    private static final String OFFSITE_FEE_DESCRIPTION =
            "Offsite: JP perform function away from court";
    private static final String APPCODE_CODE = "AD99002";
    private static final String DUPLICATE_APPCODE_CODE = "MS99006";

    private static final String DATE_TO_FIND_CODE = "2016-01-01T00:00Z";
    private static final String START_AUDIT_LOG = "Start audit";
    private static final String COMPLETION_AUDIT_LOG = "Completion audit";

    private static final String GET_APPCODE_AUDIT_ACTION = "Get Application Code";
    private static final String GET_APPCODES_AUDIT_ACTION = "Get Application Codes";
    private static final String CURRENT_TIME = "2020-07-25T00:00:00Z";

    @BeforeEach
    public void before() {
        // a date that is without range for the main but out of range for the offsite fee
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.withZone(org.mockito.ArgumentMatchers.any(ZoneId.class))).thenReturn(clock);
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

        ApplicationCodePage page = responseSpec.as(ApplicationCodePage.class);
        PagingAssertionUtil.assertPageDetails(page, defaultPageSize, 0, 5, TOTAL_APP_CODES_COUNT);
        assertEquals(defaultPageSize, page.getContent().size());

        // assert
        ApplicationCodeGetSummaryDto applicationCodeDto =
                generateDefaultApplicationCodeGetSummaryDtoAssertionPayload(
                        Optional.of(FEE_DESCRIPTION), Optional.of(200.0), Optional.of(40.0));

        assertApplicationCode(page.getContent().get(1), applicationCodeDto);

        // assert the audit log message
        assertTrue(
                Pattern.matches(
                        getExpectedLog(
                                START_AUDIT_LOG,
                                GET_APPCODES_AUDIT_ACTION,
                                OperationStatus.STARTED),
                        logCaptor.getInfoLogs().get(0)));
    }

    @Test
    public void
            givenValidRequest_whenGetApplicationCodesWithUserRoleAndMultipleFeesForMainAndOffsite_thenReturn200()
                    throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.USER)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT), tokenGenerator.fetchTokenForRole());

        ApplicationCodePage page = responseSpec.as(ApplicationCodePage.class);
        PagingAssertionUtil.assertPageDetails(page, defaultPageSize, 0, 5, TOTAL_APP_CODES_COUNT);
        assertEquals(defaultPageSize, page.getContent().size());

        // assert
        ApplicationCodeGetSummaryDto applicationCodeDto =
                generateDefaultApplicationCodeGetSummaryDtoAssertionPayload(
                        Optional.of(FEE_DESCRIPTION), Optional.of(200.0), Optional.of(40.0));

        assertApplicationCode(page.getContent().get(1), applicationCodeDto);

        // assert the audit log message
        assertTrue(
                Pattern.matches(
                        getExpectedLog(
                                START_AUDIT_LOG,
                                GET_APPCODES_AUDIT_ACTION,
                                OperationStatus.STARTED),
                        logCaptor.getInfoLogs().get(0)));
    }

    @Test
    public void givenValidRequest_whenGetApplicationCodesWithOffsiteFeeButNoMain_thenReturn200()
            throws Exception {
        // a date that is within range for the offset but out of range for the main fee
        when(clock.instant()).thenReturn(Instant.parse(CURRENT_TIME));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT), tokenGenerator.fetchTokenForRole());
        responseSpec.then().statusCode(200);

        // assert
        ApplicationCodePage page = responseSpec.as(ApplicationCodePage.class);
        PagingAssertionUtil.assertPageDetails(page, defaultPageSize, 0, 5, TOTAL_APP_CODES_COUNT);

        ApplicationCodeGetSummaryDto applicationCodeDto =
                generateDefaultApplicationCodeGetSummaryDtoAssertionPayload(
                        Optional.empty(), Optional.empty(), Optional.of(70.0));

        assertApplicationCode(page.getContent().get(1), applicationCodeDto);

        // assert the audit log message
        assertTrue(
                Pattern.matches(
                        getExpectedLog(
                                START_AUDIT_LOG,
                                GET_APPCODES_AUDIT_ACTION,
                                OperationStatus.STARTED),
                        logCaptor.getInfoLogs().get(0)));

        assertTrue(
                Pattern.matches(
                        getExpectedLog(
                                COMPLETION_AUDIT_LOG,
                                GET_APPCODES_AUDIT_ACTION,
                                OperationStatus.COMPLETED),
                        logCaptor.getInfoLogs().get(1)));
    }

    @Test
    public void
            givenValidRequest_whenGetApplicationCodesForCodeWithMultipleFeesForMainAndOffsite_thenReturn200()
                    throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        String id = APPCODE_CODE;
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrlWithDate(
                                WEB_CONTEXT + "/" + id, OffsetDateTime.parse(DATE_TO_FIND_CODE)),
                        tokenGenerator.fetchTokenForRole());

        // make the assertions
        responseSpec.then().statusCode(200);

        ApplicationCodeGetDetailDto responseContent =
                responseSpec.as(ApplicationCodeGetDetailDto.class);

        ApplicationCodeGetDetailDto applicationCodeDto =
                generateDefaultApplicationCodeGetDetailDtoAssertionPayload(
                        Optional.of(FEE_DESCRIPTION), Optional.of(200.0), Optional.of(40.0));

        assertApplicationCode(responseContent, applicationCodeDto);

        // assert the audit log message
        assertTrue(
                Pattern.matches(
                        getExpectedLog(
                                START_AUDIT_LOG, GET_APPCODE_AUDIT_ACTION, OperationStatus.STARTED),
                        logCaptor.getInfoLogs().get(0)));

        assertTrue(
                Pattern.matches(
                        getExpectedLog(
                                COMPLETION_AUDIT_LOG,
                                GET_APPCODE_AUDIT_ACTION,
                                OperationStatus.COMPLETED),
                        logCaptor.getInfoLogs().get(1)));
    }

    @Test
    public void
            givenValidRequest_whenGetApplicationCodesForCodeWithUserRoleAndMultipleFeesForMainAndOffsite_thenReturn200()
                    throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.USER)).build();

        String id = APPCODE_CODE;
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrlWithDate(
                                WEB_CONTEXT + "/" + id, OffsetDateTime.parse(DATE_TO_FIND_CODE)),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);
        ApplicationCodeGetDetailDto response = responseSpec.as(ApplicationCodeGetDetailDto.class);

        // assert the first auth code record
        ApplicationCodeGetDetailDto applicationCodeDto =
                generateDefaultApplicationCodeGetDetailDtoAssertionPayload(
                        Optional.of(FEE_DESCRIPTION), Optional.of(200.0), Optional.of(40.0));

        assertApplicationCode(response, applicationCodeDto);

        // assert the audit log message
        assertTrue(
                Pattern.matches(
                        getExpectedLog(
                                START_AUDIT_LOG, GET_APPCODE_AUDIT_ACTION, OperationStatus.STARTED),
                        logCaptor.getInfoLogs().get(0)));

        assertTrue(
                Pattern.matches(
                        getExpectedLog(
                                COMPLETION_AUDIT_LOG,
                                GET_APPCODE_AUDIT_ACTION,
                                OperationStatus.COMPLETED),
                        logCaptor.getInfoLogs().get(1)));
    }

    @Test
    public void givenValidRequest_whenGetApplicationCodesForCodeWithoutOffsite_thenReturn200()
            throws Exception {
        // a date that is within range for the main but out of range for the offsite fee
        when(clock.instant()).thenReturn(Instant.parse("2014-07-25T10:15:30Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        String id = APPCODE_CODE;
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrlWithDate(
                                WEB_CONTEXT + "/" + id, OffsetDateTime.parse(DATE_TO_FIND_CODE)),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);

        ApplicationCodeGetDetailDto response = responseSpec.as(ApplicationCodeGetDetailDto.class);

        // assert
        ApplicationCodeGetDetailDto applicationCodeDto =
                generateDefaultApplicationCodeGetDetailDtoAssertionPayload(
                        Optional.of(FEE_DESCRIPTION), Optional.of(50.0), Optional.empty());

        assertApplicationCode(response, applicationCodeDto);

        // assert the audit log message
        assertTrue(
                Pattern.matches(
                        getExpectedLog(
                                START_AUDIT_LOG, GET_APPCODE_AUDIT_ACTION, OperationStatus.STARTED),
                        logCaptor.getInfoLogs().get(0)));

        assertTrue(
                Pattern.matches(
                        getExpectedLog(
                                COMPLETION_AUDIT_LOG,
                                GET_APPCODE_AUDIT_ACTION,
                                OperationStatus.COMPLETED),
                        logCaptor.getInfoLogs().get(1)));
    }

    @Test
    public void
            givenValidRequest_whenGetApplicationCodesForCodeWithOffsiteFeeButNoMain_thenReturn200()
                    throws Exception {
        // a date that is within range for the offset but out of range for the main fee
        when(clock.instant()).thenReturn(Instant.parse(CURRENT_TIME));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        String id = APPCODE_CODE;
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrlWithDate(
                                WEB_CONTEXT + "/" + id, OffsetDateTime.parse(DATE_TO_FIND_CODE)),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);

        // assert
        ApplicationCodeGetDetailDto response = responseSpec.as(ApplicationCodeGetDetailDto.class);

        ApplicationCodeGetDetailDto applicationCodeDto =
                generateDefaultApplicationCodeGetDetailDtoAssertionPayload(
                        Optional.empty(), Optional.empty(), Optional.of(70.0));

        assertApplicationCode(response, applicationCodeDto);

        // assert the audit log message
        assertTrue(
                Pattern.matches(
                        getExpectedLog(
                                START_AUDIT_LOG, GET_APPCODE_AUDIT_ACTION, OperationStatus.STARTED),
                        logCaptor.getInfoLogs().get(0)));

        assertTrue(
                Pattern.matches(
                        getExpectedLog(
                                COMPLETION_AUDIT_LOG,
                                GET_APPCODE_AUDIT_ACTION,
                                OperationStatus.COMPLETED),
                        logCaptor.getInfoLogs().get(1)));
    }

    @Test
    public void givenValidRequest_whenGetApplicationCodesDateIsNotCorrectlyFormatted_thenReturn400()
            throws Exception {
        // a date that is within range for the offset but out of range for the main fee
        when(clock.instant()).thenReturn(Instant.parse(CURRENT_TIME));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + APPCODE_CODE),
                        tokenGenerator.fetchTokenForRole(),
                        new SpecificApplicationCodeRequestFilter(
                                Optional.of("invalid-date-format")));

        responseSpec.then().statusCode(400);

        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);
        assertEquals(
                CommonAppError.TYPE_MISMATCH_ERROR.getCode().getHttpCode().value(),
                problemDetail.getStatus());
        assertEquals(
                "Problem with value invalid-date-format for parameter date",
                problemDetail.getDetail());
    }

    @Test
    public void givenValidRequest_whenGetApplicationCodesDateIsNotSet_thenReturn400()
            throws Exception {
        // a date that is within range for the offset but out of range for the main fee
        when(clock.instant()).thenReturn(Instant.parse(CURRENT_TIME));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + APPCODE_CODE),
                        tokenGenerator.fetchTokenForRole(),
                        new SpecificApplicationCodeRequestFilter(Optional.empty()));

        responseSpec.then().statusCode(400);

        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);
        assertEquals(
                CommonAppError.PARAMETER_REQUIRED.getCode().getHttpCode().value(),
                problemDetail.getStatus());
        assertEquals("Required request parameter 'date' is missing", problemDetail.getDetail());
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
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());
        responseSpec.then().statusCode(200);

        ApplicationCodePage response = responseSpec.as(ApplicationCodePage.class);

        // make the assertions
        PagingAssertionUtil.assertPageDetails(
                response, pageSize, pageNumber, 22, TOTAL_APP_CODES_COUNT);

        // assert the first auth code record
        ApplicationCodeGetSummaryDto firstEntry = response.getContent().getFirst();

        assertEquals("AD99003", firstEntry.getApplicationCode());
        assertEquals("Extract from the Court Register", firstEntry.getTitle());
        assertEquals(
                "Certified extract from the court register", firstEntry.getWording().getTemplate());
        assertTrue(firstEntry.getIsFeeDue());
        Assertions.assertFalse(firstEntry.getRequiresRespondent());
        Assertions.assertFalse(firstEntry.getBulkRespondentAllowed());
        assertEquals("CO1.1", firstEntry.getFeeReference().get());
        assertEquals("JP perform function away from court", firstEntry.getFeeDescription().get());
        assertEquals(20000L, firstEntry.getFeeAmount().get().getValue());
        assertEquals(4000L, firstEntry.getOffsiteFeeAmount().get().getValue());

        // assert the second record
        ApplicationCodeGetSummaryDto secondEntry = response.getContent().get(1);
        assertEquals("AD99004", secondEntry.getApplicationCode());
        assertEquals("Certificate of Satisfaction", secondEntry.getTitle());
        assertEquals(
                "Request for a certificate of satisfaction of debt registered in the register "
                        + "of judgements, orders and fines",
                secondEntry.getWording().getTemplate());
        Assertions.assertFalse(secondEntry.getIsFeeDue());
        Assertions.assertFalse(secondEntry.getRequiresRespondent());
        Assertions.assertFalse(secondEntry.getBulkRespondentAllowed());
        Assertions.assertFalse(secondEntry.getFeeReference().isPresent());
        Assertions.assertFalse(secondEntry.getFeeDescription().isPresent());
        Assertions.assertFalse(secondEntry.getFeeAmount().isPresent());
        Assertions.assertFalse(secondEntry.getOffsiteFeeAmount().isPresent());
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
                        List.of("title"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());
        responseSpec.then().statusCode(200);

        ApplicationCodePage response = responseSpec.as(ApplicationCodePage.class);

        // assert the response
        PagingAssertionUtil.assertPageDetails(
                response, pageSize, pageNumber, 22, TOTAL_APP_CODES_COUNT);

        // assert records are sorted based on the title of the auth codes
        ApplicationCodeGetSummaryDto firstEntry = response.getContent().get(0);
        ApplicationCodeGetSummaryDto secondEntry = response.getContent().get(1);

        assertEquals("AP99001", firstEntry.getApplicationCode());
        assertEquals("SW99009", secondEntry.getApplicationCode());
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
                        List.of("title"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(
                                Optional.of("does not exist"), Optional.of("does not exist")),
                        new OpenApiPageMetaData());

        // assert the response is successful with no content
        responseSpec.then().statusCode(200);
        ApplicationCodePage response = responseSpec.as(ApplicationCodePage.class);
        PagingAssertionUtil.assertPageDetails(response, pageSize, pageNumber, 0, 0);
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
                        List.of("title"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(Optional.of("CT99002"), Optional.empty()),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);
        ApplicationCodePage page = responseSpec.as(ApplicationCodePage.class);
        PagingAssertionUtil.assertPageDetails(page, pageSize, pageNumber, 1, 1);
        ApplicationCodeGetSummaryDto firstEntry = page.getContent().get(0);
        assertEquals("CT99002", firstEntry.getApplicationCode());
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
                        List.of("title"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(
                                Optional.empty(), Optional.of("Certificate of Satisfaction")),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);
        ApplicationCodePage page = responseSpec.as(ApplicationCodePage.class);
        PagingAssertionUtil.assertPageDetails(page, pageSize, pageNumber, 1, 1);
        ApplicationCodeGetSummaryDto firstEntry = page.getContent().get(0);
        assertEquals("AD99004", firstEntry.getApplicationCode());
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
                        List.of("title"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(
                                Optional.of("AP99004"),
                                Optional.of(
                                        "Request for Certificate of Refusal to State a Case (Civil)")),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);
        ApplicationCodePage page = responseSpec.as(ApplicationCodePage.class);
        PagingAssertionUtil.assertPageDetails(page, pageSize, pageNumber, 1, 1);
        ApplicationCodeGetSummaryDto firstEntry = page.getContent().get(0);
        assertEquals("AP99004", firstEntry.getApplicationCode());
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
                        List.of("title"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(
                                Optional.of("AP99004"),
                                Optional.of(
                                        "Request for Certificate of Refusal to State a Case (Civil)")),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);
        ApplicationCodePage page = responseSpec.as(ApplicationCodePage.class);
        PagingAssertionUtil.assertPageDetails(page, pageSize, pageNumber, 1, 1);
        Assertions.assertNull(page.getContent());
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
                        List.of("incorrect"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(
                                Optional.of("AP99004"),
                                Optional.of(
                                        "Request for Certificate of Refusal to State a Case (Civil)")),
                        new OpenApiPageMetaData());
        // assert the response
        responseSpec.then().statusCode(400);
    }

    // NOTE: Spring is more forgiving in this scenario and defaults the page number to
    // 0 and returns a 200. Our implementation
    // returns a 500
    @Test
    public void givenValidRequest_whenGetApplicationCodesWithPagingInvalidPageNumber_thenReturn200()
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
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(
                                Optional.of("AP99004"),
                                Optional.of(
                                        "Request for Certificate of Refusal to State a Case (Civil)")),
                        new OpenApiPageMetaData());
        // assert the response
        responseSpec.then().statusCode(400);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);
        Assertions.assertTrue(
                problemDetail.getDetail().endsWith("must be greater than or equal to 1"));
        Assertions.assertEquals("Constraint Error", problemDetail.getTitle());
        Assertions.assertEquals(400, problemDetail.getStatus());
        Assertions.assertEquals(
                CommonAppError.CONSTRAINT_ERROR.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    // NOTE: Spring defaults the page size to the max size if we try and increase it beyond. This
    // does not behave
    // accordingly
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
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(
                                Optional.of("AP99004"),
                                Optional.of(
                                        "Request for Certificate of Refusal to State a Case (Civil)")),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(400);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);
        Assertions.assertTrue(
                problemDetail.getDetail().endsWith("must be less than or equal to 100"));
        Assertions.assertEquals("Constraint Error", problemDetail.getTitle());
        Assertions.assertEquals(400, problemDetail.getStatus());
        Assertions.assertEquals(
                CommonAppError.CONSTRAINT_ERROR.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    @Test
    public void
            givenValidRequest_whenGetApplicationCodesWithPagingInvalidPageSizeType_thenReturn200()
                    throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute the functionality
        int pageSize = maxPageSize;
        int pageNumber = 0;
        OpenApiPageMetaData openApiPageMetaData = new OpenApiPageMetaData();
        String token = tokenGenerator.fetchTokenForRole().getToken();
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        req -> {
                            RequestSpecification specification =
                                    given().header("Authorization", "Bearer " + token);
                            specification =
                                    specification.queryParam(
                                            openApiPageMetaData.getPageSizeQueryName(),
                                            "invalid-type");
                            return specification;
                        },
                        openApiPageMetaData);

        // assert the response
        responseSpec.then().statusCode(400);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(
                "Problem with value invalid-type for parameter "
                        + openApiPageMetaData.getPageSizeQueryName(),
                problemDetail.getDetail());
        Assertions.assertEquals(400, problemDetail.getStatus());
        Assertions.assertEquals(
                CommonAppError.TYPE_MISMATCH_ERROR.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    @Test
    public void givenValidRequest_whenGetApplicationCodesForCodeNotValid_thenReturn404()
            throws Exception {

        // execute the functionality
        String id = "doesntexist";
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrlWithDate(WEB_CONTEXT + "/" + id, OffsetDateTime.now()),
                        getATokenWithValidCredentials()
                                .roles(List.of(RoleEnum.ADMIN))
                                .build()
                                .fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(404);
        ProblemDetail codeDto = responseSpec.as(ProblemDetail.class);
        assertEquals(
                ApplicationCodeError.CODE_NOT_FOUND.getCode().getType().get(), codeDto.getType());
        assertEquals(
                ApplicationCodeError.CODE_NOT_FOUND.getCode().getMessage(), codeDto.getDetail());
        assertEquals(
                ApplicationCodeError.CODE_NOT_FOUND.getCode().getMessage(), codeDto.getTitle());
        assertEquals("/" + WEB_CONTEXT + "/" + id, codeDto.getInstance().toString());

        assertTrue(
                Pattern.matches(
                        getExpectedLog(
                                START_AUDIT_LOG, GET_APPCODE_AUDIT_ACTION, OperationStatus.STARTED),
                        logCaptor.getInfoLogs().get(0)));

        assertTrue(
                Pattern.matches(
                        getExpectedLog(
                                "Completion fail audit",
                                GET_APPCODE_AUDIT_ACTION,
                                OperationStatus.FAILED),
                        logCaptor.getInfoLogs().get(1)));
    }

    @Test
    public void givenValidRequest_whenGetApplicationCodesForDateNotValid_thenReturn404()
            throws Exception {
        String id = APPCODE_CODE;
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrlWithDate(
                                WEB_CONTEXT + "/" + id, OffsetDateTime.parse("1915-01-01T00:00Z")),
                        getATokenWithValidCredentials()
                                .roles(List.of(RoleEnum.ADMIN))
                                .build()
                                .fetchTokenForRole());

        responseSpec.then().statusCode(404);
        ProblemDetail codeDto = responseSpec.as(ProblemDetail.class);
        assertEquals(
                ApplicationCodeError.CODE_NOT_FOUND.getCode().getType().get(), codeDto.getType());
        assertEquals(
                ApplicationCodeError.CODE_NOT_FOUND.getCode().getMessage(), codeDto.getDetail());
        assertEquals(
                ApplicationCodeError.CODE_NOT_FOUND.getCode().getMessage(), codeDto.getTitle());
        assertEquals("/" + WEB_CONTEXT + "/" + id, codeDto.getInstance().toString());

        assertTrue(
                Pattern.matches(
                        getExpectedLog(
                                START_AUDIT_LOG, GET_APPCODE_AUDIT_ACTION, OperationStatus.STARTED),
                        logCaptor.getInfoLogs().get(0)));

        assertTrue(
                Pattern.matches(
                        getExpectedLog(
                                "Completion fail audit",
                                GET_APPCODE_AUDIT_ACTION,
                                OperationStatus.FAILED),
                        logCaptor.getInfoLogs().get(1)));
    }

    @Test
    public void givenValidRequest_whenGetApplicationCodesReturnsMultipleRecords_thenReturn400()
            throws Exception {

        // a date that is within range for the offset but out of range for the main fee
        when(clock.instant()).thenReturn(Instant.parse(CURRENT_TIME));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        String id = DUPLICATE_APPCODE_CODE;
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrlWithDate(
                                WEB_CONTEXT + "/" + id,
                                OffsetDateTime.parse("2016-01-01T00:00:00Z")),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(409);
        ProblemAssertUtil.assertEquals(
                ApplicationCodeError.DUPLICATE_CODE_FOUND.getCode(), responseSpec);
    }

    @Test
    public void givenValidRequest_whenGetWithMultipleTemplateValues_thenReturn200()
            throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.USER)).build();

        String id = "SW99007";
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrlWithDate(
                                WEB_CONTEXT + "/" + id, OffsetDateTime.parse(DATE_TO_FIND_CODE)),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);
        ApplicationCodeGetDetailDto response = responseSpec.as(ApplicationCodeGetDetailDto.class);

        // assert
        Assertions.assertEquals(
                "Application for an order to allow the applicant "
                        + "to inspect or take copies of bankers books held by {{Name of Bank}} in respect "
                        + "of criminal proceedings at {{Name of Court}}.",
                response.getWording().getTemplate());
        Assertions.assertEquals(2, response.getWording().getSubstitutionKeyConstraints().size());
        Assertions.assertEquals(
                "Name of Bank",
                response.getWording().getSubstitutionKeyConstraints().get(0).getKey());
        Assertions.assertEquals(
                TemplateConstraint.TypeEnum.TEXT,
                response.getWording()
                        .getSubstitutionKeyConstraints()
                        .get(0)
                        .getConstraint()
                        .getType());
        Assertions.assertEquals(
                100,
                response.getWording()
                        .getSubstitutionKeyConstraints()
                        .get(0)
                        .getConstraint()
                        .getLength());

        Assertions.assertEquals(
                "Name of Court",
                response.getWording().getSubstitutionKeyConstraints().get(1).getKey());
        Assertions.assertEquals(
                TemplateConstraint.TypeEnum.TEXT,
                response.getWording()
                        .getSubstitutionKeyConstraints()
                        .get(1)
                        .getConstraint()
                        .getType());
        Assertions.assertEquals(
                100,
                response.getWording()
                        .getSubstitutionKeyConstraints()
                        .get(1)
                        .getConstraint()
                        .getLength());

        // assert the audit log message
        assertTrue(
                Pattern.matches(
                        getExpectedLog(
                                START_AUDIT_LOG, GET_APPCODE_AUDIT_ACTION, OperationStatus.STARTED),
                        logCaptor.getInfoLogs().get(0)));

        assertTrue(
                Pattern.matches(
                        getExpectedLog(
                                COMPLETION_AUDIT_LOG,
                                GET_APPCODE_AUDIT_ACTION,
                                OperationStatus.COMPLETED),
                        logCaptor.getInfoLogs().get(1)));
    }

    private ApplicationCodeGetSummaryDto
            generateDefaultApplicationCodeGetSummaryDtoAssertionPayload(
                    Optional<String> mainFeeDesc,
                    Optional<Double> mainFeeAmt,
                    Optional<Double> offsiteFeeAmt) {
        ApplicationCodeGetSummaryDto applicationCodeGetSummaryDto =
                new ApplicationCodeGetSummaryDto();
        applicationCodeGetSummaryDto.setApplicationCode("AD99002");
        applicationCodeGetSummaryDto.setTitle("Copy documents (electronic)");
        TemplateDetail templateDetail = new TemplateDetail();
        templateDetail.setTemplate(
                "Request for copy documents on computer disc or in electronic form");
        templateDetail.setSubstitutionKeyConstraints(new ArrayList<>());
        applicationCodeGetSummaryDto.setWording(templateDetail);
        applicationCodeGetSummaryDto.setIsFeeDue(true);
        applicationCodeGetSummaryDto.setRequiresRespondent(false);
        applicationCodeGetSummaryDto.setBulkRespondentAllowed(false);
        applicationCodeGetSummaryDto.setFeeReference(JsonNullable.of("CO1.1"));

        if (mainFeeDesc.isPresent()) {
            applicationCodeGetSummaryDto.setFeeDescription(JsonNullable.of(mainFeeDesc.get()));
        }

        if (mainFeeAmt.isPresent()) {
            applicationCodeGetSummaryDto.setFeeAmount(
                    JsonNullable.of(new ApplicationCodeGetSummaryDtoFeeAmount()));
            applicationCodeGetSummaryDto
                    .getFeeAmount()
                    .get()
                    .setValue(Math.round(mainFeeAmt.get() * 100));
        }

        if (offsiteFeeAmt.isPresent()) {
            applicationCodeGetSummaryDto.setOffsiteFeeAmount(
                    JsonNullable.of(new ApplicationCodeGetSummaryDtoFeeAmount()));
            applicationCodeGetSummaryDto
                    .getOffsiteFeeAmount()
                    .get()
                    .setValue(Math.round(offsiteFeeAmt.get() * 100));
        }
        return applicationCodeGetSummaryDto;
    }

    private ApplicationCodeGetDetailDto generateDefaultApplicationCodeGetDetailDtoAssertionPayload(
            Optional<String> mainFeeDesc,
            Optional<Double> mainFeeAmt,
            Optional<Double> offsiteFeeAmt) {

        ApplicationCodeGetDetailDto applicationCodeGetSummaryDto =
                new ApplicationCodeGetDetailDto();

        applicationCodeGetSummaryDto.setApplicationCode("AD99002");
        applicationCodeGetSummaryDto.setTitle("Copy documents (electronic)");
        TemplateDetail templateDetail = new TemplateDetail();
        templateDetail.setTemplate(
                "Request for copy documents on computer disc or in electronic form");
        applicationCodeGetSummaryDto.setWording(templateDetail);
        templateDetail.setSubstitutionKeyConstraints(new ArrayList<>());

        applicationCodeGetSummaryDto.setIsFeeDue(true);
        applicationCodeGetSummaryDto.setRequiresRespondent(false);
        applicationCodeGetSummaryDto.setBulkRespondentAllowed(false);
        applicationCodeGetSummaryDto.setFeeReference(JsonNullable.of("CO1.1"));

        if (mainFeeDesc.isPresent()) {
            applicationCodeGetSummaryDto.setFeeDescription(JsonNullable.of(mainFeeDesc.get()));
        }

        if (mainFeeAmt.isPresent()) {
            applicationCodeGetSummaryDto.setFeeAmount(
                    JsonNullable.of(new ApplicationCodeGetSummaryDtoFeeAmount()));
            applicationCodeGetSummaryDto
                    .getFeeAmount()
                    .get()
                    .setValue(Math.round(mainFeeAmt.get() * 100));
        }

        if (offsiteFeeAmt.isPresent()) {
            applicationCodeGetSummaryDto.setOffsiteFeeAmount(
                    JsonNullable.of(new ApplicationCodeGetSummaryDtoFeeAmount()));
            applicationCodeGetSummaryDto
                    .getOffsiteFeeAmount()
                    .get()
                    .setValue(Math.round(offsiteFeeAmt.get() * 100));
        }
        return applicationCodeGetSummaryDto;
    }

    private void assertApplicationCode(
            ApplicationCodeGetSummaryDto actual, ApplicationCodeGetSummaryDto expected) {
        assertEquals(expected.getApplicationCode(), actual.getApplicationCode());
        assertEquals(expected.getTitle(), actual.getTitle());
        assertEquals(expected.getWording(), actual.getWording());
        assertEquals(expected.getIsFeeDue(), actual.getIsFeeDue());
        assertEquals(expected.getRequiresRespondent(), actual.getRequiresRespondent());
        assertEquals(expected.getBulkRespondentAllowed(), actual.getBulkRespondentAllowed());

        if (expected.getFeeDescription().isPresent()) {
            assertEquals(
                    expected.getFeeAmount().get().getValue(),
                    actual.getFeeAmount().get().getValue());
        } else {
            assertEquals(expected.getFeeAmount().isPresent(), actual.getFeeAmount().isPresent());
        }

        if (expected.getOffsiteFeeAmount().isPresent()) {
            assertEquals(
                    expected.getOffsiteFeeAmount().get().getValue(),
                    actual.getOffsiteFeeAmount().get().getValue());
        } else {
            assertEquals(
                    expected.getOffsiteFeeAmount().isPresent(),
                    actual.getOffsiteFeeAmount().isPresent());
        }

        if (expected.getFeeDescription().isPresent()) {
            assertEquals(expected.getFeeDescription(), actual.getFeeDescription());
        } else {
            assertEquals(
                    expected.getFeeDescription().isPresent(),
                    actual.getFeeDescription().isPresent());
        }

        if (expected.getFeeReference().isPresent()) {
            assertEquals(expected.getFeeReference(), actual.getFeeReference());
        } else {
            assertEquals(
                    expected.getFeeReference().isPresent(), actual.getFeeReference().isPresent());
        }
    }

    private void assertApplicationCode(
            ApplicationCodeGetDetailDto actual, ApplicationCodeGetDetailDto expected) {
        assertEquals(expected.getApplicationCode(), actual.getApplicationCode());
        assertEquals(expected.getTitle(), actual.getTitle());
        assertEquals(expected.getWording(), actual.getWording());
        assertEquals(expected.getIsFeeDue(), actual.getIsFeeDue());
        assertEquals(expected.getRequiresRespondent(), actual.getRequiresRespondent());
        assertEquals(expected.getBulkRespondentAllowed(), actual.getBulkRespondentAllowed());
        if (expected.getFeeDescription().isPresent()) {
            assertEquals(
                    expected.getFeeAmount().get().getValue(),
                    actual.getFeeAmount().get().getValue());
        } else {
            assertEquals(expected.getFeeAmount().isPresent(), actual.getFeeAmount().isPresent());
        }

        if (expected.getOffsiteFeeAmount().isPresent()) {
            assertEquals(
                    expected.getOffsiteFeeAmount().get().getValue(),
                    actual.getOffsiteFeeAmount().get().getValue());
        } else {
            assertEquals(
                    expected.getOffsiteFeeAmount().isPresent(),
                    actual.getOffsiteFeeAmount().isPresent());
        }

        if (expected.getFeeDescription().isPresent()) {
            assertEquals(expected.getFeeDescription(), actual.getFeeDescription());
        } else {
            assertEquals(
                    expected.getFeeDescription().isPresent(),
                    actual.getFeeDescription().isPresent());
        }

        if (expected.getFeeReference().isPresent()) {
            assertEquals(expected.getFeeReference(), actual.getFeeReference());
        } else {
            assertEquals(
                    expected.getFeeReference().isPresent(), actual.getFeeReference().isPresent());
        }
    }

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrlWithDate("application-codes/2", OffsetDateTime.now()))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }

    private String getExpectedLog(String event, String action, OperationStatus operationStatus) {
        return "%s\\s*-p_requestaction=%s\\R-p_messageuuid=.*\\R-p_messagestatus=%s\\R-p_messagecontent=.*"
                .formatted(event, action, operationStatus.getStatus());
    }

    /**
     * A request specification that knows what query filters can be applied to get application
     * codes.
     */
    @RequiredArgsConstructor
    static class ApplicationCodeRequestFilter implements UnaryOperator<RequestSpecification> {
        private final Optional<String> appCode;
        private final Optional<String> appTitle;

        @Override
        public RequestSpecification apply(RequestSpecification rs) {
            if (appCode.isPresent()) {
                rs = rs.queryParam("code", appCode.get());
            }

            if (appTitle.isPresent()) {
                rs = rs.queryParam("title", appTitle.get());
            }

            return rs;
        }
    }

    /**
     * A request specification that knows what filters can be applied to get specific application
     * code.
     */
    @RequiredArgsConstructor
    static class SpecificApplicationCodeRequestFilter
            implements UnaryOperator<RequestSpecification> {
        private final Optional<String> dateValue;

        @Override
        public RequestSpecification apply(RequestSpecification rs) {
            if (dateValue.isPresent()) {
                rs = rs.queryParam("date", dateValue.get());
            }

            return rs;
        }
    }
}
