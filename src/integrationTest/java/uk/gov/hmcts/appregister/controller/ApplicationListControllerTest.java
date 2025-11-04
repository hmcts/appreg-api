package uk.gov.hmcts.appregister.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.courtlocation.exception.CourtLocationError;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.testutils.client.PageMetaData;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

public class ApplicationListControllerTest extends AbstractSecurityControllerTest {

    private static final String WEB_CONTEXT = "application-lists";
    private static final String VND_JSON_V1 = "application/vnd.hmcts.appreg.v1+json";
    private static final String UNKNOWN_APPLICATION_LIST_ID = "ffffffff-ffff-ffff-ffff-ffffffffffff";

    // --- Seeded reference data ----------------------------------------------------
    private static final String VALID_COURT_CODE = "CCC003";
    private static final String VALID_COURT_NAME = "Cardiff Crown Court";

    private static final String VALID_CJA_CODE = "CD";
    private static final String VALID_OTHER_LOCATION = "CJA_CD_DESCRIPTION";

    private static final String UNKNOWN_COURT_CODE = "ZZZ999";
    private static final String UNKNOWN_CJA_CODE = "99X";

    private static final LocalDate TEST_DATE = LocalDate.of(2025, 10, 15);
    private static final LocalTime TEST_TIME = LocalTime.of(10, 30);

    // --- POST ---------------------------------------------------------------------
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

    // --- GET_ALL ---------------------------------------------------------------------
    private static String uniquePrefix(String base) {
        return base + " :: " + UUID.randomUUID();
    }

    private static PageMetaData stdPageMeta() {
        return new PageMetaData() {
            @Override
            public String getPageNumberQueryName() {
                return "page";
            }

            @Override
            public String getPageSizeQueryName() {
                return "size";
            }

            @Override
            public String getSortName() {
                return "sort";
            }
        };
    }

