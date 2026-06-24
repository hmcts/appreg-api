package uk.gov.hmcts.appregister.controller.applicationcode;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.appregister.applicationcode.api.ApplicationCodeSortFieldEnum;
import uk.gov.hmcts.appregister.applicationcode.audit.AppCodeAuditOperation;
import uk.gov.hmcts.appregister.applicationcode.exception.ApplicationCodeError;
import uk.gov.hmcts.appregister.applicationlist.api.ApplicationListSortFieldEnum;
import uk.gov.hmcts.appregister.audit.event.OperationStatus;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.data.FeeTestData;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetSummaryDtoFeeAmount;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetSummaryDtoOffsiteFeeAmount;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodePage;
import uk.gov.hmcts.appregister.generated.model.SortOrdersInner;
import uk.gov.hmcts.appregister.generated.model.TemplateConstraint;
import uk.gov.hmcts.appregister.generated.model.TemplateDetail;
import uk.gov.hmcts.appregister.testutils.annotation.StabilityTest;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.DataAuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.PagingAssertionUtil;
import uk.gov.hmcts.appregister.testutils.util.TemplateAssertion;

class ApplicationCodeSearchTest extends AbstractApplicationCodeEntryCrudTest {
    private static final String APPLICATION_CODE_FIELD = "application_code";
    private static final String TITLE_QUERY_PARAM = "title";
    private static final String MAIN_FEE_REFERENCE = "CO1.1";
    private static final String COPY_DOCUMENTS_ELECTRONIC = "Copy documents (electronic)";
    private static final String COPY_DOCUMENTS_ELECTRONIC_WORDING =
            "Request for copy documents on computer disc or in electronic form";

    private final EntityManagerFactory entityManagerFactory;

