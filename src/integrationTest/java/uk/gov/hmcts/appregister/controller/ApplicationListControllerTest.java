package uk.gov.hmcts.appregister.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;

import io.restassured.response.Response;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.http.HttpHeaders;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;
import uk.gov.hmcts.appregister.testutils.client.RoleEnum;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

public class ApplicationListControllerTest extends AbstractSecurityControllerTest {

    private static final String WEB_CONTEXT = "application-lists";
    private static final String VND_JSON_V1 = "application/vnd.hmcts.appreg.v1+json";

    // --- Seeded reference data ----------------------------------------------------
    private static final String VALID_COURT_CODE = "CCC003";
    private static final String VALID_COURT_NAME = "Cardiff Crown Court";
    private static final String VALID_COURT_CODE2 = "BCC006";

    private static final String VALID_CJA_CODE = "CD";
    private static final String VALID_OTHER_LOCATION = "CJA_CD_DESCRIPTION";

    private static final String UNKNOWN_COURT_CODE = "ZZZ999";
    private static final String UNKNOWN_CJA_CODE = "99X";

    private static final LocalDate TEST_DATE = LocalDate.of(2025, 10, 15);
    private static final String TEST_TIME = "10:30";

    private static final LocalDate TEST_DATE2 = LocalDate.of(2025, 10, 19);
    private static final String TEST_TIME2 = "10:30";

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

        resp.then().header("Etag", org.hamcrest.Matchers.notNullValue());
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
        ProblemAssertUtil.assertEquals(ApplicationListError.COURT_NOT_FOUND.getCode(), resp);
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
        ProblemAssertUtil.assertEquals(ApplicationListError.CJA_NOT_FOUND.getCode(), resp);
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
    void givenValidRequest_whenUpdateWithCourt_then200AndBody() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        String[] createdLocation = createAppListUsingRestApi();

