package uk.gov.hmcts.appregister.controller.standardapplicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.generated.model.SortOrdersInner;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPage;
import uk.gov.hmcts.appregister.standardapplicant.api.StandardApplicantSortFieldEnum;
import uk.gov.hmcts.appregister.standardapplicant.audit.StandardApplicantOperation;
import uk.gov.hmcts.appregister.standardapplicant.exception.StandardApplicantCodeError;
import uk.gov.hmcts.appregister.testutils.annotation.StabilityTest;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.DataAuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.PagingAssertionUtil;

@ExtendWith(OutputCaptureExtension.class)
class StandardApplicantControllerSearchTest extends AbstractSecurityControllerTest {
    private static final String WEB_CONTEXT = "standard-applicants";

    @Value("${spring.data.web.pageable.default-page-size}")
    private Integer defaultPageSize;

    @Value("${spring.data.web.pageable.max-page-size}")
    private Integer maxPageSize;

    @MockitoBean private Clock clock; // replaces Clock bean in Spring context

    // The total standard applicant inserted by flyway scripts. See V6__InitialTestData.sql
    private static final int TOTAL_STANDARD_APPLICANT_COUNT = 7;

    private static final String APPCODE_CODE = "APP001";
    private static final String APPCODE_CODE_ORGANISATION = "APP005";

    private static final String DUPLICATE_APPCODE_CODE = "APP003";