    @Autowired
    ApplicationCodeSearchTest(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Test
    @StabilityTest
    void
            givenValidRequest_whenGetApplicationCodesWithWithMultipleFeesForMainAndOffsite_thenReturn200()
                    throws URISyntaxException,
                            MalformedURLException,
                            JOSEException,
                            JsonProcessingException {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionaity
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        "00-ecaf9ce5d2b348338cd6b7630c837186-7b3f6a2c9e4d1a8f-01");

        // assert the response
        responseSpec.then().statusCode(200);

        ApplicationCodePage page = responseSpec.as(ApplicationCodePage.class);
        PagingAssertionUtil.assertPageDetails(page, defaultPageSize, 0, 5, TOTAL_APP_CODES_COUNT);
        assertEquals(defaultPageSize, page.getContent().size(), "");

        TemplateAssertion.assertTemplate(
                "Request to copy documents", page.getContent().get(0).getWording());

        // assert
        ApplicationCodeGetSummaryDto applicationCodeDto =
                generateDefaultApplicationCodeGetSummaryDtoAssertionPayload(
                        Optional.of(FEE_DESCRIPTION),
                        Optional.of(200.0),
                        Optional.of(MAIN_FEE_REFERENCE),
                        Optional.of(OFFSITE_FEE_DESCRIPTION),
                        Optional.of(155.0),
                        Optional.of("CO2.1"));

        assertApplicationCode(page.getContent().get(1), applicationCodeDto);

        // assert the audit log message
        assertTrue(
                Pattern.matches(
                        getExpectedLog(
                                START_AUDIT_LOG,
                                GET_APPCODES_AUDIT_ACTION,
                                OperationStatus.STARTED),
                        logCaptor.getInfoLogs().get(0)));

        // Checking for audit log - no filter provided
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_CODES,
                        APPLICATION_CODE_FIELD,
                        null,
                        "",
                        AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT.getType().name(),
                        AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT.getEventName()));

        activityAuditLogAsserter.assertCompletedLogContains(
                GET_APPCODES_AUDIT_ACTION,
                "ecaf9ce5d2b348338cd6b7630c837186",
                Integer.toString(OperationStatus.COMPLETED.getStatus()),
                mapper.writeValueAsString(page));
    }

    @Test
    @StabilityTest
    void
            givenValidRequest_whenGetApplicationCodesWithUserRoleAndMultipleFeesForMainAndOffsite_thenReturn200()
                    throws URISyntaxException,
                            MalformedURLException,
                            JOSEException,
                            JsonProcessingException {
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
                        Optional.of(FEE_DESCRIPTION),
                        Optional.of(200.0),
                        Optional.of(MAIN_FEE_REFERENCE),
                        Optional.of(OFFSITE_FEE_DESCRIPTION),
                        Optional.of(155.0),
                        Optional.of("CO2.1"));

        assertApplicationCode(page.getContent().get(1), applicationCodeDto);

        // assert the audit log message
        assertTrue(
                Pattern.matches(
                        getExpectedLog(
                                START_AUDIT_LOG,
                                GET_APPCODES_AUDIT_ACTION,
                                OperationStatus.STARTED),
                        logCaptor.getInfoLogs().get(0)));

        // Checking for audit log - no filter provided
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_CODES,
                        APPLICATION_CODE_FIELD,
                        null,
                        "",
                        AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT.getType().name(),
                        AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT.getEventName()));

        activityAuditLogAsserter.assertCompletedLogContainsWithUnknownMessageId(
                GET_APPCODES_AUDIT_ACTION,
                Integer.toString(OperationStatus.COMPLETED.getStatus()),
                mapper.writeValueAsString(page));
    }

    @Test
    @StabilityTest
    void givenValidRequest_whenGetApplicationCodesWithOffsiteFeeButNoMain_thenReturn200()
            throws URISyntaxException, MalformedURLException, JOSEException {
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
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(OFFSITE_FEE_DESCRIPTION2),
                        Optional.of(0.50),
                        Optional.of("CO4.1"));

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

        // Checking for audit log - no filter provided
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_CODES,
                        APPLICATION_CODE_FIELD,
                        null,
                        "",
                        AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT.getType().name(),
                        AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT.getEventName()));
    }

    @Test
    @StabilityTest
    void
            givenValidRequest_whenGetApplicationCodesForCodeWithMultipleFeesForMainAndOffsite_thenReturn200()
                    throws URISyntaxException, MalformedURLException, JOSEException {
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
                        Optional.of(FEE_DESCRIPTION),
                        Optional.of(50.0),
                        Optional.of(MAIN_FEE_REFERENCE),
                        Optional.of(OFFSITE_FEE_DESCRIPTION3),
                        Optional.of(70.0),
                        Optional.of(MAIN_FEE_REFERENCE));

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

        // Checking for audit log - filter provided
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_CODES,
                        APPLICATION_CODE_FIELD,
                        null,
                        id,
                        AppCodeAuditOperation.GET_APPLICATION_CODE_AUDIT_EVENT.getType().name(),
                        AppCodeAuditOperation.GET_APPLICATION_CODE_AUDIT_EVENT.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_CODES,
                        "application_code_start_date",
                        null,
                        "2016-01-01",
                        AppCodeAuditOperation.GET_APPLICATION_CODE_AUDIT_EVENT.getType().name(),
                        AppCodeAuditOperation.GET_APPLICATION_CODE_AUDIT_EVENT.getEventName()));
    }

    @Test
    void
            givenOverlappingActiveApplicationCodes_whenGetApplicationCodes_thenCallerSortControlsPageOrder()
                    throws MalformedURLException, JOSEException {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of("title,desc"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(
                                Optional.of(DUPLICATE_APPCODE_CODE), Optional.empty()),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);

        ApplicationCodePage page = responseSpec.as(ApplicationCodePage.class);
        assertEquals(DUPLICATE_APPCODE_CODE, page.getContent().getFirst().getApplicationCode());
        assertEquals(COPY_DOCUMENTS_ELECTRONIC, page.getContent().getFirst().getTitle());
        assertEquals("Condemnation of Unfit Food", page.getContent().get(1).getTitle());
    }

    @Test
    @StabilityTest
    void
            givenValidRequest_whenGetApplicationCodesForCodeWithUserRoleAndMultipleFeesForMainAndOffsite_thenReturn200()
                    throws URISyntaxException, MalformedURLException, JOSEException {
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
                        Optional.of(FEE_DESCRIPTION),
                        Optional.of(50.0),
                        Optional.of(MAIN_FEE_REFERENCE),
                        Optional.of(OFFSITE_FEE_DESCRIPTION3),
                        Optional.of(70.0),
                        Optional.of(MAIN_FEE_REFERENCE));

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
    @StabilityTest
    void givenValidRequest_whenGetApplicationCodesForCodeWithOffsiteFeeButNoMain_thenReturn200()
            throws URISyntaxException, MalformedURLException, JOSEException {
        // The GET-by-code endpoint resolves fees using the request date, not the mocked clock.
        when(clock.instant()).thenReturn(Instant.parse(CURRENT_TIME));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        String id = APPCODE_CODE;
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrlWithDate(
                                WEB_CONTEXT + "/" + id, OffsetDateTime.parse("2021-07-25T00:00Z")),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);

        // assert
        ApplicationCodeGetDetailDto response = responseSpec.as(ApplicationCodeGetDetailDto.class);

        ApplicationCodeGetDetailDto applicationCodeDto =
                generateDefaultApplicationCodeGetDetailDtoAssertionPayload(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(OFFSITE_FEE_DESCRIPTION3),
                        Optional.of(40.0),
                        Optional.of(MAIN_FEE_REFERENCE));

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
    void givenValidRequest_whenGetApplicationCodesDateIsNotCorrectlyFormatted_thenReturn400()
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
    @StabilityTest
    void givenValidRequest_whenGetApplicationCodesDateIsNotSet_thenReturn400() throws Exception {
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
    void givenHistoricDate_whenGetApplicationCodes_thenReturnEmptyPage()
            throws JOSEException, ParseException, MalformedURLException {
        String code = "ZZDATE01";
        saveApplicationCodeWithFees(
                code,
                "ZZFEE01",
                LocalDate.of(2020, Month.JANUARY, 1),
                LocalDate.of(2020, Month.JANUARY, 1),
                LocalDate.of(2020, Month.JANUARY, 1));

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        int pageSize = 10;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(
                                Optional.of(code), Optional.empty(), Optional.of("1000-01-01")),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);
        ApplicationCodePage page = responseSpec.as(ApplicationCodePage.class);
        PagingAssertionUtil.assertPageDetails(page, pageSize, pageNumber, 0, 0);
        assertThat(page.getContent()).isNullOrEmpty();
    }

    /**
     * Demonstrates the optimised query shape: when every returned row shares the same fee
     * reference, the application-code search should reuse the offsite fee and cached fee pair
     * instead of adding extra lookup queries per row.
     */
    @Test
    void
            givenMultipleApplicationCodesSharingFeeReference_whenGetApplicationCodes_thenQueryCountStaysBounded()
                    throws JOSEException, ParseException, MalformedURLException {
        when(clock.instant()).thenReturn(Instant.parse(CURRENT_TIME));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        var activeDate = LocalDate.of(2020, Month.JULY, 25);
        var sharedFeeReference = "QFEE01";
        var singleCode = "QFANSING";
        var multiCodePrefix = "QFANM";

        saveFeePair(sharedFeeReference, activeDate.minusDays(10L), activeDate.minusDays(10L));
        saveApplicationCode(singleCode, sharedFeeReference, activeDate.minusDays(1));

        for (int index = 1; index <= 4; index++) {
            saveApplicationCode(
                    multiCodePrefix + index, sharedFeeReference, activeDate.minusDays(1));
        }

        var tokenGenerator = getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // One matching row gives us the baseline query count for this endpoint.
        var singleResultStatements =
                executeSearchAndCountPreparedStatements(tokenGenerator, singleCode, 1);
        // Four matching rows with the same fee reference should stay at the same query count
        // because the offsite fee and fee pair are reused for the page.
        var multipleResultStatements =
                executeSearchAndCountPreparedStatements(tokenGenerator, multiCodePrefix, 4);

        assertThat(multipleResultStatements).isEqualTo(singleResultStatements);
    }

    @Test
    void givenCodeTitleAndDate_whenGetApplicationCodes_thenReturnDateFilteredPage()
            throws JOSEException, ParseException, MalformedURLException {
        String code = "ZZDATE02";
        saveApplicationCodeWithFees(
                code,
                "ZZFEE02",
                LocalDate.of(2020, Month.JANUARY, 1),
                LocalDate.of(2020, Month.JANUARY, 1),
                LocalDate.of(2020, Month.JANUARY, 1));

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        int pageSize = 10;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of(TITLE_QUERY_PARAM),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(
                                Optional.of(code),
                                Optional.of("Copy documents"),
                                Optional.of("2020-06-01")),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);
        ApplicationCodePage page = responseSpec.as(ApplicationCodePage.class);
        PagingAssertionUtil.assertPageDetails(page, pageSize, pageNumber, 1, 1);
        assertEquals(code, page.getContent().getFirst().getApplicationCode());
    }

    @Test
    void givenInvalidDate_whenGetApplicationCodes_thenReturn400()
            throws MalformedURLException, JOSEException {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of("invalid-date-format")),
                        new OpenApiPageMetaData());

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
    @StabilityTest
    void
            givenValidRequest_whenGetApplicationCodesWithPagingCriteriaWithoutExplicitSort_thenReturn200()
                    throws MalformedURLException, JOSEException {

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
                response, pageSize, pageNumber, 23, TOTAL_APP_CODES_COUNT);

        // assert the first auth code record
        ApplicationCodeGetSummaryDto firstEntry = response.getContent().getFirst();

        assertEquals("AD99003", firstEntry.getApplicationCode());
        assertEquals("Extract from the Court Register", firstEntry.getTitle());
        assertEquals(
                "Certified extract from the court register", firstEntry.getWording().getTemplate());
        assertTrue(firstEntry.getIsFeeDue());
        assertFalse(firstEntry.getRequiresRespondent());
        assertFalse(firstEntry.getBulkRespondentAllowed());
        assertEquals(MAIN_FEE_REFERENCE, firstEntry.getFeeReference().get());
        assertEquals("JP perform function away from court", firstEntry.getFeeDescription().get());
        assertEquals(20000L, firstEntry.getFeeAmount().get().getValue());
        assertEquals(15500L, firstEntry.getOffsiteFeeAmount().get().getValue());

        // assert the second record
        ApplicationCodeGetSummaryDto secondEntry = response.getContent().get(1);
        assertEquals("AD99004", secondEntry.getApplicationCode());
        assertEquals("Certificate of Satisfaction", secondEntry.getTitle());
        assertEquals(
                "Request for a certificate of satisfaction of debt registered in the register "
                        + "of judgements, orders and fines",
                secondEntry.getWording().getTemplate());
        assertFalse(secondEntry.getIsFeeDue());
        assertFalse(secondEntry.getRequiresRespondent());
        assertFalse(secondEntry.getBulkRespondentAllowed());
        assertFalse(secondEntry.getFeeReference().isPresent());
        assertFalse(secondEntry.getFeeDescription().isPresent());
        assertFalse(secondEntry.getFeeAmount().isPresent());
        assertTrue(secondEntry.getOffsiteFeeAmount().isPresent());
    }

    @Test
    void givenValidRequest_whenGetApplicationCodes_ensureOffsiteFeeIsPresentForAll_returns200()
            throws MalformedURLException, JOSEException {
        // create the token to send
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute the functionality
        int pageSize = 100;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(Optional.of("ZS99007"), Optional.empty()));
        responseSpec.then().statusCode(200);

        ApplicationCodePage response = responseSpec.as(ApplicationCodePage.class);
        assertEquals(15500, response.getContent().get(0).getOffsiteFeeAmount().get().getValue());
        assertFalse(response.getContent().get(0).getFeeAmount().isPresent());
    }

    @Test
    void
            givenValidRequest_whenGetAppCodeByCodeAndDate_ensureOffsiteFeeIsPresentWithNullOffsiteFeeRef_returns200()
                    throws URISyntaxException, MalformedURLException, JOSEException {
        // create the token to send
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(
                                WEB_CONTEXT
                                        + "/"
                                        + "AD99004"
                                        + "?date="
                                        + LocalDate.now(java.time.ZoneOffset.UTC)
                                                .format(DateTimeFormatter.ISO_LOCAL_DATE)),
                        tokenGenerator.fetchTokenForRole());
        responseSpec.then().statusCode(200);

        ApplicationCodeGetDetailDto detailDto = responseSpec.as(ApplicationCodeGetDetailDto.class);
        assertTrue(
                detailDto.getOffsiteFeeAmount().isPresent(),
                "Offsite fee amount should be present for all records");
    }

    @Test
    @StabilityTest
    void givenValidRequest_whenGetApplicationCodesWithPagingCriteriaWithExplicitSort_thenReturn200()
            throws MalformedURLException, JOSEException {

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
                response, pageSize, pageNumber, 23, TOTAL_APP_CODES_COUNT);

        // assert records are sorted based on the title of the auth codes
        ApplicationCodeGetSummaryDto firstEntry = response.getContent().get(0);
        ApplicationCodeGetSummaryDto secondEntry = response.getContent().get(1);

        assertEquals("AP99001", firstEntry.getApplicationCode());
        assertEquals("SW99009", secondEntry.getApplicationCode());
    }

    @Test
    @StabilityTest
    void givenValidRequest_whenGetApplicationCodesWithPagingNoResult_thenReturn200()
            throws MalformedURLException, JOSEException {

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
                                Optional.of("not exist"), Optional.of("does not exist")),
                        new OpenApiPageMetaData());

        // assert the response is successful with no content
        responseSpec.then().statusCode(200);
        ApplicationCodePage response = responseSpec.as(ApplicationCodePage.class);
        PagingAssertionUtil.assertPageDetails(response, pageSize, pageNumber, 0, 0);

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_CODES,
                        APPLICATION_CODE_FIELD,
                        null,
                        null,
                        AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT.getType().name(),
                        AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT.getEventName()));
    }

    @Test
    @StabilityTest
    void givenValidRequest_whenGetApplicationCodesWithPagingApplicationCodeFilter_thenReturn200()
            throws MalformedURLException, JOSEException {

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

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_CODES,
                        APPLICATION_CODE_FIELD,
                        null,
                        "CT99002",
                        AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT.getType().name(),
                        AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT.getEventName()));
    }

    @Test
    @StabilityTest
    void givenValidRequest_whenGetApplicationCodesWithPagingTitleFilter_thenReturn200()
            throws MalformedURLException, JOSEException {

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

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_CODES,
                        "application_code_title",
                        null,
                        "Certificate of Satisfaction",
                        AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT.getType().name(),
                        AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT.getEventName()));
    }

    @Test
    @StabilityTest
    void givenValidRequest_whenGetApplicationCodesWithPagingAllFilter_thenReturn200()
            throws MalformedURLException, JOSEException {
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

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_CODES,
                        APPLICATION_CODE_FIELD,
                        null,
                        "AP99004",
                        AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT.getType().name(),
                        AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_CODES,
                        "application_code_title",
                        null,
                        "Request for Certificate of Refusal to State a Case \\(Civil\\)",
                        AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT.getType().name(),
                        AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT.getEventName()));
    }

    @Test
    @StabilityTest
    void givenValidRequest_whenGetApplicationCodesWithPageNumberBeyondResultBoundary_thenReturn200()
            throws MalformedURLException, JOSEException {
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
        assertThat(page.getContent()).isEmpty();
    }

    @StabilityTest
    void givenApplicationCodeSuccessfulSort_whenSearchWithAllSortKeys_thenSuccessResponse()
            throws MalformedURLException, JOSEException {
        for (ApplicationCodeSortFieldEnum applicationCodeSortFieldEnum :
                ApplicationCodeSortFieldEnum.values()) {

            // create the token
            TokenGenerator tokenGenerator =
                    getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

            // test the functionality
            Response responseSpec =
                    restAssuredClient.executeGetRequestWithPaging(
                            Optional.of(10),
                            Optional.of(0),
                            List.of(applicationCodeSortFieldEnum.getApiValue() + "," + "desc"),
                            getLocalUrl(WEB_CONTEXT),
                            tokenGenerator.fetchTokenForRole());

            ApplicationCodePage page = responseSpec.as(ApplicationCodePage.class);

            // make sure the order response marries with the request data
            assertEquals(1, page.getSort().getOrders().size());
            assertEquals(
                    SortOrdersInner.DirectionEnum.DESC,
                    page.getSort().getOrders().get(0).getDirection());
            assertEquals(
                    applicationCodeSortFieldEnum.getApiValue(),
                    page.getSort().getOrders().get(0).getProperty());
            responseSpec.then().statusCode(200);
        }

        assertTrue(ApplicationListSortFieldEnum.values().length > 0);
    }

    @Test
    void givenValidRequest_whenGetApplicationCodesWithPagingInvalidSortQuery_thenReturn400()
            throws MalformedURLException, JOSEException {
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
    void givenValidRequest_whenGetApplicationCodesWithPagingInvalidPageNumber_thenReturn400()
            throws MalformedURLException, JOSEException {
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
        assertTrue(problemDetail.getDetail().endsWith("must be greater than or equal to 1"));
        assertEquals("Constraint Error", problemDetail.getTitle());
        assertEquals(400, problemDetail.getStatus());
        assertEquals(
                CommonAppError.CONSTRAINT_ERROR.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    // NOTE: Spring defaults the page size to the max size if we try and increase it beyond. This
    // does not behave
    // accordingly
    @Test
    void
            givenValidRequest_whenGetApplicationCodesWithPagingInvalidPageSizeBeyondDefault_thenReturn400()
                    throws MalformedURLException, JOSEException {
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
        assertTrue(problemDetail.getDetail().endsWith("must be less than or equal to 100"));
        assertEquals("Constraint Error", problemDetail.getTitle());
        assertEquals(400, problemDetail.getStatus());
        assertEquals(
                CommonAppError.CONSTRAINT_ERROR.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    @Test
    void givenValidRequest_whenGetApplicationCodesWithPagingInvalidPageSizeType_thenReturn200()
            throws JOSEException, MalformedURLException {
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
        assertEquals(
                "Problem with value invalid-type for parameter "
                        + openApiPageMetaData.getPageSizeQueryName(),
                problemDetail.getDetail());
        assertEquals(400, problemDetail.getStatus());
        assertEquals(
                CommonAppError.TYPE_MISMATCH_ERROR.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    @Test
    @StabilityTest
    void givenValidRequest_whenGetApplicationCodesForCodeNotValid_thenReturn404()
            throws URISyntaxException, MalformedURLException, JOSEException {

        // execute the functionality
        String id = "notexist";
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrlWithDate(
                                WEB_CONTEXT + "/" + id,
                                OffsetDateTime.now(java.time.ZoneOffset.UTC)),
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
    void givenValidRequest_whenGetApplicationCodesForDateNotValid_thenReturn404()
            throws URISyntaxException, MalformedURLException, JOSEException {
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
    void
            givenValidRequest_whenGetApplicationCodesReturnsMultipleRecords_thenReturnPreferredActiveRecord()
                    throws URISyntaxException, MalformedURLException, JOSEException {

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

        responseSpec.then().statusCode(200);

        ApplicationCodeGetDetailDto response = responseSpec.as(ApplicationCodeGetDetailDto.class);
        assertEquals("MS99006", response.getApplicationCode());
        assertEquals("Condemnation of Unfit Food", response.getTitle());
        assertFalse(response.getEndDate().isPresent());
    }

    @Test
    void givenValidRequest_whenGetWithMultipleTemplateValues_thenReturn200()
            throws URISyntaxException, MalformedURLException, JOSEException {
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
        assertEquals(
                "Application for an order to allow the applicant "
                        + "to inspect or take copies of bankers books held by {{Name of Bank}} in respect "
                        + "of criminal proceedings at {{Name of Court}}.",
                response.getWording().getTemplate());
        assertEquals(2, response.getWording().getSubstitutionKeyConstraints().size());
        assertEquals(
                "Name of Bank",
                response.getWording().getSubstitutionKeyConstraints().get(0).getKey());
        assertEquals(
                TemplateConstraint.TypeEnum.TEXT,
                response.getWording()
                        .getSubstitutionKeyConstraints()
                        .get(0)
                        .getConstraint()
                        .getType());
        assertEquals(
                100,
                response.getWording()
                        .getSubstitutionKeyConstraints()
                        .get(0)
                        .getConstraint()
                        .getLength());

        assertEquals(
                "Name of Court",
                response.getWording().getSubstitutionKeyConstraints().get(1).getKey());
        assertEquals(
                TemplateConstraint.TypeEnum.TEXT,
                response.getWording()
                        .getSubstitutionKeyConstraints()
                        .get(1)
                        .getConstraint()
                        .getType());
        assertEquals(
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

    @Test
    @StabilityTest
    void givenASuccessfulFilterPartialCode_whenSearch_thenSuccessResponse()
            throws MalformedURLException, JOSEException {
        // a date that is within range for the offset but out of range for the main fee
        when(clock.instant()).thenReturn(Instant.parse(CURRENT_TIME));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(Optional.of("99001"), Optional.empty()),
                        new OpenApiPageMetaData());
        responseSpec.then().statusCode(200);

        // assert
        ApplicationCodePage page = responseSpec.as(ApplicationCodePage.class);
        assertEquals(6, page.getContent().size());
        assertEquals("AD99001", page.getContent().get(0).getApplicationCode());
        assertEquals("AP99001", page.getContent().get(1).getApplicationCode());
        assertEquals("CT99001", page.getContent().get(2).getApplicationCode());
        assertEquals("MS99001", page.getContent().get(3).getApplicationCode());
        assertEquals("RE99001", page.getContent().get(4).getApplicationCode());
        assertEquals("SW99001", page.getContent().get(5).getApplicationCode());
    }

    @Test
    void givenValidRequest_whenMultipleSortsArePresent_thenReturn400()
            throws MalformedURLException, JOSEException {
        var tokenGenerator = createAdminToken();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(maxPageSize),
                        Optional.of(0),
                        List.of(
                                ApplicationCodeSortFieldEnum.CODE.getApiValue(),
                                ApplicationCodeSortFieldEnum.TITLE.getApiValue()),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(400);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);
        assertEquals(
                CommonAppError.MULTIPLE_SORT_NOT_SUPPORTED.getCode().getType().get(),
                problemDetail.getType());
    }

    private ApplicationCodeGetSummaryDto
            generateDefaultApplicationCodeGetSummaryDtoAssertionPayload(
                    Optional<String> mainFeeDesc,
                    Optional<Double> mainFeeAmt,
                    Optional<String> mainFeeReference,
                    Optional<String> offsiteFeeDesc,
                    Optional<Double> offsiteFeeAmt,
                    Optional<String> offsiteFeeReference) {
        ApplicationCodeGetSummaryDto applicationCodeGetSummaryDto =
                new ApplicationCodeGetSummaryDto();
        applicationCodeGetSummaryDto.setApplicationCode("AD99002");
        applicationCodeGetSummaryDto.setTitle(COPY_DOCUMENTS_ELECTRONIC);
        TemplateDetail templateDetail = new TemplateDetail();
        templateDetail.setTemplate(COPY_DOCUMENTS_ELECTRONIC_WORDING);
        templateDetail.setSubstitutionKeyConstraints(new ArrayList<>());
        applicationCodeGetSummaryDto.setWording(templateDetail);
        applicationCodeGetSummaryDto.setIsFeeDue(true);
        applicationCodeGetSummaryDto.setRequiresRespondent(false);
        applicationCodeGetSummaryDto.setBulkRespondentAllowed(false);

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

        if (mainFeeReference.isPresent()) {
            applicationCodeGetSummaryDto.setFeeReference(JsonNullable.of(mainFeeReference.get()));
        }

        if (offsiteFeeDesc.isPresent()) {
            applicationCodeGetSummaryDto.setOffsiteFeeDescription(
                    JsonNullable.of(offsiteFeeDesc.get()));
        }

        if (offsiteFeeAmt.isPresent()) {
            applicationCodeGetSummaryDto.setOffsiteFeeAmount(
                    JsonNullable.of(new ApplicationCodeGetSummaryDtoOffsiteFeeAmount()));
            applicationCodeGetSummaryDto
                    .getOffsiteFeeAmount()
                    .get()
                    .setValue(Math.round(offsiteFeeAmt.get() * 100));
        }

        if (offsiteFeeReference.isPresent()) {
            applicationCodeGetSummaryDto.setOffsiteFeeReference(
                    JsonNullable.of(offsiteFeeReference.get()));
        }

        return applicationCodeGetSummaryDto;
    }

    private ApplicationCodeGetDetailDto generateDefaultApplicationCodeGetDetailDtoAssertionPayload(
            Optional<String> mainFeeDesc,
            Optional<Double> mainFeeAmt,
            Optional<String> mainFeeReference,
            Optional<String> offsiteFeeDesc,
            Optional<Double> offsiteFeeAmt,
            Optional<String> offsiteFeeReference) {

        ApplicationCodeGetDetailDto applicationCodeGetSummaryDto =
                new ApplicationCodeGetDetailDto();

        applicationCodeGetSummaryDto.setApplicationCode("AD99002");
        applicationCodeGetSummaryDto.setTitle(COPY_DOCUMENTS_ELECTRONIC);
        TemplateDetail templateDetail = new TemplateDetail();
        templateDetail.setTemplate(COPY_DOCUMENTS_ELECTRONIC_WORDING);
        applicationCodeGetSummaryDto.setWording(templateDetail);
        templateDetail.setSubstitutionKeyConstraints(new ArrayList<>());

        applicationCodeGetSummaryDto.setIsFeeDue(true);
        applicationCodeGetSummaryDto.setRequiresRespondent(false);
        applicationCodeGetSummaryDto.setBulkRespondentAllowed(false);

        if (mainFeeDesc.isPresent()) {
            applicationCodeGetSummaryDto.setFeeDescription(JsonNullable.of(mainFeeDesc.get()));
        }

        if (mainFeeReference.isPresent()) {
            applicationCodeGetSummaryDto.setFeeReference(JsonNullable.of(mainFeeReference.get()));
        }

        if (mainFeeAmt.isPresent()) {
            applicationCodeGetSummaryDto.setFeeAmount(
                    JsonNullable.of(new ApplicationCodeGetSummaryDtoFeeAmount()));
            applicationCodeGetSummaryDto
                    .getFeeAmount()
                    .get()
                    .setValue(Math.round(mainFeeAmt.get() * 100));
        }

        if (offsiteFeeDesc.isPresent()) {
            applicationCodeGetSummaryDto.setOffsiteFeeDescription(
                    JsonNullable.of(offsiteFeeDesc.get()));
        }

        if (offsiteFeeReference.isPresent()) {
            applicationCodeGetSummaryDto.setOffsiteFeeReference(
                    JsonNullable.of(offsiteFeeReference.get()));
        }

        if (offsiteFeeAmt.isPresent()) {
            applicationCodeGetSummaryDto.setOffsiteFeeAmount(
                    JsonNullable.of(new ApplicationCodeGetSummaryDtoOffsiteFeeAmount()));
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

        if (expected.getOffsiteFeeAmount().isPresent()) {
            assertEquals(
                    expected.getOffsiteFeeAmount().get().getValue(),
                    actual.getOffsiteFeeAmount().get().getValue());
        } else {
            assertEquals(
                    expected.getOffsiteFeeAmount().isPresent(),
                    actual.getOffsiteFeeAmount().isPresent());
        }

        if (expected.getOffsiteFeeDescription().isPresent()) {
            assertEquals(expected.getOffsiteFeeDescription(), actual.getOffsiteFeeDescription());
        } else {
            assertEquals(
                    expected.getOffsiteFeeDescription().isPresent(),
                    actual.getOffsiteFeeDescription().isPresent());
        }

        if (expected.getOffsiteFeeReference().isPresent()) {
            assertEquals(expected.getOffsiteFeeReference(), actual.getOffsiteFeeReference());
        } else {
            assertEquals(
                    expected.getOffsiteFeeReference().isPresent(),
                    actual.getOffsiteFeeReference().isPresent());
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
        if (expected.getFeeAmount().isPresent()) {
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

        if (expected.getOffsiteFeeDescription().isPresent()) {
            assertEquals(expected.getOffsiteFeeDescription(), actual.getOffsiteFeeDescription());
        } else {
            assertEquals(
                    expected.getOffsiteFeeDescription().isPresent(),
                    actual.getOffsiteFeeDescription().isPresent());
        }

        if (expected.getOffsiteFeeReference().isPresent()) {
            assertEquals(expected.getOffsiteFeeReference(), actual.getOffsiteFeeReference());
        } else {
            assertEquals(
                    expected.getOffsiteFeeReference().isPresent(),
                    actual.getOffsiteFeeReference().isPresent());
        }
    }

    private String getExpectedLog(String event, String action, OperationStatus operationStatus) {
        return "%s\\s*-p_requestaction=%s\\R-p_messageuuid=.*\\R-p_messagestatus=%s"
                .formatted(event, action, operationStatus.getStatus());
    }

    private void saveApplicationCodeWithFees(
            String code,
            String feeReference,
            LocalDate applicationCodeStartDate,
            LocalDate mainFeeStartDate,
            LocalDate offsiteFeeStartDate)
            throws JOSEException, ParseException {
        saveFeePair(feeReference, mainFeeStartDate, offsiteFeeStartDate);
        saveApplicationCode(code, feeReference, applicationCodeStartDate);
    }

    private void saveFeePair(
            String feeReference, LocalDate mainFeeStartDate, LocalDate offsiteFeeStartDate)
            throws JOSEException, ParseException {
        var jwt = TokenGenerator.builder().build().getJwtFromToken();
        var auth = new JwtAuthenticationToken(jwt, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            Fee mainFee = new FeeTestData().someComplete();
            mainFee.setReference(feeReference);
            mainFee.setDescription(FEE_DESCRIPTION);
            mainFee.setAmount(BigDecimal.valueOf(50));
            mainFee.setOffsite(false);
            mainFee.setStartDate(mainFeeStartDate);
            mainFee.setEndDate(null);
            persistance.save(mainFee);

            Fee offsiteFee = new FeeTestData().someComplete();
            offsiteFee.setReference(feeReference);
            offsiteFee.setDescription(OFFSITE_FEE_DESCRIPTION);
            offsiteFee.setAmount(BigDecimal.valueOf(70));
            offsiteFee.setOffsite(true);
            offsiteFee.setStartDate(offsiteFeeStartDate);
            offsiteFee.setEndDate(null);
            persistance.save(offsiteFee);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void saveApplicationCode(
            String code, String feeReference, LocalDate applicationCodeStartDate)
            throws JOSEException, java.text.ParseException {
        var jwt = TokenGenerator.builder().build().getJwtFromToken();
        var auth = new JwtAuthenticationToken(jwt, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            ApplicationCode applicationCode = new ApplicationCodeTestData().someComplete();
            applicationCode.setCode(code);
            applicationCode.setTitle(COPY_DOCUMENTS_ELECTRONIC);
            applicationCode.setWording(COPY_DOCUMENTS_ELECTRONIC_WORDING);
            applicationCode.setFeeReference(feeReference);
            applicationCode.setFeeDue(YesOrNo.YES);
            applicationCode.setRequiresRespondent(YesOrNo.NO);
            applicationCode.setBulkRespondentAllowed(YesOrNo.NO);
            applicationCode.setStartDate(applicationCodeStartDate);
            applicationCode.setEndDate(null);
            persistance.save(applicationCode);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private long executeSearchAndCountPreparedStatements(
            TokenGenerator tokenGenerator, String codeFilter, int expectedContentSize)
            throws MalformedURLException, JOSEException {
        Statistics statistics = getHibernateStatistics();
        statistics.clear();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeRequestFilter(Optional.of(codeFilter), Optional.empty()),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);

        ApplicationCodePage page = responseSpec.as(ApplicationCodePage.class);
        assertThat(page.getContent()).hasSize(expectedContentSize);

        return statistics.getPrepareStatementCount();
    }

    private Statistics getHibernateStatistics() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        return statistics;
    }

    /**
     * A request specification that knows what query filters can be applied to get application
     * codes.
     */
    static class ApplicationCodeRequestFilter implements UnaryOperator<RequestSpecification> {
        private final Optional<String> appCode;
        private final Optional<String> appTitle;
        private final Optional<String> dateValue;

        ApplicationCodeRequestFilter(Optional<String> appCode, Optional<String> appTitle) {
            this(appCode, appTitle, Optional.empty());
        }

        ApplicationCodeRequestFilter(
                Optional<String> appCode, Optional<String> appTitle, Optional<String> dateValue) {
            this.appCode = appCode;
            this.appTitle = appTitle;
            this.dateValue = dateValue;
        }

        @Override
        public RequestSpecification apply(RequestSpecification rs) {
            if (appCode.isPresent()) {
                rs = rs.queryParam("code", appCode.get());
            }

            if (appTitle.isPresent()) {
                rs = rs.queryParam(TITLE_QUERY_PARAM, appTitle.get());
            }

            if (dateValue.isPresent()) {
                rs = rs.queryParam("date", dateValue.get());
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