        CourtLocationGetDetailDto courtLocationGetDetailDto = new CourtLocationGetDetailDto();
        courtLocationGetDetailDto.setLocationCode(VALID_COURT_CODE2);
        courtLocationGetDetailDto.setStartDate(LocalDate.now());
        courtLocationGetDetailDto.setEndDate(JsonNullable.of(LocalDate.now()));
        courtLocationGetDetailDto.setName("Manchester Crown Court");
        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list (court) update")
                        .status(ApplicationListStatus.CLOSED)
                        .courtLocation(courtLocationGetDetailDto)
                        .durationHours(4)
                        .durationMinutes(32)
                        .version(2L);

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req);

        resp.then().statusCode(HttpStatus.OK.value());
        resp.then().contentType(VND_JSON_V1);
        resp.then().header("Etag", org.hamcrest.Matchers.notNullValue());

        // Location header should point to /application-lists/{uuid}
        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getVersion()).isEqualTo(1L); // per seed: Version = 0
        assertThat(dto.getDate()).isEqualTo(TEST_DATE2);
        assertThat(dto.getTime()).isEqualTo(TEST_TIME2); // mapper emits "HH:mm" when seconds = 0
        assertThat(dto.getDescription()).isEqualTo("Morning list (court) update");
        assertThat(dto.getStatus()).isEqualTo(ApplicationListStatus.CLOSED);
        assertThat(dto.getDurationHours()).isEqualTo(4);
        assertThat(dto.getDurationMinutes()).isEqualTo(32);

        // Court populated, CJA null
        assertThat(dto.getCourtCode()).isEqualTo(VALID_COURT_CODE2);
        assertThat(dto.getCourtName()).isEqualTo("Bristol Crown Court");
        assertThat(dto.getCjaCode()).isNull();
        assertThat(dto.getOtherLocationDescription()).isNull();
    }

    @Test
    void givenValidRequest_whenUpdateWithCourtWithMatchProblem_then412() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        String[] createdLocation = createAppListUsingRestApi();

        CourtLocationGetDetailDto courtLocationGetDetailDto = new CourtLocationGetDetailDto();
        courtLocationGetDetailDto.setLocationCode(VALID_COURT_CODE2);
        courtLocationGetDetailDto.setStartDate(LocalDate.now());
        courtLocationGetDetailDto.setEndDate(JsonNullable.of(LocalDate.now()));
        courtLocationGetDetailDto.setName("Manchester Crown Court");
        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list (court) update")
                        .status(ApplicationListStatus.CLOSED)
                        .courtLocation(courtLocationGetDetailDto)
                        .durationHours(4)
                        .durationMinutes(32)
                        .version(2L);

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req, "never going to match");

        resp.then().statusCode(HttpStatus.PRECONDITION_FAILED.value());
        ProblemAssertUtil.assertEquals(CommonAppError.MATCH_ETAG_FAILURE.getCode(), resp);
    }

    @Test
    void givenValidRequest_whenUpdateWithCourtWithMatch_then200() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        String[] createdLocation = createAppListUsingRestApi();

        CourtLocationGetDetailDto courtLocationGetDetailDto = new CourtLocationGetDetailDto();
        courtLocationGetDetailDto.setLocationCode(VALID_COURT_CODE2);
        courtLocationGetDetailDto.setStartDate(LocalDate.now());
        courtLocationGetDetailDto.setEndDate(JsonNullable.of(LocalDate.now()));
        courtLocationGetDetailDto.setName("Manchester Crown Court");
        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list (court) update")
                        .status(ApplicationListStatus.CLOSED)
                        .courtLocation(courtLocationGetDetailDto)
                        .durationHours(4)
                        .durationMinutes(32)
                        .version(2L);

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req, createdLocation[1]);

        resp.then().statusCode(HttpStatus.OK.value());
        resp.then().contentType(VND_JSON_V1);
        resp.then().header("Etag", org.hamcrest.Matchers.notNullValue());

        // Location header should point to /application-lists/{uuid}
        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getVersion()).isEqualTo(1L); // per seed: Version = 0
        assertThat(dto.getDate()).isEqualTo(TEST_DATE2);
        assertThat(dto.getTime()).isEqualTo(TEST_TIME2); // mapper emits "HH:mm" when seconds = 0
        assertThat(dto.getDescription()).isEqualTo("Morning list (court) update");
        assertThat(dto.getStatus()).isEqualTo(ApplicationListStatus.CLOSED);
        assertThat(dto.getDurationHours()).isEqualTo(4);
        assertThat(dto.getDurationMinutes()).isEqualTo(32);

        // Court populated, CJA null
        assertThat(dto.getCourtCode()).isEqualTo(VALID_COURT_CODE2);
        assertThat(dto.getCourtName()).isEqualTo("Bristol Crown Court");
        assertThat(dto.getCjaCode()).isNull();
        assertThat(dto.getOtherLocationDescription()).isNull();
    }

    // --- Happy path: create with CJA + otherLocation ------------------------------------------
    @Test
    void givenValidRequest_whenUpdateWithCja_then201() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        String[] createdLocation = createAppListUsingRestApi();

        CriminalJusticeAreaGetDto criminalJusticeAreaGetDto = new CriminalJusticeAreaGetDto();
        criminalJusticeAreaGetDto.setCode(VALID_CJA_CODE);
        criminalJusticeAreaGetDto.setDescription("Description of CD");

        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list (court) update")
                        .status(ApplicationListStatus.CLOSED)
                        .criminalJusticeArea(criminalJusticeAreaGetDto)
                        .durationHours(4)
                        .durationMinutes(32)
                        .version(2L)
                        .otherLocationDescription("Updated other location");

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req);

        resp.then().contentType(VND_JSON_V1);

        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getVersion()).isEqualTo(1L);
        assertThat(dto.getDate()).isEqualTo(TEST_DATE2);
        assertThat(dto.getTime()).isEqualTo(TEST_TIME2);
        assertThat(dto.getDescription()).isEqualTo("Morning list (court) update");
        assertThat(dto.getStatus()).isEqualTo(ApplicationListStatus.CLOSED);

        // CJA populated, Court null
        assertThat(dto.getCjaCode()).isEqualTo(VALID_CJA_CODE);
        assertThat(dto.getOtherLocationDescription()).isEqualTo("Updated other location");
        assertThat(dto.getCourtCode()).isNull();
        assertThat(dto.getCourtName()).isNull();
    }

    // --- Happy path: create with CJA + otherLocation ------------------------------------------
    @Test
    void givenValidRequest_whenUpdateWithCjaWithMatch_then201() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        String[] createdLocation = createAppListUsingRestApi();

        CriminalJusticeAreaGetDto criminalJusticeAreaGetDto = new CriminalJusticeAreaGetDto();
        criminalJusticeAreaGetDto.setCode(VALID_CJA_CODE);
        criminalJusticeAreaGetDto.setDescription("Description of CD");

        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list (court) update")
                        .status(ApplicationListStatus.CLOSED)
                        .criminalJusticeArea(criminalJusticeAreaGetDto)
                        .durationHours(4)
                        .durationMinutes(32)
                        .version(2L)
                        .otherLocationDescription("Updated other location");

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req, createdLocation[1]);

        resp.then().contentType(VND_JSON_V1);

        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getVersion()).isEqualTo(1L);
        assertThat(dto.getDate()).isEqualTo(TEST_DATE2);
        assertThat(dto.getTime()).isEqualTo(TEST_TIME2);
        assertThat(dto.getDescription()).isEqualTo("Morning list (court) update");
        assertThat(dto.getStatus()).isEqualTo(ApplicationListStatus.CLOSED);

        // CJA populated, Court null
        assertThat(dto.getCjaCode()).isEqualTo(VALID_CJA_CODE);
        assertThat(dto.getOtherLocationDescription()).isEqualTo("Updated other location");
        assertThat(dto.getCourtCode()).isNull();
        assertThat(dto.getCourtName()).isNull();
    }

    @Test
    void givenValidRequest_whenUpdateWithCjaWithMatchProblem_then412() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        String[] createdLocation = createAppListUsingRestApi();

        CriminalJusticeAreaGetDto criminalJusticeAreaGetDto = new CriminalJusticeAreaGetDto();
        criminalJusticeAreaGetDto.setCode(VALID_CJA_CODE);
        criminalJusticeAreaGetDto.setDescription("Description of CD");

        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list (court) update")
                        .status(ApplicationListStatus.CLOSED)
                        .criminalJusticeArea(criminalJusticeAreaGetDto)
                        .durationHours(4)
                        .durationMinutes(32)
                        .version(2L)
                        .otherLocationDescription("Updated other location");

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req, "no match");
        resp.then().statusCode(HttpStatus.PRECONDITION_FAILED.value());
        ProblemAssertUtil.assertEquals(CommonAppError.MATCH_ETAG_FAILURE.getCode(), resp);
    }

    // --- Validation: UUID not found for update -------------------------------------------------
    @Test
    void givenInvalidUUID_whenUpdate_then404() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        CriminalJusticeAreaGetDto criminalJusticeAreaGetDto = new CriminalJusticeAreaGetDto();
        criminalJusticeAreaGetDto.setCode(VALID_CJA_CODE);
        criminalJusticeAreaGetDto.setDescription("Description of CD");

        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list (court) update")
                        .status(ApplicationListStatus.CLOSED)
                        .criminalJusticeArea(criminalJusticeAreaGetDto)
                        .durationHours(4)
                        .durationMinutes(32)
                        .version(2L)
                        .otherLocationDescription("Updated other location");

        Response resp =
                restAssuredClient.executePutRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + UUID.randomUUID()), token, req);

        resp.then().statusCode(HttpStatus.NOT_FOUND.value());

        // AL-1 (INVALID_LOCATION_COMBINATION)
        ProblemAssertUtil.assertEquals(
                ApplicationListError.APPLICATION_LIST_NOT_FOUND.getCode(), resp);
    }

    // --- Validation: XOR rule (both supplied) -------------------------------------------------
    @Test
    void givenInvalidLocationCombination_whenUpdate_then400() throws Exception {

        CriminalJusticeAreaGetDto criminalJusticeAreaGetDto = new CriminalJusticeAreaGetDto();
        criminalJusticeAreaGetDto.setCode(VALID_CJA_CODE);
        criminalJusticeAreaGetDto.setDescription("Description of CD");

        CourtLocationGetDetailDto courtLocationGetDetailDto = new CourtLocationGetDetailDto();
        courtLocationGetDetailDto.setName("Court Location");
        courtLocationGetDetailDto.setLocationCode(VALID_COURT_CODE2);
        courtLocationGetDetailDto.setStartDate(LocalDate.now());
        courtLocationGetDetailDto.setEndDate(JsonNullable.of(LocalDate.now()));
        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list (court) update")
                        .status(ApplicationListStatus.CLOSED)
                        .criminalJusticeArea(criminalJusticeAreaGetDto)
                        .durationHours(4)
                        .durationMinutes(32)
                        .version(2L)
                        .courtLocation(courtLocationGetDetailDto)
                        .otherLocationDescription("Updated other location");

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        String[] createdLocation = createAppListUsingRestApi();
        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req);

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
    void givenUnknownCourt_whenUpdate_then404() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        String[] createdLocation = createAppListUsingRestApi();
        CourtLocationGetDetailDto courtLocationGetDetailDto = new CourtLocationGetDetailDto();
        courtLocationGetDetailDto.setName("Court Location");
        courtLocationGetDetailDto.setLocationCode("Unknown");
        courtLocationGetDetailDto.setStartDate(LocalDate.now());
        courtLocationGetDetailDto.setEndDate(JsonNullable.of(LocalDate.now()));
        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list (court) update")
                        .status(ApplicationListStatus.CLOSED)
                        .durationHours(4)
                        .durationMinutes(32)
                        .version(2L)
                        .courtLocation(courtLocationGetDetailDto)
                        .otherLocationDescription("Updated other location");

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req);

        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        ProblemAssertUtil.assertEquals(ApplicationListError.COURT_NOT_FOUND.getCode(), resp);
    }

    // --- Not found: CJA -----------------------------------------------------------------------
    @Test
    void givenUnknownCja_whenUpdate_then404() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        String[] createdLocation = createAppListUsingRestApi();

        CriminalJusticeAreaGetDto criminalJusticeAreaGetDto = new CriminalJusticeAreaGetDto();
        criminalJusticeAreaGetDto.setCode("Unknown");
        criminalJusticeAreaGetDto.setDescription("Description of CD");
        var req =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Morning list (court) update")
                        .status(ApplicationListStatus.CLOSED)
                        .durationHours(4)
                        .durationMinutes(32)
                        .version(2L)
                        .criminalJusticeArea(criminalJusticeAreaGetDto)
                        .otherLocationDescription("Updated other location");

        Response resp =
                restAssuredClient.executePutRequest(
                        URI.create(createdLocation[0]).toURL(), token, req);

        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        ProblemAssertUtil.assertEquals(ApplicationListError.CJA_NOT_FOUND.getCode(), resp);
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

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        ApplicationListUpdateDto uploadPayload =
                Instancio.of(ApplicationListUpdateDto.class)
                        .withSettings(settings)
                        .ignore(field(ApplicationListUpdateDto::getCourtLocation))

                        // Instancio does not honour Max and Min annotations
                        .ignore(field(ApplicationListUpdateDto::getDurationHours))
                        .ignore(field(ApplicationListUpdateDto::getDurationMinutes))
                        .create();

        uploadPayload.setDurationHours(1);
        uploadPayload.setDurationMinutes(1);

        return Stream.of(
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT))
                        .method(HttpMethod.POST)
                        .payload(validPayload)
                        .successRole(RoleEnum.USER)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT))
                        .method(HttpMethod.POST)
                        .payload(validPayload)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT + "/" + UUID.randomUUID()))
                        .method(HttpMethod.PUT)
                        .payload(uploadPayload)
                        .successRole(RoleEnum.USER)
                        .build(),
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT + "/" + UUID.randomUUID()))
                        .method(HttpMethod.PUT)
                        .payload(uploadPayload)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }

    /**
     * A utility method to create a new record.
     *
     * @return A two part array:- [0] - the uuid that can be used to fetch or update the record [1]-
     *     the etag for optimistic locking at the api level
     */
    private String[] createAppListUsingRestApi() throws Exception {
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
        return new String[] {resp.header(HttpHeaders.LOCATION), resp.header(HttpHeaders.ETAG)};
    }
}
