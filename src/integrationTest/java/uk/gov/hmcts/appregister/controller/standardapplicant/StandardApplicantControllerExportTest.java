package uk.gov.hmcts.appregister.controller.standardapplicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static utils.CsvParser.parseCsv;

import io.restassured.response.Response;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.standardapplicant.model.StandardApplicantCsvRow;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = "appreg.standard-applicants.max-print-rows=2")
public class StandardApplicantControllerExportTest
        extends AbstractStandardApplicantControllerCrudTest {

    @Autowired private StandardApplicantRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        when(clock.withZone(any(ZoneId.class))).thenReturn(Clock.systemUTC());
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal())
                .thenReturn(TokenGenerator.builder().build().getJwtFromToken());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void populatedPersonalDetailsAreAbsentFromAllReferenceResponses() throws Exception {
        insertStandardApplicant();
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();
        var detail =
                restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT + "/TEST001"), token);
        detail.then().statusCode(200);
        assertThat(detail.jsonPath().getMap("").keySet())
                .containsExactlyInAnyOrder("code", "name", "startDate", "endDate");
        var search =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT), token, rs -> rs.queryParam("code", "TEST001"));
        search.then().statusCode(200);
        assertThat(search.jsonPath().getMap("content[0]").keySet())
                .containsExactlyInAnyOrder("code", "name", "startDate", "endDate");
        var print =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/reports/print"),
                        token,
                        rs -> rs.queryParam("code", "TEST001"));
        print.then().statusCode(200);
        assertThat(print.jsonPath().getMap("searchCriteria").keySet())
                .containsExactlyInAnyOrder("code", "name", "from", "to");
        assertThat(print.jsonPath().getMap("applicants[0]").keySet())
                .containsExactlyInAnyOrder("code", "name", "useFrom", "useTo");
        for (var response : List.of(detail, search, print)) {
            assertThat(response.asString())
                    .doesNotContain(
                            "PlayDough", "123 Test Street", "john@testorg.com", "07123456789");
        }
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
    void testExportCsvCodeAndNameSucceed() throws Exception {
        final StandardApplicant sa = insertStandardApplicant();
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

        responseSpec.then().statusCode(200);
        var rows = parseCsv(responseSpec.asString());
        Assertions.assertEquals(2, rows.size());
        dataRowValidation(rows.get(1), sa);
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

    @ParameterizedTest
    @CsvSource({"code,asc", "code,desc", "name,asc", "name,desc"})
    void searchPrintAndExportReturnSameActiveMatchesAcrossPages(String field, String direction)
            throws Exception {
        final var first = insertStandardApplicant();
        var second = insertStandardApplicant();
        second.setApplicantCode("TEST002");
        second.setName("Another Test Organisation");
        repository.saveAndFlush(second);
        var expired = insertStandardApplicant();
        expired.setApplicantEndDate(LocalDate.now().minusDays(1));
        repository.saveAndFlush(expired);
        var future = insertStandardApplicant();
        future.setApplicantStartDate(LocalDate.now().plusDays(1));
        repository.saveAndFlush(future);
        var nonMatching = insertStandardApplicant();
        nonMatching.setName("Unrelated");
        nonMatching.setApplicantForename1("Unrelated");
        repository.saveAndFlush(nonMatching);
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();
        var sort = field + "," + direction;
        var codes = new ArrayList<String>();
        for (int page = 0; page < 2; page++) {
            var pageNumber = page;
            var response =
                    restAssuredClient.executeGetRequest(
                            getLocalUrl(WEB_CONTEXT),
                            token,
                            rs ->
                                    rs.queryParam("code", "TEST")
                                            .queryParam("name", "Organisation")
                                            .queryParam("sort", sort)
                                            .queryParam("pageSize", 1)
                                            .queryParam("pageNumber", pageNumber));
            response.then().statusCode(200);
            assertThat(response.jsonPath().getInt("totalElements")).isEqualTo(2);
            codes.addAll(response.jsonPath().getList("content.code", String.class));
        }
        var expected =
                field.equals("code") == direction.equals("asc")
                        ? List.of(first.getApplicantCode(), second.getApplicantCode())
                        : List.of(second.getApplicantCode(), first.getApplicantCode());
        assertThat(codes).containsExactlyElementsOf(expected);

        var csv =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/export"),
                        token,
                        rs ->
                                rs.queryParam("code", "TEST")
                                        .queryParam("name", "Organisation")
                                        .queryParam("sort", sort));
        csv.then().statusCode(200);
        assertThat(
                        parseCsv(csv.asString()).stream()
                                .skip(1)
                                .map(StandardApplicantCsvRow::getApplicantCode))
                .containsExactlyElementsOf(codes);
        var print =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/reports/print"),
                        token,
                        rs ->
                                rs.queryParam("code", "TEST")
                                        .queryParam("name", "Organisation")
                                        .queryParam("sort", sort));
        print.then().statusCode(200);
        assertThat(print.jsonPath().getList("applicants.code", String.class))
                .containsExactlyElementsOf(codes);
    }

    @Test
    void printRejectsResultsAboveConfiguredLimit() throws Exception {
        insertStandardApplicant();
        insertStandardApplicant();
        insertStandardApplicant();
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();
        var response =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/reports/print"),
                        token,
                        rs -> rs.queryParam("code", "TEST001"));
        response.then().statusCode(400);
        assertThat(response.as(ProblemDetail.class).getDetail())
                .isEqualTo("Standard Applicant print result limit exceeded");
    }

    @Test
    void exportWithoutFiltersReturnsActiveApplicants() throws Exception {
        var applicant = insertStandardApplicant();
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();
        var response =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/export"), token, rs -> rs);
        response.then().statusCode(200);
        assertThat(
                        parseCsv(response.asString()).stream()
                                .skip(1)
                                .map(StandardApplicantCsvRow::getApplicantCode))
                .contains(applicant.getApplicantCode());
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