    @BeforeEach
    void before() {
        when(clock.instant())
                .thenReturn(Instant.now(java.time.Clock.systemUTC()).plus(2, ChronoUnit.DAYS));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.withZone(org.mockito.ArgumentMatchers.any(ZoneId.class))).thenReturn(clock);
    }

    @Test
    void givenValidRequest_whenGetStandardApplicantByCodeForIndividual_thenReturn200()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + APPCODE_CODE),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(200);

        StandardApplicantGetDetailDto returnedSa =
                responseSpec.as(StandardApplicantGetDetailDto.class);

        // assert the data
        Assertions.assertEquals("APP001", returnedSa.getCode());
        Assertions.assertEquals(LocalDate.now(java.time.ZoneOffset.UTC), returnedSa.getStartDate());
        Assertions.assertTrue(returnedSa.getEndDate().isPresent());
        assertNull(returnedSa.getEndDate().get());
        Assertions.assertNotNull(returnedSa.getApplicant().getPerson().getName());
        Assertions.assertEquals("Mr", returnedSa.getApplicant().getPerson().getName().getTitle());
        Assertions.assertEquals(
                "John", returnedSa.getApplicant().getPerson().getName().getFirstName());
        Assertions.assertNull(
                returnedSa.getApplicant().getPerson().getName().getMiddleName().get());
        Assertions.assertNull(
                returnedSa.getApplicant().getPerson().getName().getMiddleName().get());
        Assertions.assertEquals(
                "Smith", returnedSa.getApplicant().getPerson().getName().getLastName());
        Assertions.assertEquals(
                "123 High Street",
                returnedSa.getApplicant().getPerson().getContactDetails().getAddressLine1());
        Assertions.assertNull(
                returnedSa.getApplicant().getPerson().getContactDetails().getAddressLine2().get());
        Assertions.assertNull(
                returnedSa.getApplicant().getPerson().getContactDetails().getAddressLine3().get());
        Assertions.assertEquals(
                "Townsville",
                returnedSa.getApplicant().getPerson().getContactDetails().getAddressLine4().get());
        Assertions.assertNull(
                returnedSa.getApplicant().getPerson().getContactDetails().getAddressLine5().get());
        Assertions.assertEquals(
                "john.smith@example.com",
                returnedSa.getApplicant().getPerson().getContactDetails().getEmail().get());
        Assertions.assertEquals(
                "07123456789",
                returnedSa.getApplicant().getPerson().getContactDetails().getMobile().get());
        Assertions.assertEquals(
                "01234567890",
                returnedSa.getApplicant().getPerson().getContactDetails().getPhone().get());
        Assertions.assertEquals(
                "TS1 1AB", returnedSa.getApplicant().getPerson().getContactDetails().getPostcode());

        // audit assertion
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "standard_applicant_code",
                        null,
                        APPCODE_CODE,
                        StandardApplicantOperation.GET_STANDARD_APPLICANT_BY_CODE.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANT_BY_CODE.getEventName()));
    }

    @Test
    void givenSparsePersonStandardApplicant_whenGetStandardApplicantByCode_thenReturnExplicitNulls()
            throws Exception {
        LocalDate activeDate = LocalDate.now(java.time.ZoneOffset.UTC);
        String sparseCode = "A1348SA1";
        saveSparsePersonStandardApplicant(sparseCode, activeDate.minusDays(1), null);

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + sparseCode),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);

        JsonNode responseBody = mapper.readTree(responseSpec.asString());

        Assertions.assertEquals(sparseCode, responseBody.path("code").asText());
        Assertions.assertEquals(
                activeDate.minusDays(1).toString(), responseBody.path("startDate").asText());
        assertExplicitNull(responseBody, "endDate");
        assertExplicitNull(responseBody, "applicant.person.name.middleName");
        assertExplicitNull(responseBody, "applicant.person.contactDetails.addressLine2");
        assertExplicitNull(responseBody, "applicant.person.contactDetails.addressLine3");
        assertExplicitNull(responseBody, "applicant.person.contactDetails.addressLine4");
        assertExplicitNull(responseBody, "applicant.person.contactDetails.addressLine5");
        assertExplicitNull(responseBody, "applicant.person.contactDetails.phone");
        assertExplicitNull(responseBody, "applicant.person.contactDetails.mobile");
        assertExplicitNull(responseBody, "applicant.person.contactDetails.email");
    }

    @Test
    void givenValidRequest_whenGetStandardApplicantByCodeForOrganisation_thenReturn200()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + APPCODE_CODE_ORGANISATION),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(200);

        StandardApplicantGetDetailDto returnedSa =
                responseSpec.as(StandardApplicantGetDetailDto.class);

        // assert the data
        Assertions.assertEquals(APPCODE_CODE_ORGANISATION, returnedSa.getCode());
        Assertions.assertEquals(
                LocalDate.now(java.time.ZoneOffset.UTC).minusDays(1), returnedSa.getStartDate());
        Assertions.assertTrue(returnedSa.getEndDate().isPresent());
        assertNull(returnedSa.getEndDate().get());
        Assertions.assertEquals(
                "Organisation 1", returnedSa.getApplicant().getOrganisation().getName());
        Assertions.assertEquals(
                "123 High Street",
                returnedSa.getApplicant().getOrganisation().getContactDetails().getAddressLine1());
        Assertions.assertNull(
                returnedSa
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine2()
                        .get());
        Assertions.assertNull(
                returnedSa
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine3()
                        .get());
        Assertions.assertEquals(
                "Townsville",
                returnedSa
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine4()
                        .get());
        Assertions.assertNull(
                returnedSa
                        .getApplicant()
                        .getOrganisation()
                        .getContactDetails()
                        .getAddressLine5()
                        .get());
        Assertions.assertEquals(
                "john.smith@example.com",
                returnedSa.getApplicant().getOrganisation().getContactDetails().getEmail().get());
        Assertions.assertEquals(
                "07123456789",
                returnedSa.getApplicant().getOrganisation().getContactDetails().getMobile().get());
        Assertions.assertEquals(
                "01234567890",
                returnedSa.getApplicant().getOrganisation().getContactDetails().getPhone().get());
        Assertions.assertEquals(
                "TS1 1AB",
                returnedSa.getApplicant().getOrganisation().getContactDetails().getPostcode());

        // audit assertion
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "standard_applicant_code",
                        null,
                        APPCODE_CODE_ORGANISATION,
                        StandardApplicantOperation.GET_STANDARD_APPLICANT_BY_CODE.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANT_BY_CODE.getEventName()));
    }

    @Test
    void givenApp006_whenGetStandardApplicantByCodeWithoutDate_thenReturn200() throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/APP006"), tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);

        StandardApplicantGetDetailDto returnedSa =
                responseSpec.as(StandardApplicantGetDetailDto.class);
        Assertions.assertEquals("APP006", returnedSa.getCode());
        Assertions.assertEquals(
                "Organisation 3", returnedSa.getApplicant().getOrganisation().getName());
    }

    @Test
    void givenDateQuery_whenGetStandardApplicantByCode_thenDateIsIgnored() throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + APPCODE_CODE),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("date", "2025-10-27"));

        responseSpec.then().statusCode(200);

        StandardApplicantGetDetailDto returnedSa =
                responseSpec.as(StandardApplicantGetDetailDto.class);
        Assertions.assertEquals(APPCODE_CODE, returnedSa.getCode());
    }

    @Test
    void givenOversizedCode_whenGetStandardApplicantByCode_thenReturn400AndLogWarning(
            CapturedOutput output) throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + "ABCDEFGHIJK"),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(400);

        Assertions.assertTrue(
                output.toString()
                        .contains(
                                "[400]: Constraints failed for fields:"
                                        + System.lineSeparator()
                                        + "getStandardApplicantByCode.code="
                                        + "size must be between 0 and 10"));
    }

    @Test
    void givenInvalidPageSize_whenGetStandardApplicants_thenReturn400AndLogWarning(
            CapturedOutput output) throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("pageSize", 101));

        responseSpec.then().statusCode(400);

        Assertions.assertTrue(
                output.toString()
                        .contains(
                                "[400]: Constraints failed for fields:"
                                        + System.lineSeparator()
                                        + "getStandardApplicants.pageSize="
                                        + "must be less than or equal to 100"));
    }

    @Test
    void givenValidRequest_whenGetStandardApplicantByCodeAndCodeNotExist_thenReturn404()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + "NotExist"),
                        tokenGenerator.fetchTokenForRole());

        // assert the response
        ProblemDetail returnedSc = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(
                StandardApplicantCodeError.STANDARD_APPLICANT_NOT_FOUND.getCode().getAppCode(),
                returnedSc.getType().toString());
        Assertions.assertEquals(
                StandardApplicantCodeError.STANDARD_APPLICANT_NOT_FOUND
                        .getCode()
                        .getHttpCode()
                        .value(),
                responseSpec.getStatusCode());
    }

    @Test
    void givenDateOutsideApplicantRange_whenGetStandardApplicantByCode_thenDateIsIgnored()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + DUPLICATE_APPCODE_CODE),
                        tokenGenerator.fetchTokenForRole(),
                        rs ->
                                rs.queryParam(
                                        "date",
                                        LocalDate.now(java.time.ZoneOffset.UTC).minusDays(1)));

        // assert the response
        responseSpec.then().statusCode(200);
        StandardApplicantGetDetailDto returnedSa =
                responseSpec.as(StandardApplicantGetDetailDto.class);
        Assertions.assertEquals(DUPLICATE_APPCODE_CODE, returnedSa.getCode());
    }

    @Test
    void givenDuplicateCode_whenGetStandardApplicantByCode_thenReturnPreferredRecord()
            throws Exception {
        String code = "SANULL001";
        LocalDate queryDate = LocalDate.now(java.time.ZoneOffset.UTC);
        saveStandardApplicant(
                code, "Time-Bounded Applicant", queryDate.minusDays(2), queryDate.plusDays(5));
        saveStandardApplicant(code, "Open-Ended Applicant", queryDate.minusDays(1), null);

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + code), tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(200);
        StandardApplicantGetDetailDto returnedSa =
                responseSpec.as(StandardApplicantGetDetailDto.class);
        Assertions.assertEquals(code, returnedSa.getCode());
        Assertions.assertEquals(
                "Open-Ended Applicant", returnedSa.getApplicant().getOrganisation().getName());
        Assertions.assertTrue(returnedSa.getEndDate().isPresent());
        assertNull(returnedSa.getEndDate().get());
    }

    @Test
    void givenDuplicateApplicants_whenGetAllStandardApplicants_thenCallerSortControlsPageOrder()
            throws Exception {
        String code = "SANULL001";
        LocalDate activeDate = LocalDate.now(java.time.ZoneOffset.UTC);
        saveStandardApplicant(
                code, "Time-Bounded Applicant", activeDate.minusDays(2), activeDate.plusDays(5));
        saveStandardApplicant(code, "Open-Ended Applicant", activeDate.minusDays(1), null);

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of("name,desc"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new StandardApplicantRequestFilter(
                                Optional.of(code),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);
        StandardApplicantPage response = responseSpec.as(StandardApplicantPage.class);

        Assertions.assertEquals(2, response.getContent().size());
        Assertions.assertEquals(code, response.getContent().get(0).getCode());
        Assertions.assertEquals(
                "Time-Bounded Applicant",
                response.getContent().get(0).getApplicant().getOrganisation().getName());
        Assertions.assertEquals(code, response.getContent().get(1).getCode());
        Assertions.assertEquals(
                "Open-Ended Applicant",
                response.getContent().get(1).getApplicant().getOrganisation().getName());
    }

    @Test
    void givenReversedDateRange_whenGetAllStandardApplicants_thenDatesAreNormalised()
            throws Exception {
        String code = "SAREV001";
        LocalDate rangeStart = LocalDate.of(2024, Month.MAY, 6);
        LocalDate rangeEnd = LocalDate.of(2025, Month.NOVEMBER, 6);
        saveStandardApplicant(code, "Reversed Range Applicant", rangeEnd, null);

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of("name,asc"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new StandardApplicantRequestFilter(
                                Optional.of(code),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(rangeEnd),
                                Optional.of(rangeStart)),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);
        StandardApplicantPage response = responseSpec.as(StandardApplicantPage.class);

        Assertions.assertEquals(1, response.getContent().size());
        Assertions.assertEquals(code, response.getContent().getFirst().getCode());
        Assertions.assertEquals(
                "Reversed Range Applicant",
                response.getContent().getFirst().getApplicant().getOrganisation().getName());
    }

    @Test
    void
            givenHistoricalDateRange_whenGetAllStandardApplicants_thenPastOverlappingApplicantIsReturned()
                    throws Exception {
        String code = "SAHIST001";
        saveStandardApplicant(
                code,
                "Historical Range Applicant",
                LocalDate.of(2025, Month.NOVEMBER, 6),
                LocalDate.of(2025, Month.NOVEMBER, 20));

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of("name,asc"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new StandardApplicantRequestFilter(
                                Optional.of(code),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(LocalDate.of(2024, Month.MAY, 6)),
                                Optional.of(LocalDate.of(2025, Month.NOVEMBER, 6))),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);
        StandardApplicantPage response = responseSpec.as(StandardApplicantPage.class);

        Assertions.assertEquals(1, response.getContent().size());
        Assertions.assertEquals(code, response.getContent().getFirst().getCode());
        Assertions.assertEquals(
                "Historical Range Applicant",
                response.getContent().getFirst().getApplicant().getOrganisation().getName());
    }

    @StabilityTest
    void givenValidRequest_whenGetAllStandardApplicant_thenReturn200() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT), tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(200);

        StandardApplicantPage responseContent = responseSpec.as(StandardApplicantPage.class);

        // make the assertions
        PagingAssertionUtil.assertPageDetails(
                responseContent, 10, 0, 1, TOTAL_STANDARD_APPLICANT_COUNT);

        // assert
        StandardApplicantGetSummaryDto returnedSc = responseContent.getContent().get(2);
        Assertions.assertEquals("APP003", returnedSc.getCode());

        // audit assertion
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "standard_applicant_code",
                        null,
                        "",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "standard_applicant_start_date",
                        null,
                        "",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));
    }

    @StabilityTest
    @Test
    void
            givenValidRequest_whenGetStandardApplicantWithPagingCriteriaWithoutExplicitSort_thenReturn200()
                    throws Exception {

        // create the token to send
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute the functionality
        int pageSize = 10;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());
        responseSpec.then().statusCode(200);

        StandardApplicantPage response = responseSpec.as(StandardApplicantPage.class);

        // make the assertions
        PagingAssertionUtil.assertPageDetails(
                response, pageSize, pageNumber, 1, TOTAL_STANDARD_APPLICANT_COUNT);

        // assert the first auth code record
        StandardApplicantGetSummaryDto firstEntry = response.getContent().get(0);

        assertEquals("APP001", firstEntry.getCode());
        assertEquals("John", firstEntry.getApplicant().getPerson().getName().getFirstName());
        assertEquals("Smith", firstEntry.getApplicant().getPerson().getName().getLastName());
        assertEquals(
                "123 High Street",
                firstEntry.getApplicant().getPerson().getContactDetails().getAddressLine1());
        assertNotNull(firstEntry.getStartDate());
        assertTrue(firstEntry.getEndDate().isPresent());
        assertNull(firstEntry.getEndDate().get());

        StandardApplicantGetSummaryDto secondEntry = response.getContent().get(1);
        assertEquals("APP002", secondEntry.getCode());
        assertEquals("Jane", secondEntry.getApplicant().getPerson().getName().getFirstName());
        assertEquals("Doe", secondEntry.getApplicant().getPerson().getName().getLastName());
        assertEquals(
                "456 Elm Road",
                secondEntry.getApplicant().getPerson().getContactDetails().getAddressLine1());
        assertNotNull(secondEntry.getStartDate());
        assertTrue(secondEntry.getEndDate().isPresent());
        assertNull(secondEntry.getEndDate().get());

        StandardApplicantGetSummaryDto org = response.getContent().get(6);
        assertEquals("APP006", org.getCode());
        assertEquals("Organisation 3", org.getApplicant().getOrganisation().getName());
        assertEquals(
                "456 Elm Road",
                org.getApplicant().getOrganisation().getContactDetails().getAddressLine1());
        assertEquals(
                "Apt 5",
                org.getApplicant().getOrganisation().getContactDetails().getAddressLine2().get());
        assertEquals(
                "Cityville",
                org.getApplicant().getOrganisation().getContactDetails().getAddressLine4().get());
        assertNotNull(secondEntry.getStartDate());
        assertTrue(secondEntry.getEndDate().isPresent());
        assertNull(secondEntry.getEndDate().get());

        // audit assertion
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "standard_applicant_code",
                        null,
                        "",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "standard_applicant_start_date",
                        null,
                        "",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));
    }

    @Test
    @StabilityTest
    void
            givenValidRequest_whenGetStandardApplicantWithPagingCriteriaWithExplicitSort_thenReturn200()
                    throws Exception {

        // create the token to send
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute the functionality
        int pageSize = 10;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of("name,desc"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());
        responseSpec.then().statusCode(200);

        StandardApplicantPage response = responseSpec.as(StandardApplicantPage.class);

        // assert the response
        PagingAssertionUtil.assertPageDetails(
                response, pageSize, pageNumber, 1, TOTAL_STANDARD_APPLICANT_COUNT);

        // assert records are sorted based on the title of the auth codes
        StandardApplicantGetSummaryDto firstEntry = response.getContent().get(0);
        assertEquals("APP006", firstEntry.getCode());
        assertEquals("Organisation 3", firstEntry.getApplicant().getOrganisation().getName());
        assertEquals(
                "456 Elm Road",
                firstEntry.getApplicant().getOrganisation().getContactDetails().getAddressLine1());
        assertNotNull(firstEntry.getStartDate());
        assertTrue(firstEntry.getEndDate().isPresent());
        assertNull(firstEntry.getEndDate().get());

        StandardApplicantGetSummaryDto secondEntry = response.getContent().get(1);
        assertEquals("APP004", secondEntry.getCode());
        assertEquals("Organisation 2", secondEntry.getApplicant().getOrganisation().getName());
        assertEquals(
                "123 High Street",
                secondEntry.getApplicant().getOrganisation().getContactDetails().getAddressLine1());
        assertNotNull(secondEntry.getStartDate());
        assertTrue(secondEntry.getEndDate().isPresent());
        assertNull(secondEntry.getEndDate().get());

        // audit assertion
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "standard_applicant_code",
                        null,
                        "",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "standard_applicant_start_date",
                        null,
                        "",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));
    }

    @Test
    @StabilityTest
    void givenValidRequest_whenGetStandardApplicantWithPagingNoResult_thenReturn200()
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
                        List.of("name"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new StandardApplicantRequestFilter(
                                Optional.of("not exist"),
                                Optional.of("does not exist"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()),
                        new OpenApiPageMetaData());

        // assert the response is successful with no content
        responseSpec.then().statusCode(200);
        StandardApplicantPage response = responseSpec.as(StandardApplicantPage.class);
        PagingAssertionUtil.assertPageDetails(response, pageSize, pageNumber, 0, 0);

        // audit assertion
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "standard_applicant_code",
                        null,
                        "not exist",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "name",
                        null,
                        "does not exist",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));
    }

    @Test
    @StabilityTest
    void givenValidRequest_whenGetStandardApplicantWithPagingNoResultDateRange_thenReturn200()
            throws Exception {

        Mockito.reset(clock);

        when(clock.instant())
                .thenReturn(Instant.now(java.time.Clock.systemUTC()).minus(2, ChronoUnit.DAYS));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.withZone(org.mockito.ArgumentMatchers.any(ZoneId.class))).thenReturn(clock);

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
                        List.of("name"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new OpenApiPageMetaData());

        // assert the response is successful with no content
        responseSpec.then().statusCode(200);
        StandardApplicantPage response = responseSpec.as(StandardApplicantPage.class);
        PagingAssertionUtil.assertPageDetails(response, pageSize, pageNumber, 0, 0);

        // audit assertion
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "standard_applicant_code",
                        null,
                        "",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "standard_applicant_start_date",
                        null,
                        "",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));
    }

    @Test
    @StabilityTest
    void givenValidRequest_whenGetStandardApplicantWithPagingFilterPartialCode_thenReturn200()
            throws Exception {

        // create a token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        int pageSize = 2;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of("name"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new StandardApplicantRequestFilter(
                                Optional.of("APP00"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()),
                        new OpenApiPageMetaData());

        // assert the response
        Assertions.assertEquals(200, responseSpec.getStatusCode());
        StandardApplicantPage response = responseSpec.as(StandardApplicantPage.class);
        PagingAssertionUtil.assertPageDetails(
                response, pageSize, pageNumber, 4, TOTAL_STANDARD_APPLICANT_COUNT);

        // audit assertion
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "standard_applicant_code",
                        null,
                        "APP00",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "name",
                        null,
                        "",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));
    }

    @Test
    @StabilityTest
    void
            givenValidRequest_whenGetStandardApplicantWithPagingNameFilterPartialForOrganisation_thenReturn200()
                    throws Exception {

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute functionality
        int pageSize = 3;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of("name"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new StandardApplicantRequestFilter(
                                Optional.empty(),
                                Optional.of("ORG"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);
        StandardApplicantPage response = responseSpec.as(StandardApplicantPage.class);
        PagingAssertionUtil.assertPageDetails(response, pageSize, pageNumber, 1, 3);

        Assertions.assertEquals(
                "Organisation 1",
                response.getContent().get(0).getApplicant().getOrganisation().getName());
        Assertions.assertEquals(
                "Organisation 2",
                response.getContent().get(1).getApplicant().getOrganisation().getName());
        Assertions.assertEquals(
                "Organisation 3",
                response.getContent().get(2).getApplicant().getOrganisation().getName());

        // audit assertion
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "standard_applicant_code",
                        null,
                        "",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "name",
                        null,
                        "ORG",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));
    }

    @Test
    @StabilityTest
    void
            givenValidRequest_whenGetStandardApplicantWithPagingNameFilterPartialForNameOfIndividual_thenReturn200()
                    throws Exception {

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute functionality
        int pageSize = 3;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of("name"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new StandardApplicantRequestFilter(
                                Optional.empty(),
                                Optional.of("D"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);
        StandardApplicantPage response = responseSpec.as(StandardApplicantPage.class);
        PagingAssertionUtil.assertPageDetails(response, pageSize, pageNumber, 1, 3);

        Assertions.assertEquals(
                "Alex",
                response.getContent().get(0).getApplicant().getPerson().getName().getFirstName());
        Assertions.assertEquals(
                "Dunn",
                response.getContent().get(0).getApplicant().getPerson().getName().getLastName());
        Assertions.assertEquals(
                "Alex",
                response.getContent().get(1).getApplicant().getPerson().getName().getFirstName());
        Assertions.assertEquals(
                "Dunn",
                response.getContent().get(1).getApplicant().getPerson().getName().getLastName());
        Assertions.assertEquals(
                "Jane",
                response.getContent().get(2).getApplicant().getPerson().getName().getFirstName());
        Assertions.assertEquals(
                "Doe",
                response.getContent().get(2).getApplicant().getPerson().getName().getLastName());

        // audit assertion
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "standard_applicant_code",
                        null,
                        "",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "name",
                        null,
                        "D",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));
    }

    @Test
    @StabilityTest
    void
            givenValidRequest_whenGetStandardApplicantWithPagingNameFilterPartialForSurNameOfIndividual_thenReturn200()
                    throws Exception {

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute functionality
        int pageSize = 3;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of("name"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new StandardApplicantRequestFilter(
                                Optional.empty(),
                                Optional.of("Dunn"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);
        StandardApplicantPage response = responseSpec.as(StandardApplicantPage.class);
        PagingAssertionUtil.assertPageDetails(response, pageSize, pageNumber, 1, 2);

        Assertions.assertEquals(
                "Alex",
                response.getContent().get(0).getApplicant().getPerson().getName().getFirstName());
        Assertions.assertEquals(
                "Dunn",
                response.getContent().get(0).getApplicant().getPerson().getName().getLastName());
        Assertions.assertEquals(
                "Alex",
                response.getContent().get(0).getApplicant().getPerson().getName().getFirstName());
        Assertions.assertEquals(
                "Dunn",
                response.getContent().get(0).getApplicant().getPerson().getName().getLastName());

        // audit assertion
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "standard_applicant_code",
                        null,
                        "",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "name",
                        null,
                        "Dunn",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));
    }

    @Test
    @StabilityTest
    void givenValidRequest_whenGetStandardApplicantWithPagingAllFilter_thenReturn200()
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
                        List.of("name"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new StandardApplicantRequestFilter(
                                Optional.of("APP001"),
                                Optional.of("Smith"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);
        StandardApplicantPage page = responseSpec.as(StandardApplicantPage.class);
        PagingAssertionUtil.assertPageDetails(page, pageSize, pageNumber, 1, 1);
        StandardApplicantGetSummaryDto firstEntry = page.getContent().get(0);
        assertEquals("APP001", firstEntry.getCode());
        assertEquals("John", firstEntry.getApplicant().getPerson().getName().getFirstName());
        assertEquals("Smith", firstEntry.getApplicant().getPerson().getName().getLastName());

        // audit assertion
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "standard_applicant_code",
                        null,
                        "APP001",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "name",
                        null,
                        "Smith",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));
    }

    @Test
    void givenValidRequest_whenGetStandardApplicantWithFullNameFilterForIndividual_thenReturn200()
            throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        int pageSize = 1;
        int pageNumber = 0;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of("name"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new StandardApplicantRequestFilter(
                                Optional.of("APP001"),
                                Optional.of("John Smith"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);
        StandardApplicantPage page = responseSpec.as(StandardApplicantPage.class);
        PagingAssertionUtil.assertPageDetails(page, pageSize, pageNumber, 1, 1);

        StandardApplicantGetSummaryDto firstEntry = page.getContent().get(0);
        assertEquals("APP001", firstEntry.getCode());
        assertEquals("John", firstEntry.getApplicant().getPerson().getName().getFirstName());
        assertEquals("Smith", firstEntry.getApplicant().getPerson().getName().getLastName());

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "standard_applicant_code",
                        null,
                        "APP001",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "name",
                        null,
                        "John Smith",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));
    }

    @Test
    void givenValidRequest_whenGetStandardApplicantWithNameFilterForSecondForename_thenReturn200()
            throws Exception {
        String code = "SAFN2001";
        savePersonStandardApplicant(
                code,
                "Amelia",
                "Rosemarie",
                null,
                "Walker",
                LocalDate.now(java.time.ZoneOffset.UTC).minusDays(1),
                null);

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of("name"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new StandardApplicantRequestFilter(
                                Optional.empty(),
                                Optional.of("rosemarie"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);

        StandardApplicantPage page = responseSpec.as(StandardApplicantPage.class);
        Assertions.assertNotNull(page.getContent());
        Assertions.assertTrue(
                page.getContent().stream().anyMatch(item -> code.equals(item.getCode())));
    }

    @Test
    @StabilityTest
    void
            givenValidRequest_whenGetStandardApplicantWithPageNumberBeyondResultBoundary_thenReturn200()
                    throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // execute the functionality
        int pageSize = 6;
        int pageNumber = 200;
        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(pageSize),
                        Optional.of(pageNumber),
                        List.of("name"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new StandardApplicantRequestFilter(
                                Optional.of("APP001"),
                                Optional.of("John"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);
        StandardApplicantPage page = responseSpec.as(StandardApplicantPage.class);
        PagingAssertionUtil.assertPageDetails(page, pageSize, pageNumber, 1, 1);
        assertThat(page.getContent()).isEmpty();

        // audit assertion
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "standard_applicant_code",
                        null,
                        "APP001",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "name",
                        null,
                        "John",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));
    }

    @StabilityTest
    void givenSASuccessfulSort_whenSearchWithAllSortKeys_thenSuccessResponse() throws Exception {
        for (StandardApplicantSortFieldEnum standardApplicantSortFieldEnum :
                StandardApplicantSortFieldEnum.values()) {

            // create the token
            TokenGenerator tokenGenerator =
                    getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

            // test the functionality
            Response responseSpec =
                    restAssuredClient.executeGetRequestWithPaging(
                            Optional.of(10),
                            Optional.of(0),
                            List.of(standardApplicantSortFieldEnum.getApiValue() + "," + "desc"),
                            getLocalUrl(WEB_CONTEXT),
                            tokenGenerator.fetchTokenForRole());

            StandardApplicantPage page = responseSpec.as(StandardApplicantPage.class);

            // make sure the order response marries with the request data
            responseSpec.then().statusCode(200);
            Assertions.assertEquals(1, page.getSort().getOrders().size());
            Assertions.assertEquals(
                    SortOrdersInner.DirectionEnum.DESC,
                    page.getSort().getOrders().get(0).getDirection());
            Assertions.assertEquals(
                    standardApplicantSortFieldEnum.getApiValue(),
                    page.getSort().getOrders().get(0).getProperty());

            // audit assertion
            differenceLogAsserter.assertDataAuditChange(
                    DataAuditLogAsserter.getDataAuditAssertion(
                            TableNames.STANDARD_APPLICANTS,
                            "standard_applicant_code",
                            null,
                            "",
                            StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                            StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));

            differenceLogAsserter.assertDataAuditChange(
                    DataAuditLogAsserter.getDataAuditAssertion(
                            TableNames.STANDARD_APPLICANTS,
                            "name",
                            null,
                            "",
                            StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                            StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));
        }

        Assertions.assertTrue(StandardApplicantSortFieldEnum.values().length > 0);
    }

    @Test
    void givenValidRequest_whenGetStandardApplicantWithPagingInvalidSortQuery_thenReturn400()
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
                        new StandardApplicantRequestFilter(
                                Optional.of("AP99004"),
                                Optional.of("John, Smith"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()),
                        new OpenApiPageMetaData());
        // assert the response
        responseSpec.then().statusCode(400);
    }

    // NOTE: Spring is more forgiving in this scenario and defaults the page number to
    // 0 and returns a 200. Our implementation
    // returns a 500
    @Test
    void givenValidRequest_whenGetStandardApplicantWithPagingInvalidPageNumber_thenReturn400()
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
                        new StandardApplicantRequestFilter(
                                Optional.of("AP99004"),
                                Optional.of("John"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()),
                        new OpenApiPageMetaData());
        // assert the response
        responseSpec.then().statusCode(400);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(
                CommonAppError.CONSTRAINT_ERROR.getCode().getType().get(), problemDetail.getType());
    }

    // NOTE: Spring defaults the page size to the max size if we try and increase it beyond. This
    // does not behave
    // accordingly
    @Test
    void
            givenValidRequest_whenGetStandardApplicantWithPagingInvalidPageSizeBeyondDefault_thenReturn400()
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
                        new StandardApplicantRequestFilter(
                                Optional.of("AP99004"),
                                Optional.of("John"),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()),
                        new OpenApiPageMetaData());

        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);

        // assert the response
        responseSpec.then().statusCode(400);
        Assertions.assertEquals(
                CommonAppError.CONSTRAINT_ERROR.getCode().getType().get(), problemDetail.getType());
    }

    @StabilityTest
    void givenASuccessfulFilterPartialCode_whenSearch_thenSuccessResponse() throws Exception {
        for (StandardApplicantSortFieldEnum standardApplicantSortFieldEnum :
                StandardApplicantSortFieldEnum.values()) {

            // create the token
            TokenGenerator tokenGenerator =
                    getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

            // test the functionality
            Response responseSpec =
                    restAssuredClient.executeGetRequestWithPaging(
                            Optional.of(10),
                            Optional.of(0),
                            List.of(),
                            getLocalUrl(WEB_CONTEXT),
                            tokenGenerator.fetchTokenForRole(),
                            new StandardApplicantRequestFilter(
                                    Optional.of("P0"),
                                    Optional.empty(),
                                    Optional.empty(),
                                    Optional.empty(),
                                    Optional.empty()),
                            new OpenApiPageMetaData());

            StandardApplicantPage page = responseSpec.as(StandardApplicantPage.class);

            // make sure the order response marries with the request data
            responseSpec.then().statusCode(200);
            Assertions.assertEquals(7, page.getContent().size());
            Assertions.assertEquals("APP001", page.getContent().get(0).getCode());
            Assertions.assertEquals("APP002", page.getContent().get(1).getCode());

            // we have a duplicate record
            Assertions.assertEquals("APP003", page.getContent().get(2).getCode());
            Assertions.assertEquals("APP003", page.getContent().get(3).getCode());

            Assertions.assertEquals("APP004", page.getContent().get(4).getCode());
            Assertions.assertEquals("APP005", page.getContent().get(5).getCode());

            // audit assertion
            differenceLogAsserter.assertDataAuditChange(
                    DataAuditLogAsserter.getDataAuditAssertion(
                            TableNames.STANDARD_APPLICANTS,
                            "standard_applicant_code",
                            null,
                            "P0",
                            StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                            StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));

            differenceLogAsserter.assertDataAuditChange(
                    DataAuditLogAsserter.getDataAuditAssertion(
                            TableNames.STANDARD_APPLICANTS,
                            "name",
                            null,
                            "",
                            StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                            StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));
        }

        Assertions.assertTrue(StandardApplicantSortFieldEnum.values().length > 0);
    }

    @StabilityTest
    void givenASuccessfulFilterPartialName_whenSearch_thenSuccessResponse() throws Exception {
        for (StandardApplicantSortFieldEnum standardApplicantSortFieldEnum :
                StandardApplicantSortFieldEnum.values()) {

            // create the token
            TokenGenerator tokenGenerator =
                    getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

            // test the functionality
            Response responseSpec =
                    restAssuredClient.executeGetRequestWithPaging(
                            Optional.of(10),
                            Optional.of(0),
                            List.of(),
                            getLocalUrl(WEB_CONTEXT),
                            tokenGenerator.fetchTokenForRole(),
                            new StandardApplicantRequestFilter(
                                    Optional.empty(),
                                    Optional.of("anisation 1"),
                                    Optional.empty(),
                                    Optional.empty(),
                                    Optional.empty()),
                            new OpenApiPageMetaData());

            StandardApplicantPage page = responseSpec.as(StandardApplicantPage.class);

            // make sure the order response marries with the request data
            responseSpec.then().statusCode(200);
            Assertions.assertEquals(1, page.getContent().size());
            Assertions.assertEquals("APP005", page.getContent().get(0).getCode());

            // audit assertion
            differenceLogAsserter.assertDataAuditChange(
                    DataAuditLogAsserter.getDataAuditAssertion(
                            TableNames.STANDARD_APPLICANTS,
                            "standard_applicant_code",
                            null,
                            "",
                            StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                            StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));

            differenceLogAsserter.assertDataAuditChange(
                    DataAuditLogAsserter.getDataAuditAssertion(
                            TableNames.STANDARD_APPLICANTS,
                            "name",
                            null,
                            "anisation 1",
                            StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                            StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));
        }
    }

    @StabilityTest
    void givenASuccessfulFilterPartialForename_whenSearch_thenSuccessResponse() throws Exception {
        for (StandardApplicantSortFieldEnum standardApplicantSortFieldEnum :
                StandardApplicantSortFieldEnum.values()) {

            // create the token
            TokenGenerator tokenGenerator =
                    getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

            // test the functionality
            Response responseSpec =
                    restAssuredClient.executeGetRequestWithPaging(
                            Optional.of(10),
                            Optional.of(0),
                            List.of(),
                            getLocalUrl(WEB_CONTEXT),
                            tokenGenerator.fetchTokenForRole(),
                            new StandardApplicantRequestFilter(
                                    Optional.empty(),
                                    Optional.of("Owe"),
                                    Optional.empty(),
                                    Optional.empty(),
                                    Optional.empty()),
                            new OpenApiPageMetaData());

            StandardApplicantPage page = responseSpec.as(StandardApplicantPage.class);

            // make sure the order response marries with the request data
            responseSpec.then().statusCode(200);
            Assertions.assertEquals(2, page.getContent().size());
            Assertions.assertEquals("APP005", page.getContent().get(0).getCode());
            Assertions.assertEquals("APP006", page.getContent().get(1).getCode());

            // audit assertion
            differenceLogAsserter.assertDataAuditChange(
                    DataAuditLogAsserter.getDataAuditAssertion(
                            TableNames.STANDARD_APPLICANTS,
                            "standard_applicant_code",
                            null,
                            "",
                            StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                            StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));

            differenceLogAsserter.assertDataAuditChange(
                    DataAuditLogAsserter.getDataAuditAssertion(
                            TableNames.STANDARD_APPLICANTS,
                            "name",
                            null,
                            "Owe",
                            StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                            StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));
        }
    }

    @StabilityTest
    void givenASuccessfulFilterPartialSurname_whenSearch_thenSuccessResponse() throws Exception {
        for (StandardApplicantSortFieldEnum standardApplicantSortFieldEnum :
                StandardApplicantSortFieldEnum.values()) {

            // create the token
            TokenGenerator tokenGenerator =
                    getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

            // test the functionality
            Response responseSpec =
                    restAssuredClient.executeGetRequestWithPaging(
                            Optional.of(10),
                            Optional.of(0),
                            List.of(),
                            getLocalUrl(WEB_CONTEXT),
                            tokenGenerator.fetchTokenForRole(),
                            new StandardApplicantRequestFilter(
                                    Optional.empty(),
                                    Optional.of("Jones"),
                                    Optional.empty(),
                                    Optional.empty(),
                                    Optional.empty()),
                            new OpenApiPageMetaData());

            StandardApplicantPage page = responseSpec.as(StandardApplicantPage.class);

            // make sure the order response marries with the request data
            responseSpec.then().statusCode(200);
            Assertions.assertEquals(2, page.getContent().size());
            Assertions.assertEquals("APP004", page.getContent().get(0).getCode());
            Assertions.assertEquals("APP005", page.getContent().get(1).getCode());

            // audit assertion
            differenceLogAsserter.assertDataAuditChange(
                    DataAuditLogAsserter.getDataAuditAssertion(
                            TableNames.STANDARD_APPLICANTS,
                            "standard_applicant_code",
                            null,
                            "",
                            StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                            StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));

            differenceLogAsserter.assertDataAuditChange(
                    DataAuditLogAsserter.getDataAuditAssertion(
                            TableNames.STANDARD_APPLICANTS,
                            "name",
                            null,
                            "Jones",
                            StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                            StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));
        }
    }

    @Test
    void givenValidRequest_whenMultipleSortsArePresent_thenReturn400() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(1),
                        Optional.of(0),
                        List.of(
                                StandardApplicantSortFieldEnum.CODE.getApiValue(),
                                StandardApplicantSortFieldEnum.NAME.getApiValue()),
                        getLocalUrl(WEB_CONTEXT),
                        token);

        // assert the response
        responseSpec.then().statusCode(400);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(
                CommonAppError.MULTIPLE_SORT_NOT_SUPPORTED.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    void
            givenValidRequest_whenFilterByAddressLine1AndFromDateAndSortByName_thenReturnSortedResults()
                    throws Exception {
        val tokenGenerator = getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        val responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of("name,asc"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new StandardApplicantRequestFilter(
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of("123 High Street"),
                                Optional.of(
                                        LocalDate.of(2026, Month.APRIL, 1)), // matches seeded data
                                Optional.empty()),
                        new OpenApiPageMetaData());

        responseSpec.then().statusCode(200);

        val page = responseSpec.as(StandardApplicantPage.class);
        assertThat(page.getContent()).isNotEmpty();

        // verify sorting by name (ascending)
        val first = page.getContent().get(0);
        val second = page.getContent().get(1);

        val firstName =
                first.getApplicant().getOrganisation() != null
                        ? first.getApplicant().getOrganisation().getName()
                        : first.getApplicant().getPerson().getName().getFirstName();

        val secondName =
                second.getApplicant().getOrganisation() != null
                        ? second.getApplicant().getOrganisation().getName()
                        : second.getApplicant().getPerson().getName().getFirstName();

        Assertions.assertEquals("John", firstName);
        Assertions.assertEquals("Organisation 1", secondName);

        // verify filter applied
        page.getContent()
                .forEach(
                        item -> {
                            val address =
                                    item.getApplicant().getOrganisation() != null
                                            ? item.getApplicant()
                                                    .getOrganisation()
                                                    .getContactDetails()
                                                    .getAddressLine1()
                                            : item.getApplicant()
                                                    .getPerson()
                                                    .getContactDetails()
                                                    .getAddressLine1();

                            Assertions.assertEquals("123 High Street", address);
                        });

        // The GET audit should capture each DB-backed filter value that was sent on the request.
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "address_l1",
                        null,
                        "123 High Street",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.STANDARD_APPLICANTS,
                        "standard_applicant_start_date",
                        null,
                        "2026-04-01",
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getType().name(),
                        StandardApplicantOperation.GET_STANDARD_APPLICANTS.getEventName()));
    }

    @Test
    void givenValidRequest_whenSortByAddressLine1_thenReturnSortedResults() throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of("addressLine1,asc"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);

        StandardApplicantPage page = responseSpec.as(StandardApplicantPage.class);
        assertThat(page.getContent()).isNotEmpty();

        String firstAddress = extractAddress(page.getContent().get(0));
        String secondAddress = extractAddress(page.getContent().get(1));
        String thirdAddress = extractAddress(page.getContent().get(2));
        String fourthAddress = extractAddress(page.getContent().get(3));

        Assertions.assertEquals("123 High Street", firstAddress);
        Assertions.assertEquals("123 High Street", secondAddress);
        Assertions.assertEquals("123 High Street", thirdAddress);
        Assertions.assertEquals("456 Elm Road", fourthAddress);
    }

    @Test
    void givenValidRequest_whenSortByFrom_thenReturnSortedResults() throws Exception {

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(10),
                        Optional.of(0),
                        List.of("from,asc"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);

        StandardApplicantPage page = responseSpec.as(StandardApplicantPage.class);
        assertThat(page.getContent()).isNotEmpty();

        List<LocalDate> dates =
                page.getContent().stream()
                        .map(StandardApplicantGetSummaryDto::getStartDate)
                        .toList();

        List<LocalDate> sortedDates = dates.stream().sorted().toList();

        Assertions.assertEquals(sortedDates, dates);
    }

    @RequiredArgsConstructor
    static class StandardApplicantRequestFilter implements UnaryOperator<RequestSpecification> {
        private final Optional<String> code;
        private final Optional<String> name;
        private final Optional<String> addressLine1;
        private final Optional<LocalDate> from;
        private final Optional<LocalDate> to;

        @Override
        public RequestSpecification apply(RequestSpecification rs) {
            if (code.isPresent()) {
                rs = rs.queryParam("code", code.get());
            }

            if (name.isPresent()) {
                rs = rs.queryParam("name", name.get());
            }

            if (addressLine1.isPresent()) {
                rs = rs.queryParam("addressLine1", addressLine1.get());
            }

            if (from.isPresent()) {
                rs = rs.queryParam("from", from.get().toString());
            }

            if (to.isPresent()) {
                rs = rs.queryParam("to", to.get().toString());
            }

            return rs;
        }
    }

    private void saveStandardApplicant(
            String code, String name, LocalDate startDate, LocalDate endDate) throws Exception {
        var jwt = TokenGenerator.builder().build().getJwtFromToken();
        var auth = new JwtAuthenticationToken(jwt, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            StandardApplicant standardApplicant = new StandardApplicantTestData().someComplete();
            standardApplicant.setApplicantCode(code);
            standardApplicant.setName(name);
            standardApplicant.setApplicantTitle(null);
            standardApplicant.setApplicantForename1(null);
            standardApplicant.setApplicantForename2(null);
            standardApplicant.setApplicantForename3(null);
            standardApplicant.setApplicantSurname(null);
            standardApplicant.setApplicantStartDate(startDate);
            standardApplicant.setApplicantEndDate(endDate);
            persistance.save(standardApplicant);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void savePersonStandardApplicant(
            String code,
            String forename1,
            String forename2,
            String forename3,
            String surname,
            LocalDate startDate,
            LocalDate endDate)
            throws Exception {
        var jwt = TokenGenerator.builder().build().getJwtFromToken();
        var auth = new JwtAuthenticationToken(jwt, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            StandardApplicant standardApplicant = new StandardApplicantTestData().someComplete();
            standardApplicant.setApplicantCode(code);
            standardApplicant.setName(null);
            standardApplicant.setApplicantTitle("Ms");
            standardApplicant.setApplicantForename1(forename1);
            standardApplicant.setApplicantForename2(forename2);
            standardApplicant.setApplicantForename3(forename3);
            standardApplicant.setApplicantSurname(surname);
            standardApplicant.setApplicantStartDate(startDate);
            standardApplicant.setApplicantEndDate(endDate);
            persistance.save(standardApplicant);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void saveSparsePersonStandardApplicant(
            String code, LocalDate startDate, LocalDate endDate) throws Exception {
        var jwt = TokenGenerator.builder().build().getJwtFromToken();
        var auth = new JwtAuthenticationToken(jwt, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            StandardApplicant standardApplicant = new StandardApplicantTestData().someComplete();
            standardApplicant.setApplicantCode(code);
            standardApplicant.setName(null);
            standardApplicant.setApplicantTitle("Mx");
            standardApplicant.setApplicantForename1("Sparse");
            standardApplicant.setApplicantForename2(null);
            standardApplicant.setApplicantForename3(null);
            standardApplicant.setApplicantSurname("Person");
            standardApplicant.setApplicantStartDate(startDate);
            standardApplicant.setApplicantEndDate(endDate);
            standardApplicant.setAddressLine1("1 Sparse Street");
            standardApplicant.setAddressLine2(null);
            standardApplicant.setAddressLine3(null);
            standardApplicant.setAddressLine4(null);
            standardApplicant.setAddressLine5(null);
            standardApplicant.setPostcode("SP1 1AA");
            standardApplicant.setTelephoneNumber(null);
            standardApplicant.setMobileNumber(null);
            standardApplicant.setEmailAddress(null);
            persistance.save(standardApplicant);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void assertExplicitNull(JsonNode root, String dottedPath) {
        JsonNode current = root;
        for (String segment : dottedPath.split("\\.")) {
            assertTrue(current.has(segment), "Expected JSON field to be present: " + dottedPath);
            current = current.get(segment);
        }
        assertTrue(current.isNull(), "Expected JSON field to be explicit null: " + dottedPath);
    }

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT + "/" + APPCODE_CODE))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }

    private String extractAddress(StandardApplicantGetSummaryDto dto) {
        return dto.getApplicant().getOrganisation() != null
                ? dto.getApplicant().getOrganisation().getContactDetails().getAddressLine1()
                : dto.getApplicant().getPerson().getContactDetails().getAddressLine1();
    }
}
