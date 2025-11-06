package uk.gov.hmcts.appregister.controller;

import io.restassured.response.Response;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import io.restassured.specification.RequestSpecification;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.standardapplicant.dto.StandardApplicantDto;
import uk.gov.hmcts.appregister.standardapplicant.exception.StandardApplicantCodeError;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.client.request.DateGetRequest;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

public class StandardApplicantControllerTest extends AbstractSecurityControllerTest {
    private static final String WEB_CONTEXT = "standard-applicants";

    private static final String APPCODE_CODE = "APP001";

    private static final String DUPLICATE_APPCODE_CODE = "APP003";

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

        StandardApplicantDto[] responseContent = responseSpec.as(StandardApplicantDto[].class);
        Assertions.assertEquals(4, responseContent.length);

        // assert
        StandardApplicantDto returnedSa = responseContent[2];
        Assertions.assertEquals("APP003", returnedSa.applicantCode());
        Assertions.assertEquals("Dr", returnedSa.applicantTitle());
        Assertions.assertEquals("Alex Dunn", returnedSa.applicantName());
        Assertions.assertEquals("Alex", returnedSa.applicantForename1());
        Assertions.assertEquals("Taylor", returnedSa.applicantForename2());
        Assertions.assertNull(returnedSa.applicantForename3());
        Assertions.assertEquals("Dunn", returnedSa.applicantSurname());
        Assertions.assertEquals("789 Oak Avenue", returnedSa.addressLine1());
        Assertions.assertNull(returnedSa.addressLine2());
        Assertions.assertNull(returnedSa.addressLine3());
        Assertions.assertEquals("Villageham", returnedSa.addressLine4());
        Assertions.assertEquals("Countyshire", returnedSa.addressLine5());
        Assertions.assertEquals("VH3 3CD", returnedSa.postcode());
        Assertions.assertEquals("alex.johnson@example.com", returnedSa.emailAddress());
        Assertions.assertEquals("07987654321", returnedSa.mobileNumber());
        Assertions.assertNotNull(returnedSa.applicantStartDate());
    }

    @Test
    public void givenValidRequest_whenGetStandardApplicantByCodeAndDate_thenReturn200() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + APPCODE_CODE), tokenGenerator.fetchTokenForRole(),
                        new DateGetRequest(LocalDate.now()));

        // assert the response
        responseSpec.then().statusCode(200);

        StandardApplicantGetDetailDto returnedSa = responseSpec.as(StandardApplicantGetDetailDto.class);

        // assert the data
        Assertions.assertEquals("APP001", returnedSa.getCode());
        Assertions.assertEquals(LocalDate.now(), returnedSa.getStartDate());
        Assertions.assertFalse(returnedSa.getEndDate().isPresent());
        Assertions.assertNotNull(returnedSa.getFullName());
        Assertions.assertEquals("Mr", returnedSa.getFullName().getTitle());
        Assertions.assertEquals("John", returnedSa.getFullName().getFirstForename());
        Assertions.assertNull(returnedSa.getFullName().getSecondForename());
        Assertions.assertNull(returnedSa.getFullName().getThirdForename());
        Assertions.assertEquals("Smith", returnedSa.getFullName().getSurname());
        Assertions.assertNull(returnedSa.getFullName().getThirdForename());
        Assertions.assertNotNull(returnedSa.getContactDetails());
        Assertions.assertEquals("123 High Street", returnedSa.getContactDetails().getAddressLine1());
        Assertions.assertNull(returnedSa.getContactDetails().getAddressLine2());
        Assertions.assertNull(returnedSa.getContactDetails().getAddressLine3());
        Assertions.assertEquals("Townsville", returnedSa.getContactDetails().getAddressLine4());
        Assertions.assertNull( returnedSa.getContactDetails().getAddressLine5());
        Assertions.assertEquals("john.smith@example.com", returnedSa.getContactDetails().getEmail());
        Assertions.assertEquals("07123456789", returnedSa.getContactDetails().getMobile());
        Assertions.assertEquals("01234567890", returnedSa.getContactDetails().getPhone());
        Assertions.assertEquals("TS1 1AB", returnedSa.getContactDetails().getPostcode());
    }

    @Test
    public void givenValidRequest_whenGetStandardApplicantByCodeAndCodeNotExist_thenReturn404() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + "APP003NotExist"), tokenGenerator.fetchTokenForRole(),
                        new DateGetRequest(LocalDate.now()));

        // assert the response
        ProblemDetail returnedSc = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(StandardApplicantCodeError.STANDARD_APPLICANT_NOT_FOUND.getCode().getAppCode(), returnedSc.getType().toString());
        Assertions.assertEquals(StandardApplicantCodeError.STANDARD_APPLICANT_NOT_FOUND.getCode().getHttpCode().value(),
                responseSpec.getStatusCode());

    }

    @Test
    public void givenValidRequest_whenGetStandardApplicantByCodeAndDateNotWithinRange_thenReturn404() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + DUPLICATE_APPCODE_CODE), tokenGenerator.fetchTokenForRole(),
                        new DateGetRequest(LocalDate.now().minusDays(1)));

        // assert the response
        ProblemDetail returnedSc = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(StandardApplicantCodeError.STANDARD_APPLICANT_NOT_FOUND.getCode().getAppCode(), returnedSc.getType().toString());
        Assertions.assertEquals(StandardApplicantCodeError.STANDARD_APPLICANT_NOT_FOUND.getCode().getHttpCode().value(),
                responseSpec.getStatusCode());
    }

    @Test
    public void givenValidRequest_whenGetStandardApplicantByCodeAndDateMultiple_thenReturn409() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + DUPLICATE_APPCODE_CODE), tokenGenerator.fetchTokenForRole(),
                        new DateGetRequest(LocalDate.now()));

        // assert the response
        ProblemDetail returnedSc = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(StandardApplicantCodeError.DUPLICATE_RESULT_CODE_FOUND.getCode().getAppCode(), returnedSc.getType().toString());
        Assertions.assertEquals(StandardApplicantCodeError.DUPLICATE_RESULT_CODE_FOUND.getCode().getHttpCode().value(),
                responseSpec.getStatusCode());
    }

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        return Stream.of(
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT + "/" + APPCODE_CODE + "?date="
                                + LocalDate.now().toString()))
                        .method(HttpMethod.GET)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }
}
