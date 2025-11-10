package uk.gov.hmcts.appregister.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.Clock;
import java.time.Instant;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.appregister.common.model.IndividualOrOrganisation;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodePage;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPage;
import uk.gov.hmcts.appregister.standardapplicant.dto.StandardApplicantDto;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
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

    @BeforeEach
    public void before() {
        when(clock.instant()).thenReturn(Instant.now().plus(2, ChronoUnit.DAYS));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.withZone(org.mockito.ArgumentMatchers.any(ZoneId.class))).thenReturn(clock);
    }

    @Test
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

    @Test
    public void givenValidRequest_whenGetStandardApplicantById_thenReturn200() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + 3), tokenGenerator.fetchTokenForRole());

        // assert the response
        responseSpec.then().statusCode(200);

        StandardApplicantDto returnedSc = responseSpec.as(StandardApplicantDto.class);

        // assert
        Assertions.assertEquals("APP003", returnedSc.applicantCode());
        Assertions.assertEquals("Dr", returnedSc.applicantTitle());
        Assertions.assertEquals("Alex", returnedSc.applicantForename1());
        Assertions.assertEquals("Taylor", returnedSc.applicantForename2());
        Assertions.assertNull(returnedSc.applicantForename3());
        Assertions.assertEquals("Dunn", returnedSc.applicantSurname());
        Assertions.assertEquals("789 Oak Avenue", returnedSc.addressLine1());
        Assertions.assertNull(returnedSc.addressLine2());
        Assertions.assertNull(returnedSc.addressLine3());
        Assertions.assertEquals("Villageham", returnedSc.addressLine4());
        Assertions.assertEquals("Countyshire", returnedSc.addressLine5());
        Assertions.assertEquals("VH3 3CD", returnedSc.postcode());
        Assertions.assertEquals("alex.johnson@example.com", returnedSc.emailAddress());
        Assertions.assertEquals("07987654321", returnedSc.mobileNumber());
        Assertions.assertNotNull(returnedSc.applicantStartDate());
    }

    @Test
    public void
            givenValidRequest_whenGetStandardApplicantWithPagingCriteriaWithoutExplicitSort_thenReturn200()
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

        StandardApplicantPage response = responseSpec.as(StandardApplicantPage.class);

        // make the assertions
        PagingAssertionUtil.assertPageDetails(
                response, pageSize, pageNumber, 4, TOTAL_STANDARD_APPLICANT_COUNT);

        // assert the first auth code record
        StandardApplicantGetSummaryDto firstEntry = response.getContent().get(0);

        assertEquals("APP003", firstEntry.getCode());
        assertEquals("Alex Dunn", firstEntry.getName());
        assertEquals("789 Oak Avenue", firstEntry.getAddressLine1());
        assertNotNull(firstEntry.getStartDate());
        assertFalse(firstEntry.getEndDate().isPresent());

        StandardApplicantGetSummaryDto secondEntry = response.getContent().get(1);
        assertEquals("APP004", secondEntry.getCode());
        assertEquals("Organisation 1", secondEntry.getName());
        assertEquals("123 High Street", secondEntry.getAddressLine1());
        assertNotNull(secondEntry.getStartDate());
        assertFalse(secondEntry.getEndDate().isPresent());
    }

    @Test
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
        assertEquals("Organisation 3", firstEntry.getName());
        assertEquals("456 Elm Road", firstEntry.getAddressLine1());
        assertNotNull(firstEntry.getStartDate());
        assertFalse(firstEntry.getEndDate().isPresent());

        StandardApplicantGetSummaryDto secondEntry = response.getContent().get(1);
        assertEquals("APP005", secondEntry.getCode());
        assertEquals("Organisation 2", secondEntry.getName());
        assertEquals("456 Elm Road", secondEntry.getAddressLine1());
        assertNotNull(secondEntry.getStartDate());
        assertFalse(secondEntry.getEndDate().isPresent());
    }

    @Test
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
                        new ApplicationCodeControllerTest.ApplicationCodeRequestFilter(
                                Optional.of("does not exist"), Optional.of("does not exist")),
                        new OpenApiPageMetaData());

        // assert the response is successful with no content
        responseSpec.then().statusCode(200);
        ApplicationCodePage response = responseSpec.as(ApplicationCodePage.class);
        PagingAssertionUtil.assertPageDetails(response, pageSize, pageNumber, 0, 0);
    }

    @Test
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
                        new ApplicationCodeControllerTest.ApplicationCodeRequestFilter(
                                Optional.empty(), Optional.of("ORG")),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);
        StandardApplicantPage response = responseSpec.as(StandardApplicantPage.class);
        PagingAssertionUtil.assertPageDetails(response, pageSize, pageNumber, 1, 3);

        Assertions.assertEquals("Organisation 1", response.getContent().get(0).getName());
        Assertions.assertEquals("Organisation 2", response.getContent().get(1).getName());
        Assertions.assertEquals("Organisation 3", response.getContent().get(2).getName());
    }

    @Test
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
                        new ApplicationCodeControllerTest.ApplicationCodeRequestFilter(
                                Optional.empty(), Optional.of("J")),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);
        StandardApplicantPage response = responseSpec.as(StandardApplicantPage.class);
        PagingAssertionUtil.assertPageDetails(response, pageSize, pageNumber, 1, 2);

        Assertions.assertEquals("Jane Doe", response.getContent().get(0).getName());
        Assertions.assertEquals("John Smith", response.getContent().get(1).getName());
    }

    @Test
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
                        new ApplicationCodeControllerTest.ApplicationCodeRequestFilter(
                                Optional.empty(), Optional.of(",Evan")),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);
        StandardApplicantPage response = responseSpec.as(StandardApplicantPage.class);
        PagingAssertionUtil.assertPageDetails(response, pageSize, pageNumber, 1, 1);

        Assertions.assertEquals(
                IndividualOrOrganisation.DEFAULT_NAME + " Evans",
                response.getContent().get(0).getName());
    }

    @Test
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
                        new ApplicationCodeControllerTest.ApplicationCodeRequestFilter(
                                Optional.of("APP001"), Optional.of("John, Smith")),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);
        StandardApplicantPage page = responseSpec.as(StandardApplicantPage.class);
        PagingAssertionUtil.assertPageDetails(page, pageSize, pageNumber, 1, 1);
        StandardApplicantGetSummaryDto firstEntry = page.getContent().get(0);
        assertEquals("APP001", firstEntry.getCode());
        assertEquals("John Smith", firstEntry.getName());
    }

    @Test
    public void
            givenValidRequest_whenGetStandardApplicantWithPageNumberBeyondResultBoundary_thenReturn200()
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
                        List.of("name"),
                        getLocalUrl(WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        new ApplicationCodeControllerTest.ApplicationCodeRequestFilter(
                                Optional.of("APP001"), Optional.of("John, Smith")),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(200);
        ApplicationCodePage page = responseSpec.as(ApplicationCodePage.class);
        PagingAssertionUtil.assertPageDetails(page, pageSize, pageNumber, 1, 1);
        Assertions.assertNull(page.getContent());
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
                        new ApplicationCodeControllerTest.ApplicationCodeRequestFilter(
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
            givenValidRequest_whenGetStandardApplicantWithPagingInvalidPageNumber_thenReturn200()
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
                        new ApplicationCodeControllerTest.ApplicationCodeRequestFilter(
                                Optional.of("AP99004"), Optional.of("John, Smith")),
                        new OpenApiPageMetaData());
        // assert the response
        responseSpec.then().statusCode(500);
    }

    // NOTE: Spring defaults the page size to the max size if we try and increase it beyond. This
    // does not behave
    // accordingly
    @Test
    public void
            givenValidRequest_whenGetStandardApplicantWithPagingInvalidPageSizeBeyondDefault_thenReturn200()
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
                        new ApplicationCodeControllerTest.ApplicationCodeRequestFilter(
                                Optional.of("AP99004"), Optional.of("John, Smith")),
                        new OpenApiPageMetaData());

        // assert the response
        responseSpec.then().statusCode(500);
    }

    @RequiredArgsConstructor
    static class StandardApplicantRequestFilter implements UnaryOperator<RequestSpecification> {
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

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }
}
