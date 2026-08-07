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
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
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
                        + System.lineSeparator()
                        + "standardApplicantsExport.code=size must be between 0 and 10",
                problemDetail.getDetail());
    }

    @Test
    void testExportCsvSearchByNameWithResults() throws Exception {
        final StandardApplicant sa = insertStandardApplicant();

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // test the functionality
        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/export"),
                        tokenGenerator.fetchTokenForRole(),
                        rs -> rs.queryParam("name", "John PlayDough"));

        responseSpec.then().statusCode(200);

        String csv = responseSpec.asString();
        List<StandardApplicantCsvRow> rows = parseCsv(csv);

        Assertions.assertEquals(2, rows.size());

        // Header row validation
        headerRowValidation(rows.get(0));

        // Data row validation
        dataRowValidation(rows.get(1), sa);
    }

    private void headerRowValidation(StandardApplicantCsvRow header) {
        Assertions.assertEquals(header.getApplicantCode(), "Applicant Code");
        Assertions.assertEquals(header.getName(), "Name");
        Assertions.assertEquals(header.getApplicantStartDate(), "Use From");
        Assertions.assertEquals(header.getApplicantEndDate(), "Use To");
    }

    private void dataRowValidation(StandardApplicantCsvRow row, StandardApplicant expected) {
        Assertions.assertEquals(row.getApplicantCode(), expected.getApplicantCode());
        Assertions.assertEquals(
                row.getName(), expected.getName() == null ? "" : expected.getName());
        Assertions.assertEquals(
                row.getApplicantStartDate(), expected.getApplicantStartDate().toString());
        Assertions.assertEquals(
                row.getApplicantEndDate(),
                expected.getApplicantEndDate() == null
                        ? ""
                        : expected.getApplicantEndDate().toString());
    }

    private StandardApplicant insertStandardApplicant() {
        StandardApplicant sa = new StandardApplicantTestData().someComplete();
        sa.setApplicantCode("TEST001");
        sa.setApplicantTitle("Mr");
        sa.setName("Test Organisation");
        sa.setApplicantForename1("John");
        sa.setApplicantForename2("A");
        sa.setApplicantForename3("B");
        sa.setApplicantSurname("PlayDough");
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