    private ApplicationListGetDetailDto createWithCourt(
            String description, LocalDate date, LocalTime time) throws Exception {

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(date)
                        .time(time)
                        .description(description)
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .durationHours(1)
                        .durationMinutes(0);

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);
        resp.then().statusCode(HttpStatus.CREATED.value()).contentType(VND_JSON_V1);
        return resp.as(ApplicationListGetDetailDto.class);
    }

    private ApplicationListGetDetailDto createWithCja(
            String description, LocalDate date, LocalTime time) throws Exception {

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        var req =
                new ApplicationListCreateDto()
                        .date(date)
                        .time(time)
                        .description(description)
                        .status(ApplicationListStatus.OPEN)
                        .cjaCode(VALID_CJA_CODE)
                        .otherLocationDescription(VALID_OTHER_LOCATION)
                        .durationHours(1)
                        .durationMinutes(0);

        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);
        resp.then().statusCode(HttpStatus.CREATED.value()).contentType(VND_JSON_V1);
        return resp.as(ApplicationListGetDetailDto.class);
    }

    @Test
    @DisplayName("GET: 403 when no role")
    void givenNoRole_whenGet_then403() throws Exception {
        var token = getATokenWithValidCredentials().build().fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        token,
                        rs -> rs.header("Accept", VND_JSON_V1),
                        null);

        resp.then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("GET: default paging + default sort (description ASC)")
    void givenDefaults_whenGet_then200AndSortedByDescriptionAsc() throws Exception {

        String prefix = uniquePrefix("get-default-sort");

        createWithCourt(prefix + " - Zebra", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));
        createWithCourt(prefix + " - Alpha", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));
        createWithCourt(prefix + " - Mango", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(), // Rely on default sort
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        rs -> rs.header("Accept", VND_JSON_V1).queryParam("description", prefix),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).getDescription()).endsWith("Alpha");
        assertThat(page.getContent().get(1).getDescription()).endsWith("Mango");
        assertThat(page.getContent().get(2).getDescription()).endsWith("Zebra");

        assertThat(page.getPageNumber()).isZero();
        assertThat(page.getPageSize()).isGreaterThanOrEqualTo(3);
        assertThat(page.getFirst()).isTrue();
    }

    @Test
    @DisplayName("GET: paging works (page=1,size=2)")
    void givenPaging_whenSecondPage_thenCorrectMetadata() throws Exception {

        String prefix = uniquePrefix("get-paging");

        createWithCourt(prefix + " - A", LocalDate.of(2025, 10, 14), LocalTime.of(9, 0));
        createWithCourt(prefix + " - B", LocalDate.of(2025, 10, 15), LocalTime.of(9, 0));
        createWithCourt(prefix + " - C", LocalDate.of(2025, 10, 16), LocalTime.of(9, 0));

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(2),
                        Optional.of(1),
                        List.of(), // default sort (description ASC)
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        rs -> rs.header("Accept", VND_JSON_V1).queryParam("description", prefix),
                        stdPageMeta());

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getPageNumber()).isEqualTo(1);
        assertThat(page.getPageSize()).isEqualTo(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getElementsOnPage()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("GET: filter by date + time (exact match)")
    void givenDateAndTimeFilter_thenOnlyThatSlot() throws Exception {

        String prefix = uniquePrefix("get-date-time");
        LocalDate day = LocalDate.of(2025, 10, 15);
        LocalTime t0930 = LocalTime.of(9, 30);
        LocalTime t1030 = LocalTime.of(10, 30);

        createWithCourt(prefix + " - keep", day, t0930);
        createWithCourt(prefix + " - drop-1", day, t1030);
        createWithCourt(prefix + " - drop-2", day.plusDays(1), t0930);

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        rs ->
                                rs.header("Accept", VND_JSON_V1)
                                        .queryParam("description", prefix)
                                        .queryParam("date", day.toString()) // yyyy-MM-dd
                                        .queryParam("time", "09:30"),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(1);
        var only = page.getContent().getFirst();
        assertThat(only.getDate()).isEqualTo(day);
        assertThat(only.getTime()).isEqualTo(t0930);
        assertThat(only.getDescription()).endsWith("keep");
    }

    @Test
    @DisplayName("GET: filter by courtLocationCode")
    void givenCourtFilter_thenOnlyCourtRows() throws Exception {

        String prefix = uniquePrefix("get-court-filter");

        createWithCourt(prefix + " - court", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));
        createWithCja(prefix + " - cja", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        rs ->
                                rs.header("Accept", VND_JSON_V1)
                                        .queryParam("description", prefix)
                                        .queryParam("courtLocationCode", VALID_COURT_CODE),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(1);
        var only = page.getContent().getFirst();
        assertThat(only.getLocation()).isEqualTo(VALID_COURT_NAME);
    }

    @Test
    @DisplayName("GET: filter by cjaCode")
    void givenCjaFilter_thenOnlyCjaRows() throws Exception {

        String prefix = uniquePrefix("get-cja-filter");

        createWithCja(prefix + " - cja", LocalDate.of(2025, 10, 16), LocalTime.of(11, 0));
        createWithCourt(prefix + " - court", LocalDate.of(2025, 10, 16), LocalTime.of(11, 0));

        var adminToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        getLocalUrl(WEB_CONTEXT),
                        adminToken,
                        rs ->
                                rs.header("Accept", VND_JSON_V1)
                                        .queryParam("description", prefix)
                                        .queryParam("cjaCode", VALID_CJA_CODE),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getDescription()).contains(prefix);
    }

    @Test
    @DisplayName("GET: allowed sort (date,desc & time,desc)")
    void givenAllowedSort_thenSorted() throws Exception {

        String prefix = uniquePrefix("get-sort-allowed");

        createWithCourt(prefix + " - A", LocalDate.of(2025, 10, 14), LocalTime.of(9, 0));
        createWithCourt(prefix + " - B", LocalDate.of(2025, 10, 15), LocalTime.of(10, 0));
        createWithCourt(prefix + " - C", LocalDate.of(2025, 10, 15), LocalTime.of(9, 0));

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of("date,desc", "time,desc"),
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        rs -> rs.header("Accept", VND_JSON_V1).queryParam("description", prefix),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).getDescription()).endsWith("B");
        assertThat(page.getContent().get(1).getDescription()).endsWith("C");
        assertThat(page.getContent().get(2).getDescription()).endsWith("A");
    }

    @Test
    @DisplayName("GET: disallowed sort (cja) -> 400")
    void givenDisallowedSort_then400() throws Exception {

        String prefix = uniquePrefix("get-sort-disallowed");

        createWithCourt(prefix + " - X", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of("cja,asc"),
                        getLocalUrl(WEB_CONTEXT),
                        userToken,
                        rs -> rs.header("Accept", VND_JSON_V1).queryParam("description", prefix),
                        null);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("GET Application List")
    void givenValidRequest_whenGetApplicationList_then200AndBody() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        String description = "List for testing get application list";

        var req =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description(description)
                        .status(ApplicationListStatus.OPEN)
                        .cjaCode(VALID_CJA_CODE)
                        .otherLocationDescription(VALID_OTHER_LOCATION)
                        .durationHours(1)
                        .durationMinutes(0);

        // setup a record for retrieval
        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);
        resp.then().statusCode(HttpStatus.CREATED.value());

        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        UUID id = dto.getId();

        // fire test
        resp = restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);

        // assert success
        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);

        dto = resp.as(ApplicationListGetDetailDto.class);
        assertThat(dto.getDescription()).isEqualToIgnoringCase(description);
        assertThat(dto.getEntriesCount()).isEqualTo(0);
        assertThat(dto.getEntriesSummary()).isNotNull();
    }

    @Test
    @DisplayName("GET Application List: 403 when no role")
    void givenNoRole_whenGetApplicationList_then403() throws Exception {
        var token = getATokenWithValidCredentials().build().fetchTokenForRole();

        UUID id = UUID.randomUUID();

        // fire test
        Response resp =
                restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);

        resp.then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    // --- Not found: Application List -------------------------------------------------
    @Test
    @DisplayName("GET Application List: 404 when list unknown")
    void givenUnknownApplicationList_whenGetApplicationList_then404() throws Exception {
        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        UUID id = UUID.fromString(UNKNOWN_APPLICATION_LIST_ID);

        // fire test
        Response resp =
                restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);

        // assert success
        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.LIST_NOT_FOUND.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    @Test
    @DisplayName("Print Application List")
    void givenValidRequest_whenPrintApplicationList_then200AndBody() throws Exception {
        var token =
            getATokenWithValidCredentials()
                .roles(List.of(RoleEnum.USER))
                .build()
                .fetchTokenForRole();

        String description = "List for testing get application list";

        var req =
            new ApplicationListCreateDto()
                .date(TEST_DATE)
                .time(TEST_TIME)
                .description(description)
                .status(ApplicationListStatus.OPEN)
                .cjaCode(VALID_CJA_CODE)
                .otherLocationDescription(VALID_OTHER_LOCATION)
                .durationHours(1)
                .durationMinutes(0);

        // setup a record for retrieval
        Response resp = restAssuredClient.executePostRequest(getLocalUrl(WEB_CONTEXT), token, req);
        resp.then().statusCode(HttpStatus.CREATED.value());

        ApplicationListGetDetailDto dto = resp.as(ApplicationListGetDetailDto.class);
        UUID id = dto.getId();

        // fire test
        Response printApplicationListResp =
            restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT + "/" + id + "/print"), token);

        // assert success
        printApplicationListResp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);

        ApplicationListGetPrintDto applicationListGetPrintDto =
            printApplicationListResp.as(ApplicationListGetPrintDto.class);
        assertThat(applicationListGetPrintDto.getEntries()).isNotNull();
    }

    @Test
    @DisplayName("Print Application List: 403 when no role")
    void givenNoRole_whenPrintApplicationList_then403() throws Exception {
        var token = getATokenWithValidCredentials().build().fetchTokenForRole();

        UUID id = UUID.randomUUID();

        // fire test
        Response resp =
            restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT + "/" + id + "/print"), token);

        resp.then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    // --- Not found: Application List -------------------------------------------------
    @Test
    @DisplayName("Print Application List: 404 when list unknown")
    void givenUnknownApplicationList_whenPrintApplicationList_then404() throws Exception {
        var token =
            getATokenWithValidCredentials()
                .roles(List.of(RoleEnum.USER))
                .build()
                .fetchTokenForRole();

        UUID id = UUID.fromString(UNKNOWN_APPLICATION_LIST_ID);

        // fire test
        Response resp =
            restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT + "/" + id + "/print"), token);

        // assert success
        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
            ApplicationListError.LIST_NOT_FOUND.getCode().getAppCode(),
            problemDetail.getType().toString());
    }
}
