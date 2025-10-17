package uk.gov.hmcts.appregister.controller;

import io.restassured.response.Response;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.standardapplicant.dto.StandardApplicantDto;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

public class StandardApplicantControllerTest extends BaseIntegration {
    private static final String WEB_CONTEXT = "standard-applicants";

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
        Assertions.assertEquals(3, responseContent.length);

        // assert
        StandardApplicantDto returnedSc = responseContent[2];
        Assertions.assertEquals("APP003", returnedSc.applicantCode());
        Assertions.assertEquals("Dr", returnedSc.applicantTitle());
        Assertions.assertEquals("Alex Dunn", returnedSc.applicantName());
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
        Assertions.assertEquals("Alex Dunn", returnedSc.applicantName());
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
}
