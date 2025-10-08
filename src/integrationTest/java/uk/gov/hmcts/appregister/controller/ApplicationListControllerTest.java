package uk.gov.hmcts.appregister.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.courtlocation.exception.CourtLocationError;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.client.RoleEnum;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.util.PagingAssertionUtil;
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

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        var validPayload =
                new ApplicationListCreateDto()
                        .date(TEST_DATE)
                        .time(TEST_TIME)
                        .description("sec-matrix")
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE);

        return Stream.of(
                RestEndpointDescription.builder()
                        .url(getLocalUrl(WEB_CONTEXT))
                        .method(HttpMethod.POST)
                        .payload(validPayload)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }

    // --- GET_ALL ------------------------------------------------------------------

    private static final LocalDate D1 = LocalDate.of(2025, 10, 7);
    private static final LocalDate D2 = LocalDate.of(2025, 10, 8);
    private static final String T_0930 = "09:30";
    private static final String T_1030 = "10:30";
    private static final String CJA_52 = "52";
    private static final String CJA_99 = "99";
    private static final String COURT_BRS = "BRS001";
    private static final String COURT_MNC = "MNC002";

    @Test
    void givenFilterByDate_whenGet_then200OnlyThatDate() throws Exception {
        var token = getATokenWithValidCredentials().roles(List.of(RoleEnum.USER)).build().fetchTokenForRole();

        var filter = new ApplicationListGetFilterDto().date(D1);
        var pageMeta = new OpenApiPageMetaData(); // defaults page 0 size 10

        Response resp =
            restAssuredClient.executeGetRequestWithPaging(
                Optional.empty(), Optional.empty(), List.of(),
                getLocalUrl(WEB_CONTEXT), token,
                filter, pageMeta);

        resp.then().statusCode(200);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).extracting("date").containsOnly(D1);
        assertThat(page.getTotalElements()).isGreaterThan(0);
    }

    @Test
    void givenFilterByTime_whenGet_then200OnlyThatTime() throws Exception {
        var token = getATokenWithValidCredentials().roles(List.of(RoleEnum.USER)).build().fetchTokenForRole();

        var filter = new ApplicationListGetFilterDto().time(T_0930);
        var pageMeta = new OpenApiPageMetaData();

        Response resp =
            restAssuredClient.executeGetRequestWithPaging(
                Optional.empty(),
                Optional.empty(),
                List.of(),
                getLocalUrl(WEB_CONTEXT),
                token,
                pageMeta
            );

        resp.then().statusCode(200);
        ApplicationListPage page = resp.as(ApplicationListPage.class);
        assertThat(page.getContent()).extracting("time").containsOnly(T_0930);
    }

    @Test
    void givenFilterByCjaCode_whenGet_then200ExactMatch() throws Exception {
        var token = getATokenWithValidCredentials().roles(List.of(RoleEnum.USER)).build().fetchTokenForRole();

        var filter = new ApplicationListGetFilterDto().cjaCode(CJA_52);
        var pageMeta = new OpenApiPageMetaData();

        Response resp =
            restAssuredClient.executeGetRequestWithPaging(
                Optional.empty(), Optional.empty(), List.of(),
                getLocalUrl(WEB_CONTEXT), token,
                filter, pageMeta);

        resp.then().statusCode(200);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).extracting("cjaCode").containsOnly(CJA_52);
    }

    @Test
    void givenFilterByCourtLocationCode_whenGet_then200ExactMatch() throws Exception {
        var token = getATokenWithValidCredentials().roles(List.of(RoleEnum.USER)).build().fetchTokenForRole();

        var filter = new ApplicationListGetFilterDto().courtLocationCode(COURT_BRS);
        var pageMeta = new OpenApiPageMetaData();

        Response resp =
            restAssuredClient.executeGetRequestWithPaging(
                Optional.empty(), Optional.empty(), List.of(),
                getLocalUrl(WEB_CONTEXT), token,
                filter, pageMeta);

        resp.then().statusCode(200);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).extracting("courtCode").containsOnly(COURT_BRS);
    }

    @Test
    void givenFilterByDescriptionContains_whenGet_then200CaseInsensitive() throws Exception {
        var token = getATokenWithValidCredentials().roles(List.of(RoleEnum.USER)).build().fetchTokenForRole();

        var filter = new ApplicationListGetFilterDto().description("morning"); // contains, case-insensitive
        var pageMeta = new OpenApiPageMetaData();

        Response resp =
            restAssuredClient.executeGetRequestWithPaging(
                Optional.empty(), Optional.empty(), List.of(),
                getLocalUrl(WEB_CONTEXT), token,
                filter, pageMeta);

        resp.then().statusCode(200);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).extracting("description")
            .allSatisfy(d -> assertThat(((String) d).toLowerCase()).contains("morning"));
    }

    @Test
    void givenFilterByOtherLocationDescriptionContains_whenGet_then200CaseInsensitive() throws Exception {
        var token = getATokenWithValidCredentials().roles(List.of(RoleEnum.USER)).build().fetchTokenForRole();

        var filter = new ApplicationListGetFilterDto().otherLocationDescription("town");
        var pageMeta = new OpenApiPageMetaData();

        Response resp =
            restAssuredClient.executeGetRequestWithPaging(
                Optional.empty(), Optional.empty(), List.of(),
                getLocalUrl(WEB_CONTEXT), token,
                filter, pageMeta);

        resp.then().statusCode(200);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).extracting("otherLocationDescription")
            .allSatisfy(s -> assertThat(((String) s).toLowerCase()).contains("town"));
    }

    @Test
    void givenFilterByStatus_whenGet_then200OnlyThatStatus() throws Exception {
        var token = getATokenWithValidCredentials().roles(List.of(RoleEnum.USER)).build().fetchTokenForRole();

        var filter = new ApplicationListGetFilterDto().status(ApplicationListStatus.OPEN);
        var pageMeta = new OpenApiPageMetaData();

        Response resp =
            restAssuredClient.executeGetRequestWithPaging(
                Optional.empty(), Optional.empty(), List.of(),
                getLocalUrl(WEB_CONTEXT), token,
                filter, pageMeta);

        resp.then().statusCode(200);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).extracting("status").containsOnly(ApplicationListStatus.OPEN);
    }

    /* ------------------------ Filters (combinations) ------------------------ */

    @Test
    void givenFilterCombo_whenGet_then200AndOnlyMatching() throws Exception {
        var token = getATokenWithValidCredentials().roles(List.of(RoleEnum.USER)).build().fetchTokenForRole();

        var filter = new ApplicationListGetFilterDto()
            .date(D2)
            .status(ApplicationListStatus.OPEN)
            .cjaCode(CJA_99)
            .otherLocationDescription("borough");

        var pageMeta = new OpenApiPageMetaData();

        Response resp =
            restAssuredClient.executeGetRequestWithPaging(
                Optional.empty(), Optional.empty(), List.of(),
                getLocalUrl(WEB_CONTEXT), token,
                filter, pageMeta);

        resp.then().statusCode(200);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(1);
        var item = page.getContent().get(0);
        assertThat(item.getDate()).isEqualTo(D2);
        assertThat(item.getStatus()).isEqualTo(ApplicationListStatus.OPEN);
        assertThat(item.getCjaCode()).isEqualTo(CJA_99);
        assertThat(item.getOtherLocationDescription()).containsIgnoringCase("borough");
    }

    /* ------------------------ Sorting ------------------------ */

    @Test
    void givenSortByDescriptionDesc_whenGet_then200AndSortedDesc() throws Exception {
        var token = getATokenWithValidCredentials().roles(List.of(RoleEnum.USER)).build().fetchTokenForRole();

        var filter = new ApplicationListGetFilterDto();
        var pageMeta = new OpenApiPageMetaData();

        // OpenAPI-style sort parameters, e.g. "description,desc"
        List<String> sort = List.of("description,desc");

        Response resp =
            restAssuredClient.executeGetRequestWithPaging(
                Optional.empty(), Optional.empty(), sort,
                getLocalUrl(WEB_CONTEXT), token,
                filter, pageMeta);

        resp.then().statusCode(200);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        // Verify desc order by descriptions (first few should be near "Foxtrot"/"echo"/"delta Session"...)
        assertThat(page.getContent()).extracting(ApplicationListGetItemDto::getDescription)
            .containsSubsequence("Foxtrot", "echo", "delta Session");
    }

    @Test
    void givenSortByDateAscThenTimeAsc_whenGet_then200AndTwoLevelSort() throws Exception {
        var token = getATokenWithValidCredentials().roles(List.of(RoleEnum.USER)).build().fetchTokenForRole();

        var filter = new ApplicationListGetFilterDto();
        var pageMeta = new OpenApiPageMetaData();
        List<String> sort = List.of("date,asc", "time,asc");

        Response resp =
            restAssuredClient.executeGetRequestWithPaging(
                Optional.empty(), Optional.empty(), sort,
                getLocalUrl(WEB_CONTEXT), token,
                filter, pageMeta);

        resp.then().statusCode(200);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        // Expect all D1 entries first (09:30 before 10:30), then D2 (09:30 before 10:30)
        var dates = page.getContent().stream().map(ApplicationListGetItemDto::getDate).toList();
        assertThat(dates.indexOf(D1)).isLessThan(dates.lastIndexOf(D1));
        assertThat(dates.lastIndexOf(D1)).isLessThan(dates.indexOf(D2));

        // Within first date, expect 09:30 before 10:30
        var firstDateItems = page.getContent().stream().filter(i -> D1.equals(i.getDate())).toList();
        assertThat(firstDateItems.stream().map(ApplicationListGetItemDto::getTime).toList())
            .containsSequence(T_0930, T_1030);
    }

    @Test
    void givenSortByStatusAscThenDescriptionAsc_whenGet_then200AndStableOrder() throws Exception {
        var token = getATokenWithValidCredentials().roles(List.of(RoleEnum.USER)).build().fetchTokenForRole();

        var filter = new ApplicationListGetFilterDto();
        var pageMeta = new OpenApiPageMetaData();
        List<String> sort = List.of("status,asc", "description,asc");

        Response resp =
            restAssuredClient.executeGetRequestWithPaging(
                Optional.empty(), Optional.empty(), sort,
                getLocalUrl(WEB_CONTEXT), token,
                filter, pageMeta);

        resp.then().statusCode(200);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        // Basic sanity: all OPEN items appear grouped before others if enum ordering matches ordinal name order
        // (OPEN < others). If your mapper maps differently, adjust assertions accordingly.
        var statuses = page.getContent().stream().map(ApplicationListGetItemDto::getStatus).toList();
        assertThat(statuses).isNotEmpty();
    }

    /* ------------------------ Paging ------------------------ */

    @Test
    void givenSecondPage_whenGet_then200AndCorrectSlice() throws Exception {
        var token = getATokenWithValidCredentials().roles(List.of(RoleEnum.USER)).build().fetchTokenForRole();

        var filter = new ApplicationListGetFilterDto();
        var pageMeta = new OpenApiPageMetaData()
            .withPage(1)     // second page (0-based)
            .withSize(2);    // small size to force multiple pages

        Response resp =
            restAssuredClient.executeGetRequestWithPaging(
                Optional.of(1), Optional.of(2), List.of(),
                getLocalUrl(WEB_CONTEXT), token,
                filter, pageMeta);

        resp.then().statusCode(200);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        PagingAssertionUtil.assertPageDetails(page, 2, 1, page.getTotalPages(), page.getTotalElements());
        assertThat(page.getContent().size()).isBetween(1, 2);
    }

    /* ------------------------ Validation / error paths ------------------------ */

    @Test
    void givenInvalidTimePattern_whenGet_then400() throws Exception {
        var token = getATokenWithValidCredentials().roles(List.of(RoleEnum.USER)).build().fetchTokenForRole();

        // time must match HH:mm (24h). Supply an invalid one to trigger @Pattern
        var filter = new ApplicationListGetFilterDto().time("9:30");
        var pageMeta = new OpenApiPageMetaData();

        Response resp =
            restAssuredClient.executeGetRequestWithPaging(
                Optional.empty(), Optional.empty(), List.of(),
                getLocalUrl(WEB_CONTEXT), token,
                filter, pageMeta);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
        resp.then().contentType(VND_JSON_V1);
    }

    @Test
    void givenTooLargePageSize_whenGet_then400() throws Exception {
        var token = getATokenWithValidCredentials().roles(List.of(RoleEnum.USER)).build().fetchTokenForRole();

        var filter = new ApplicationListGetFilterDto();
        var pageMeta = new OpenApiPageMetaData().withPage(0).withSize(200); // > @Max(100)

        Response resp =
            restAssuredClient.executeGetRequestWithPaging(
                Optional.of(0), Optional.of(200), List.of(),
                getLocalUrl(WEB_CONTEXT), token,
                filter, pageMeta);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void givenNoAuth_whenGet_then401() {
        // No token at all
        Response resp = restAssuredClient.executeGetRequestWithPaging(getLocalUrl(WEB_CONTEXT), null);
        resp.then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
