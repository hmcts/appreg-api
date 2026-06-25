package uk.gov.hmcts.appregister.controller.standardapplicant;

import static org.mockito.Mockito.when;
import static utils.CsvParser.parseCsv;

import io.restassured.response.Response;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.standardapplicant.model.StandardApplicantCsvRow;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

@ExtendWith(OutputCaptureExtension.class)
public class StandardApplicantControllerExportTest
        extends AbstractStandardApplicantControllerCrudTest {

    @Autowired private StandardApplicantRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal())
                .thenReturn(TokenGenerator.builder().build().getJwtFromToken());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void testExportCsvCodeOnlySucceed() throws Exception {
        final StandardApplicant sa = insertStandardApplicant();
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/export"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("code", "TEST001"));

        responseSpec.then().statusCode(200);

        String csv = responseSpec.asString();
        List<StandardApplicantCsvRow> rows = parseCsv(csv);

        Assertions.assertEquals(2, rows.size());

        // Header row validation
        headerRowValidation(rows.get(0));

        // Data row validation
        dataRowValidation(rows.get(1), sa);
    }

    @Test
    void testExportCsvNameOnlySucceed() throws Exception {
        final StandardApplicant sa = insertStandardApplicant();

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/export"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("name", "Test Organisation"));

        responseSpec.then().statusCode(200);

        String csv = responseSpec.asString();
        List<StandardApplicantCsvRow> rows = parseCsv(csv);

        Assertions.assertEquals(2, rows.size());

        // Header row validation
        headerRowValidation(rows.get(0));

        // Data row validation
        dataRowValidation(rows.get(1), sa);
    }

    @Test
    void testExportCsvCodeAndNameFailure() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/export"),
                        tokenGenerator.fetchTokenForRole(),
                        rs ->
                                rs.queryParam("code", "TEST001")
                                        .queryParam("name", "Test Organisation"));

        responseSpec.then().statusCode(409);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(
                "Either code or name must be provided, but not both", problemDetail.getDetail());
    }

    @Test
    void testExportCsvNoResultsFoundFailure() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/export"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("code", "None"));

        responseSpec.then().statusCode(404);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(
                "No records found for the provided code or name", problemDetail.getDetail());
    }

    @Test
    void testExportCsvNoParametersFailure() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/export"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("code", "asdfghjklqwertyui"));

        responseSpec.then().statusCode(400);
        ProblemDetail problemDetail = responseSpec.as(ProblemDetail.class);
        Assertions.assertEquals(
                "Constraints failed for fields:"
                        + "\nstandardApplicantsExport.code=size must be between 0 and 10",
                problemDetail.getDetail());
    }

    private void headerRowValidation(StandardApplicantCsvRow header) {
        Assertions.assertEquals(header.getApplicantCode(), "Applicant Code");
        Assertions.assertEquals(header.getApplicantTitle(), "Applicant Title");
        Assertions.assertEquals(header.getName(), "Name");
        Assertions.assertEquals(header.getApplicantForename1(), "Forename 1");
        Assertions.assertEquals(header.getApplicantForename2(), "Forename 2");
        Assertions.assertEquals(header.getApplicantForename3(), "Forename 3");
        Assertions.assertEquals(header.getApplicantSurname(), "Surname");
        Assertions.assertEquals(header.getAddressLine1(), "Address Line 1");
        Assertions.assertEquals(header.getAddressLine2(), "Address Line 2");
        Assertions.assertEquals(header.getAddressLine3(), "Address Line 3");
        Assertions.assertEquals(header.getAddressLine4(), "Address Line 4");
        Assertions.assertEquals(header.getAddressLine5(), "Address Line 5");
        Assertions.assertEquals(header.getPostcode(), "Postcode");
        Assertions.assertEquals(header.getEmailAddress(), "Email Address");
        Assertions.assertEquals(header.getTelephoneNumber(), "Telephone Number");
        Assertions.assertEquals(header.getMobileNumber(), "Mobile Number");
        Assertions.assertEquals(header.getApplicantStartDate(), "Use From");
        Assertions.assertEquals(header.getApplicantEndDate(), "Use To");
    }

    private void dataRowValidation(StandardApplicantCsvRow row, StandardApplicant expected) {
        Assertions.assertEquals(row.getApplicantCode(), expected.getApplicantCode());
        Assertions.assertEquals(
                row.getApplicantTitle(),
                expected.getApplicantTitle() == null ? "" : expected.getApplicantTitle());
        Assertions.assertEquals(
                row.getName(), expected.getName() == null ? "" : expected.getName());
        Assertions.assertEquals(
                row.getApplicantForename1(),
                expected.getApplicantForename1() == null ? "" : expected.getApplicantForename1());
        Assertions.assertEquals(
                row.getApplicantForename2(),
                expected.getApplicantForename2() == null ? "" : expected.getApplicantForename2());
        Assertions.assertEquals(
                row.getApplicantForename3(),
                expected.getApplicantForename3() == null ? "" : expected.getApplicantForename3());
        Assertions.assertEquals(
                row.getApplicantSurname(),
                expected.getApplicantSurname() == null ? "" : expected.getApplicantSurname());
        Assertions.assertEquals(row.getAddressLine1(), expected.getAddressLine1());
        Assertions.assertEquals(
                row.getAddressLine2(),
                expected.getAddressLine2() == null ? "" : expected.getAddressLine2());
        Assertions.assertEquals(
                row.getAddressLine3(),
                expected.getAddressLine3() == null ? "" : expected.getAddressLine3());
        Assertions.assertEquals(
                row.getAddressLine4(),
                expected.getAddressLine4() == null ? "" : expected.getAddressLine4());
        Assertions.assertEquals(
                row.getAddressLine5(),
                expected.getAddressLine5() == null ? "" : expected.getAddressLine5());
        Assertions.assertEquals(
                row.getPostcode(), expected.getPostcode() == null ? "" : expected.getPostcode());
        Assertions.assertEquals(
                row.getEmailAddress(),
                expected.getEmailAddress() == null ? "" : expected.getEmailAddress());
        Assertions.assertEquals(
                row.getTelephoneNumber(),
                expected.getTelephoneNumber() == null ? "" : expected.getTelephoneNumber());
        Assertions.assertEquals(
                row.getMobileNumber(),
                expected.getMobileNumber() == null ? "" : expected.getMobileNumber());
        Assertions.assertEquals(
                row.getApplicantStartDate(), expected.getApplicantStartDate().toString());
        Assertions.assertEquals(
                row.getApplicantEndDate(),
                expected.getApplicantEndDate() == null
                        ? ""
                        : expected.getApplicantEndDate().toString());
    }

    private StandardApplicant insertStandardApplicant() {
        StandardApplicant sa = new StandardApplicant();
        sa.setApplicantCode("TEST001");
        sa.setApplicantTitle("Mr");
        sa.setName("Test Organisation");
        sa.setApplicantForename1("John");
        sa.setApplicantForename2("A");
        sa.setApplicantForename3("B");
        sa.setApplicantSurname("Doe");
        sa.setAddressLine1("123 Test Street");
        sa.setAddressLine2("Test Town");
        sa.setAddressLine3("Test City");
        sa.setAddressLine4("Test County");
        sa.setAddressLine5("Test Country");
        sa.setPostcode("TE5 7ST");
        sa.setEmailAddress("john@testorg.com");
        sa.setTelephoneNumber("0123456789");
        sa.setMobileNumber("07123456789");
        sa.setApplicantStartDate(LocalDate.now().minusDays(7));
        sa.setApplicantEndDate(null);
        sa.setChangedBy(1L);
        sa.setChangedDate(OffsetDateTime.now());

        return repository.saveAndFlush(sa);
    }
}
