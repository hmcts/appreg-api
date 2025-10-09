package uk.gov.hmcts.appregister.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.courtlocation.exception.CourtLocationError;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

public class ApplicationListControllerTest extends AbstractSecurityControllerTest {

    private static final String WEB_CONTEXT = "application-lists";
    private static final String VND_JSON_V1 = "application/vnd.hmcts.appreg.v1+json";

    // --- Seeded reference data ----------------------------------------------------
    private static final String VALID_COURT_CODE = "CCC003";
    private static final String VALID_COURT_NAME = "Cardiff Crown Court";

    private static final String VALID_CJA_CODE = "CD";
    private static final String VALID_OTHER_LOCATION = "CJA_CD_DESCRIPTION";

    private static final String UNKNOWN_COURT_CODE = "ZZZ999";
    private static final String UNKNOWN_CJA_CODE = "99X";

    private static final LocalDate TEST_DATE = LocalDate.of(2025, 10, 15);
    private static final String TEST_TIME = "10:30";

    // --- Happy path: create with COURT --------------------------------------------------------
    @Test
    void givenValidRequest_whenCreateWithCourt_then201AndBodyAndLocationHeader() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("Morning list (court)")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .durationHours(2)
                        .durationMinutes(30);

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);

        resp.then().statusCode(HttpStatus.CREATED.value());
        resp.then().contentType(VND_JSON_V1);

        // Location header should point to /application-lists/{uuid}
        String location = resp.getHeader("Location");
        assertThat(location).isNotBlank();
        assertThat(URI.create(location).getPath())
                .matches(".*/application-lists/[0-9a-fA-F\\-]{36}$");

        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getVersion()).isEqualTo(0L); // per seed: Version = 0
        assertThat(dto.getDate()).isEqualTo(TEST_DATE);
        assertThat(dto.getTime()).isEqualTo(TEST_TIME); // mapper emits "HH:mm" when seconds = 0
        assertThat(dto.getDescription()).isEqualTo("Morning list (court)");
        assertThat(dto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);

        // Court populated, CJA null
        assertThat(dto.getCourtCode()).isEqualTo(VALID_COURT_CODE);
        assertThat(dto.getCourtName()).isEqualTo(VALID_COURT_NAME);
        assertThat(dto.getCjaCode()).isNull();
        assertThat(dto.getOtherLocationDescription()).isNull();
    }

    // --- Happy path: create with CJA + otherLocation ------------------------------------------
    @Test
    void givenValidRequest_whenCreateWithCja_then201AndBodyAndLocationHeader() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("Morning list (cja)")
                        .status(ApplicationListStatus.OPEN)
                        .cjaCode(VALID_CJA_CODE)
                        .otherLocationDescription(VALID_OTHER_LOCATION)
                        .durationHours(1)
                        .durationMinutes(0);

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);

        resp.then().statusCode(HttpStatus.CREATED.value());
        resp.then().contentType(VND_JSON_V1);

        String location = resp.getHeader("Location");
        assertThat(location).isNotBlank();
        assertThat(URI.create(location).getPath())
                .matches(".*/application-lists/[0-9a-fA-F\\-]{36}$");

        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getVersion()).isEqualTo(0L);
        assertThat(dto.getDate()).isEqualTo(TEST_DATE);
        assertThat(dto.getTime()).isEqualTo(TEST_TIME);
        assertThat(dto.getDescription()).isEqualTo("Morning list (cja)");
        assertThat(dto.getStatus()).isEqualTo(ApplicationListStatus.OPEN);

        // CJA populated, Court null
        assertThat(dto.getCjaCode()).isEqualTo(VALID_CJA_CODE);
        assertThat(dto.getOtherLocationDescription()).isEqualTo(VALID_OTHER_LOCATION);
        assertThat(dto.getCourtCode()).isNull();
        assertThat(dto.getCourtName()).isNull();
    }

    // --- Validation: XOR rule (both supplied) -------------------------------------------------
    @Test
    void givenInvalidLocationCombination_whenCreate_then400() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("Invalid XOR: both")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .cjaCode(VALID_CJA_CODE)
                        .otherLocationDescription(VALID_OTHER_LOCATION);

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());

        // AL-1 (INVALID_LOCATION_COMBINATION)
        ProblemAssertUtil.assertEquals(
                uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError
                        .INVALID_LOCATION_COMBINATION
                        .getCode(),
                resp);
    }

    // --- Not found: court ---------------------------------------------------------------------
    @Test
    void givenUnknownCourt_whenCreate_then404() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("Unknown court")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(UNKNOWN_COURT_CODE);

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);

        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        ProblemAssertUtil.assertEquals(CourtLocationError.COURT_NOT_FOUND.getCode(), resp);
    }

    // --- Not found: CJA -----------------------------------------------------------------------
    @Test
    void givenUnknownCja_whenCreate_then404() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("Unknown cja")
                        .status(ApplicationListStatus.OPEN)
                        .cjaCode(UNKNOWN_CJA_CODE)
                        .otherLocationDescription(VALID_OTHER_LOCATION);

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);

        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        ProblemAssertUtil.assertEquals(
                uk.gov.hmcts.appregister.criminaljusticearea.exception.CriminalJusticeAreaError
                        .CJA_NOT_FOUND
                        .getCode(),
                resp);
    }

    // --- Bad DTO validation (example: bad time format) ----------------------------------------
    @Test
    void givenBadTimeFormat_whenCreate_then400() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time("25:61") // invalid
                        .description("Bad time")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE);

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    // --- Security / role coverage -------------------------------------------------------------
    @Test
    void givenNoRole_whenCreate_then403() throws Exception {
        var token = getATokenWithValidCredentials().build().fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("No role")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE);

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);

        resp.then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void givenValidRequest_whenDeleteWithValidId_then204() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("Morning list (cja)")
                        .status(ApplicationListStatus.OPEN)
                        .cjaCode(VALID_CJA_CODE)
                        .otherLocationDescription(VALID_OTHER_LOCATION)
                        .durationHours(1)
                        .durationMinutes(0);

        // setup a record for deletion
        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);
        resp.then().statusCode(HttpStatus.CREATED.value());

        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        UUID id = dto.getId();

        // fire tests
        resp = restAssuredClient.executeDeleteRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);

        // assert success
        resp.then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void givenValidRequest_whenDeleteWithInvalidId_then204() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        // fire tests
        Response resp =
                restAssuredClient.executeDeleteRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + UUID.randomUUID()), token);

        // assert success
        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.DELETION_ID_NOT_FOUND.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    @Test
    void givenValidRequest_whenDeleteWithConflict_then204() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("Morning list (cja)")
                        .status(ApplicationListStatus.OPEN)
                        .cjaCode(VALID_CJA_CODE)
                        .otherLocationDescription(VALID_OTHER_LOCATION)
                        .durationHours(1)
                        .durationMinutes(0);

        // setup a record for deletion
        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);
        resp.then().statusCode(HttpStatus.CREATED.value());

        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        UUID id = dto.getId();

        // fire tests
        resp = restAssuredClient.executeDeleteRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);

        // assert success
        resp.then().statusCode(HttpStatus.NO_CONTENT.value());

        // prove the delete has been made
        resp = restAssuredClient.executeDeleteRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);
        resp.then().statusCode(HttpStatus.CONFLICT.value());

        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.DELETION_ALREADY_IN_DELETABLE_STATE.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        var validPayload =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("sec-matrix")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE);

        List<RestEndpointDescription> allRestfulDescriptions = new ArrayList<>();
        allRestfulDescriptions.add(
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT))
                        .method(HttpMethod.POST)
                        .payload(validPayload)
                        .successRole(RoleEnum.ADMIN)
                        .build());

        allRestfulDescriptions.add(
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT))
                        .method(HttpMethod.POST)
                        .payload(validPayload)
                        .successRole(RoleEnum.USER)
                        .build());

        allRestfulDescriptions.add(
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT + "/" + UUID.randomUUID()))
                        .method(HttpMethod.DELETE)
                        .successRole(RoleEnum.ADMIN)
                        .build());

        allRestfulDescriptions.add(
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT + "/" + UUID.randomUUID()))
                        .method(HttpMethod.DELETE)
                        .successRole(RoleEnum.USER)
                        .build());
        return allRestfulDescriptions.stream();
    }
}
