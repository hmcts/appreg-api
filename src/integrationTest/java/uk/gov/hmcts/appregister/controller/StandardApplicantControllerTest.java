package uk.gov.hmcts.appregister.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.SortOrdersInner;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPage;
import uk.gov.hmcts.appregister.standardapplicant.api.StandardApplicantSortFieldEnum;
import uk.gov.hmcts.appregister.standardapplicant.exception.StandardApplicantCodeError;
import uk.gov.hmcts.appregister.testutils.annotation.StabilityTest;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.client.request.DateGetRequest;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.PagingAssertionUtil;

public class StandardApplicantControllerTest extends AbstractSecurityControllerTest {
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
    public void before() {
        when(clock.instant()).thenReturn(Instant.now().plus(2, ChronoUnit.DAYS));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.withZone(org.mockito.ArgumentMatchers.any(ZoneId.class))).thenReturn(clock);
    }

    @Test
    public void givenValidRequest_whenGetStandardApplicantByCodeAndDateForIndividual_thenReturn200()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + APPCODE_CODE),
                        tokenGenerator.fetchTokenForRole(),
                        new DateGetRequest(LocalDate.now()));

        // assert the response
        responseSpec.then().statusCode(200);

        StandardApplicantGetDetailDto returnedSa =
                responseSpec.as(StandardApplicantGetDetailDto.class);

        // assert the data
        Assertions.assertEquals("APP001", returnedSa.getCode());
        Assertions.assertEquals(LocalDate.now(), returnedSa.getStartDate());
        Assertions.assertFalse(returnedSa.getEndDate().isPresent());
        Assertions.assertNotNull(returnedSa.getApplicant().getPerson().getName());
        Assertions.assertEquals("Mr", returnedSa.getApplicant().getPerson().getName().getTitle());
        Assertions.assertEquals(
                "John", returnedSa.getApplicant().getPerson().getName().getFirstForename());
        Assertions.assertNull(returnedSa.getApplicant().getPerson().getName().getSecondForename());
        Assertions.assertNull(returnedSa.getApplicant().getPerson().getName().getThirdForename());
        Assertions.assertEquals(
                "Smith", returnedSa.getApplicant().getPerson().getName().getSurname());
        Assertions.assertNull(returnedSa.getApplicant().getPerson().getName().getThirdForename());
        Assertions.assertEquals(
                "123 High Street",
                returnedSa.getApplicant().getPerson().getContactDetails().getAddressLine1());
        Assertions.assertNull(
                returnedSa.getApplicant().getPerson().getContactDetails().getAddressLine2());
        Assertions.assertNull(
                returnedSa.getApplicant().getPerson().getContactDetails().getAddressLine3());
        Assertions.assertEquals(
                "Townsville",
                returnedSa.getApplicant().getPerson().getContactDetails().getAddressLine4());
        Assertions.assertNull(
                returnedSa.getApplicant().getPerson().getContactDetails().getAddressLine5());
        Assertions.assertEquals(
                "john.smith@example.com",
                returnedSa.getApplicant().getPerson().getContactDetails().getEmail());
        Assertions.assertEquals(
                "07123456789",
                returnedSa.getApplicant().getPerson().getContactDetails().getMobile());
        Assertions.assertEquals(
                "01234567890",
                returnedSa.getApplicant().getPerson().getContactDetails().getPhone());
        Assertions.assertEquals(
                "TS1 1AB", returnedSa.getApplicant().getPerson().getContactDetails().getPostcode());
    }

    @Test
    public void
            givenValidRequest_whenGetStandardApplicantByCodeAndDateForOrganisation_thenReturn200()
                    throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + APPCODE_CODE_ORGANISATION),
                        tokenGenerator.fetchTokenForRole(),
                        new DateGetRequest(LocalDate.now()));

        // assert the response
        responseSpec.then().statusCode(200);

        StandardApplicantGetDetailDto returnedSa =
                responseSpec.as(StandardApplicantGetDetailDto.class);

        // assert the data
        Assertions.assertEquals(APPCODE_CODE_ORGANISATION, returnedSa.getCode());
        Assertions.assertEquals(LocalDate.now().minusDays(1), returnedSa.getStartDate());
        Assertions.assertFalse(returnedSa.getEndDate().isPresent());
        Assertions.assertEquals(
                "Organisation 1", returnedSa.getApplicant().getOrganisation().getName());
        Assertions.assertEquals(
                "123 High Street",
                returnedSa.getApplicant().getOrganisation().getContactDetails().getAddressLine1());
        Assertions.assertNull(
                returnedSa.getApplicant().getOrganisation().getContactDetails().getAddressLine2());
        Assertions.assertNull(
                returnedSa.getApplicant().getOrganisation().getContactDetails().getAddressLine3());
        Assertions.assertEquals(
                "Townsville",
                returnedSa.getApplicant().getOrganisation().getContactDetails().getAddressLine4());
        Assertions.assertNull(
                returnedSa.getApplicant().getOrganisation().getContactDetails().getAddressLine5());
        Assertions.assertEquals(
                "john.smith@example.com",
                returnedSa.getApplicant().getOrganisation().getContactDetails().getEmail());
        Assertions.assertEquals(
                "07123456789",
                returnedSa.getApplicant().getOrganisation().getContactDetails().getMobile());
        Assertions.assertEquals(
                "01234567890",
                returnedSa.getApplicant().getOrganisation().getContactDetails().getPhone());
        Assertions.assertEquals(
                "TS1 1AB",
                returnedSa.getApplicant().getOrganisation().getContactDetails().getPostcode());
    }

    @Test
    public void givenValidRequest_whenGetStandardApplicantByCodeAndCodeNotExist_thenReturn404()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + "APP003NotExist"),
                        tokenGenerator.fetchTokenForRole(),
                        new DateGetRequest(LocalDate.now()));

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
    public void
            givenValidRequest_whenGetStandardApplicantByCodeAndDateNotWithinRange_thenReturn404()
                    throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + DUPLICATE_APPCODE_CODE),
                        tokenGenerator.fetchTokenForRole(),
                        new DateGetRequest(LocalDate.now().minusDays(1)));

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
    public void givenValidRequest_whenGetStandardApplicantByCodeAndDateMultiple_thenReturn409()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + DUPLICATE_APPCODE_CODE),
                        tokenGenerator.fetchTokenForRole(),
                        new DateGetRequest(LocalDate.now()));

        // assert the response
        ProblemDetail returnedSc = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(
                StandardApplicantCodeError.DUPLICATE_RESULT_CODE_FOUND.getCode().getAppCode(),
                returnedSc.getType().toString());
        Assertions.assertEquals(
                StandardApplicantCodeError.DUPLICATE_RESULT_CODE_FOUND
                        .getCode()
                        .getHttpCode()
                        .value(),
                responseSpec.getStatusCode());
    }

    @Test
    @StabilityTest
    public void givenValidRequest_whenGetAllStandardApplicant_thenReturn200() throws Exception {
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
    }

    @StabilityTest
    @Test
    public void
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
        assertEquals("John", firstEntry.getApplicant().getPerson().getName().getFirstForename());
        assertEquals("Smith", firstEntry.getApplicant().getPerson().getName().getSurname());
        assertEquals(
                "123 High Street",
                firstEntry.getApplicant().getPerson().getContactDetails().getAddressLine1());
        assertNotNull(firstEntry.getStartDate());
        assertFalse(firstEntry.getEndDate().isPresent());

        StandardApplicantGetSummaryDto secondEntry = response.getContent().get(1);
        assertEquals("APP002", secondEntry.getCode());
        assertEquals("Jane", secondEntry.getApplicant().getPerson().getName().getFirstForename());
        assertEquals("Doe", secondEntry.getApplicant().getPerson().getName().getSurname());
        assertEquals(
                "456 Elm Road",
                secondEntry.getApplicant().getPerson().getContactDetails().getAddressLine1());
        assertNotNull(secondEntry.getStartDate());
        assertFalse(secondEntry.getEndDate().isPresent());

        StandardApplicantGetSummaryDto org = response.getContent().get(6);
        assertEquals("APP006", org.getCode());
        assertEquals("Organisation 3", org.getApplicant().getOrganisation().getName());
        assertEquals(
                "456 Elm Road",
                org.getApplicant().getOrganisation().getContactDetails().getAddressLine1());
        assertEquals(
                "Apt 5",
                org.getApplicant().getOrganisation().getContactDetails().getAddressLine2());
        assertEquals(
                "Cityville",
                org.getApplicant().getOrganisation().getContactDetails().getAddressLine4());
        assertNotNull(secondEntry.getStartDate());
        assertFalse(secondEntry.getEndDate().isPresent());
    }

    @Test
    @StabilityTest
    public void
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
        assertFalse(firstEntry.getEndDate().isPresent());

        StandardApplicantGetSummaryDto secondEntry = response.getContent().get(1);
        assertEquals("APP004", secondEntry.getCode());
        assertEquals("Organisation 2", secondEntry.getApplicant().getOrganisation().getName());
        assertEquals(
                "123 High Street",
                secondEntry.getApplicant().getOrganisation().getContactDetails().getAddressLine1());
        assertNotNull(secondEntry.getStartDate());
        assertFalse(secondEntry.getEndDate().isPresent());
    }

    @Test
    @StabilityTest
    public void givenValidRequest_whenGetStandardApplicantWithPagingNoResult_thenReturn200()
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
                                Optional.of("does not exist"), Optional.of("does not exist")),
                        new OpenApiPageMetaData());

        // assert the response is successful with no content
        responseSpec.then().statusCode(200);
        StandardApplicantPage response = responseSpec.as(StandardApplicantPage.class);
        PagingAssertionUtil.assertPageDetails(response, pageSize, pageNumber, 0, 0);
    }

    @Test
    @StabilityTest
    public void
            givenValidRequest_whenGetStandardApplicantWithPagingNoResultDateRange_thenReturn200()
                    throws Exception {

        Mockito.reset(clock);

        when(clock.instant()).thenReturn(Instant.now().minus(1, ChronoUnit.DAYS));
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
    }

    @Test
    @StabilityTest
    public void
            givenValidRequest_whenGetStandardApplicantWithPagingFilterPartialCode_thenReturn200()
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
                        new StandardApplicantRequestFilter(Optional.of("APP00"), Optional.empty()),
                        new OpenApiPageMetaData());

        // assert the response
        Assertions.assertEquals(200, responseSpec.getStatusCode());
        StandardApplicantPage response = responseSpec.as(StandardApplicantPage.class);
        PagingAssertionUtil.assertPageDetails(
                response, pageSize, pageNumber, 4, TOTAL_STANDARD_APPLICANT_COUNT);
    }

    @Test
    @StabilityTest
    public void
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
                        new StandardApplicantRequestFilter(Optional.empty(), Optional.of("ORG")),
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
    }

    @Test
    @StabilityTest
    public void
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
                        new StandardApplicantRequestFilter(Optional.empty(), Optional.of("D")),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);
        StandardApplicantPage response = responseSpec.as(StandardApplicantPage.class);
        PagingAssertionUtil.assertPageDetails(response, pageSize, pageNumber, 1, 3);

        Assertions.assertEquals(
                "Alex",
                response.getContent()
                        .get(0)
                        .getApplicant()
                        .getPerson()
                        .getName()
                        .getFirstForename());
        Assertions.assertEquals(
                "Dunn",
                response.getContent().get(0).getApplicant().getPerson().getName().getSurname());
        Assertions.assertEquals(
                "Alex",
                response.getContent()
                        .get(1)
                        .getApplicant()
                        .getPerson()
                        .getName()
                        .getFirstForename());
        Assertions.assertEquals(
                "Dunn",
                response.getContent().get(1).getApplicant().getPerson().getName().getSurname());
        Assertions.assertEquals(
                "Jane",
                response.getContent()
                        .get(2)
                        .getApplicant()
                        .getPerson()
                        .getName()
                        .getFirstForename());
        Assertions.assertEquals(
                "Doe",
                response.getContent().get(2).getApplicant().getPerson().getName().getSurname());
    }

    @Test
    @StabilityTest
    public void
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
                        new StandardApplicantRequestFilter(Optional.empty(), Optional.of("Dunn")),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);
        StandardApplicantPage response = responseSpec.as(StandardApplicantPage.class);
        PagingAssertionUtil.assertPageDetails(response, pageSize, pageNumber, 1, 2);

        Assertions.assertEquals(
                "Alex",
                response.getContent()
                        .get(0)
                        .getApplicant()
                        .getPerson()
                        .getName()
                        .getFirstForename());
        Assertions.assertEquals(
                "Dunn",
                response.getContent().get(0).getApplicant().getPerson().getName().getSurname());
        Assertions.assertEquals(
                "Alex",
                response.getContent()
                        .get(0)
                        .getApplicant()
                        .getPerson()
                        .getName()
                        .getFirstForename());
        Assertions.assertEquals(
                "Dunn",
                response.getContent().get(0).getApplicant().getPerson().getName().getSurname());
    }

    @Test
    @StabilityTest
    public void givenValidRequest_whenGetStandardApplicantWithPagingAllFilter_thenReturn200()
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
                                Optional.of("APP001"), Optional.of("Smith")),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);
        StandardApplicantPage page = responseSpec.as(StandardApplicantPage.class);
        PagingAssertionUtil.assertPageDetails(page, pageSize, pageNumber, 1, 1);
        StandardApplicantGetSummaryDto firstEntry = page.getContent().get(0);
        assertEquals("APP001", firstEntry.getCode());
        assertEquals("John", firstEntry.getApplicant().getPerson().getName().getFirstForename());
        assertEquals("Smith", firstEntry.getApplicant().getPerson().getName().getSurname());
    }

    @Test
    @StabilityTest
    public void
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
                                Optional.of("APP001"), Optional.of("John")),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);
        StandardApplicantPage page = responseSpec.as(StandardApplicantPage.class);
        PagingAssertionUtil.assertPageDetails(page, pageSize, pageNumber, 1, 1);
        Assertions.assertNull(page.getContent());
    }

    @StabilityTest
    public void givenSASuccessfulSort_whenSearchWithAllSortKeys_thenSuccessResponse()
            throws Exception {
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
        }

        Assertions.assertTrue(StandardApplicantSortFieldEnum.values().length > 0);
    }

    @Test
    public void givenValidRequest_whenGetStandardApplicantWithPagingInvalidSortQuery_thenReturn400()
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
                                Optional.of("AP99004"), Optional.of("John, Smith")),
                        new OpenApiPageMetaData());
        // assert the response
        responseSpec.then().statusCode(400);
    }

    // NOTE: Spring is more forgiving in this scenario and defaults the page number to
    // 0 and returns a 200. Our implementation
    // returns a 500
    @Test
    public void
            givenValidRequest_whenGetStandardApplicantWithPagingInvalidPageNumber_thenReturn400()
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
                                Optional.of("AP99004"), Optional.of("John")),
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
    public void
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
                                Optional.of("AP99004"), Optional.of("John")),
                        new OpenApiPageMetaData());

        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);

        // assert the response
        responseSpec.then().statusCode(400);
        Assertions.assertEquals(
                CommonAppError.CONSTRAINT_ERROR.getCode().getType().get(), problemDetail.getType());
    }

    @RequiredArgsConstructor
    static class StandardApplicantRequestFilter implements UnaryOperator<RequestSpecification> {
        private final Optional<String> code;
        private final Optional<String> name;

        @Override
        public RequestSpecification apply(RequestSpecification rs) {
            if (code.isPresent()) {
                rs = rs.queryParam("code", code.get());
            }

            if (name.isPresent()) {
                rs = rs.queryParam("name", name.get());
            }

            return rs;
        }
    }

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrl(
                                        WEB_CONTEXT
                                                + "/"
                                                + APPCODE_CODE
                                                + "?date="
                                                + LocalDate.now()))
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
}
