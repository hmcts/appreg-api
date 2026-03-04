package uk.gov.hmcts.appregister.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.restassured.response.Response;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation;
import uk.gov.hmcts.appregister.applicationlist.audit.AppListAuditOperation;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.controller.applicationlist.AbstractApplicationListTest;
import uk.gov.hmcts.appregister.controller.applicationlist.GetApplicationListFilterSpecification;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.testutils.annotation.StabilityTest;
import uk.gov.hmcts.appregister.testutils.client.OpenApiPageMetaData;
import uk.gov.hmcts.appregister.testutils.util.AuditLogAsserter;
import uk.gov.hmcts.appregister.util.CreateEntryDtoUtil;

public class ApplicationListControllerTest extends AbstractApplicationListTest {

    private static final String WEB_CONTEXT = "application-lists";
    private static final String GET_ENTRIES_CONTEXT = "application-list-entries";
    private static final String VND_JSON_V1 = "application/vnd.hmcts.appreg.v1+json";
    private static final String UNKNOWN_APPLICATION_LIST_ID =
            "ffffffff-ffff-ffff-ffff-ffffffffffff";

    // --- Seeded reference data ----------------------------------------------------
    private static final String VALID_COURT_CODE = "CCC003";
    private static final String VALID_COURT_NAME = "Cardiff Crown Court";

    private static final String VALID_CJA_CODE = "CD";

    private static final String VALID_OTHER_LOCATION = "CJA_CD_DESCRIPTION";

    private static final LocalDate TEST_DATE = LocalDate.of(2025, 10, 15);
    private static final LocalTime TEST_TIME = LocalTime.of(10, 30);

    @Autowired private ApplicationListRepository applicationListRepository;

    @Autowired private TransactionalUnitOfWork unitOfWork;

    @Autowired private ApplicationListEntryRepository aleRepository;

    // --- Happy path: create with COURT --------------------------------------------------------

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

        differenceLogAsserter.clearLogs();

        // fire tests
        resp = restAssuredClient.executeDeleteRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);

        // assert the diff audit log message
        differenceLogAsserter.assertNoErrors();
        differenceLogAsserter.assertDiffCount(2, false);
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "al_id",
                        null,
                        null,
                        "DELETE",
                        "Delete Application List"));
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "version",
                        "0",
                        null,
                        "DELETE",
                        "Delete Application List"));

        // assert success
        resp.then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void givenValidRequest_whenDeleteWithInvalidId_then404() throws Exception {
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

        // assert the diff audit log message
        differenceLogAsserter.assertNoErrors();
        differenceLogAsserter.assertDiffCount(0, true);
    }

    @Test
    void givenValidRequest_whenDeleteTwice_thenSecondDeleteReturns404() throws Exception {
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

        resp = restAssuredClient.executeDeleteRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);
        resp.then().statusCode(HttpStatus.NO_CONTENT.value());

        differenceLogAsserter.clearLogs();

        // prove the delete has been made
        resp = restAssuredClient.executeDeleteRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);
        resp.then().statusCode(HttpStatus.CONFLICT.value());

        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.DELETION_ALREADY_IN_DELETABLE_STATE.getCode().getAppCode(),
                problemDetail.getType().toString());

        // assert the diff audit log message
        differenceLogAsserter.assertNoErrors();
        differenceLogAsserter.assertDiffCount(0, true);
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
                        new OpenApiPageMetaData());

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getPageNumber()).isEqualTo(1);
        assertThat(page.getPageSize()).isEqualTo(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getElementsOnPage()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);

        // this check is to make sure that a default value is provided for the log when the parameter is not needed.
        differenceLogAsserter.assertDataAuditChange(
                AuditLogAsserter.getDataAuditAssertion(
                    TableNames.APPICATION_LIST,
                    "id",
                    null,
                    "00000000-0000-0000-0000-000000000000",
                    AppListAuditOperation.GET_APP_LIST.getType().name(),
                    AppListAuditOperation.GET_APP_LIST.getEventName()));

        // this is to check that the description has been logged as it was included as a param.
        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.APPICATION_LIST,
                "list_description",
                null,
                prefix,
                AppListAuditOperation.GET_APP_LIST.getType().name(),
                AppListAuditOperation.GET_APP_LIST.getEventName()));
    }

    @Test
    @DisplayName("GET: filter by date + time (exact match)")
    void givenDateAndTimeFilter_thenOnlyThatSlot() throws Exception {
        String prefix = uniquePrefix("get-date-time");
        LocalDate day = LocalDate.of(2025, 10, 15);
        LocalTime t0930 = LocalTime.of(9, 30);

        ApplicationListGetDetailDto applicationListGetDetailDto =
                createWithCourt(prefix + " - keep", day, t0930);

        // create 3 entries for the record
        createEntry(applicationListGetDetailDto.getId());
        createEntry(applicationListGetDetailDto.getId());
        createEntry(applicationListGetDetailDto.getId());

        LocalTime t1030 = LocalTime.of(10, 30);

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
                        GetApplicationListFilterSpecification.builder()
                                .description(Optional.of(prefix))
                                .dateValue(Optional.of(day.toString())) // yyyy-MM-dd
                                .localTime(Optional.of("09:30"))
                                .build(),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(1);
        var only = page.getContent().getFirst();
        assertThat(only.getDate()).isEqualTo(day);
        assertThat(only.getTime()).isEqualTo(t0930);
        assertThat(only.getDescription()).endsWith("keep");
        assertThat(only.getEntriesCount()).isEqualTo(3);

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.APPICATION_LIST,
                "list_description",
                null,
                prefix,
                AppListAuditOperation.GET_APP_LIST.getType().name(),
                AppListAuditOperation.GET_APP_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.APPICATION_LIST,
                "application_list_time",
                null,
                t0930.toString(),
                AppListAuditOperation.GET_APP_LIST.getType().name(),
                AppListAuditOperation.GET_APP_LIST.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.APPICATION_LIST,
                "application_list_date",
                null,
                day.toString(),
                AppListAuditOperation.GET_APP_LIST.getType().name(),
                AppListAuditOperation.GET_APP_LIST.getEventName()));
    }

    @Test
    @DisplayName("GET: filter by 23:59")
    void givenTimeFilter_thenSlot() throws Exception {

        String prefix = uniquePrefix("get-date-time");
        LocalDate day = LocalDate.of(2025, 10, 15);
        LocalTime t2359 = LocalTime.of(23, 59);

        createWithCourt(prefix + " - keep", day, t2359);

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
                        rs -> rs.header("Accept", VND_JSON_V1).queryParam("time", "23:59"),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(1);
        var only = page.getContent().getFirst();
        assertThat(only.getTime()).isEqualTo(t2359);

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.APPICATION_LIST,
                "application_list_time",
                null,
                t2359.toString(),
                AppListAuditOperation.GET_APP_LIST.getType().name(),
                AppListAuditOperation.GET_APP_LIST.getEventName()));

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
                        GetApplicationListFilterSpecification.builder()
                                .description(Optional.of(prefix))
                                .courtLocationCode(Optional.of(VALID_COURT_CODE))
                                .description(Optional.of("court-filter"))
                                .build(),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(1);
        var only = page.getContent().getFirst();
        assertThat(only.getLocation()).isEqualTo(VALID_COURT_NAME);

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.APPICATION_LIST,
                "courthouse_code",
                null,
                VALID_COURT_CODE,
                AppListAuditOperation.GET_APP_LIST.getType().name(),
                AppListAuditOperation.GET_APP_LIST.getEventName()));
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
                        GetApplicationListFilterSpecification.builder()
                                .description(Optional.of(prefix))
                                .cjaCode(Optional.of(VALID_CJA_CODE))
                                .otherLocationDescription(Optional.of(VALID_CJA_CODE))
                                .build(),
                        null);

        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListPage page = resp.as(ApplicationListPage.class);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getDescription()).contains(prefix);

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.APPICATION_LIST,
                "cja_code",
                null,
                VALID_CJA_CODE,
                AppListAuditOperation.GET_APP_LIST.getType().name(),
                AppListAuditOperation.GET_APP_LIST.getEventName()));
    }

    @Test
    @DisplayName("GET: does not return soft deleted list")
    void givenDefaults_whenGet_then200AndNoSoftDeletedSlot() throws Exception {

        // setup a record for deletion
        String prefix = uniquePrefix("soft-deleted");
        ApplicationListGetDetailDto dto =
                createWithCourt(
                        prefix + " - Zebra", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));
        UUID id = dto.getId();

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeDeleteRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + id), userToken);
        resp.then().statusCode(HttpStatus.NO_CONTENT.value());

        resp =
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

        assertThat(page.getContent()).hasSize(0);

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.APPICATION_LIST,
                "description",
                null,
                prefix,
                AppListAuditOperation.GET_APP_LIST.getType().name(),
                AppListAuditOperation.GET_APP_LIST.getEventName()));
    }

    @Test
    @DisplayName("GET page: entriesCount excludes soft-deleted entry")
    void givenEntryDeleted_whenGetApplicationLists_thenEntriesCountExcludesDeleted()
            throws Exception {

        // 1) Create application list via API
        String prefix = uniquePrefix("entries-delete");
        ApplicationListGetDetailDto created =
                createWithCourt(prefix + " - list", TEST_DATE, TEST_TIME);
        UUID listId = created.getId();

        // 2) Prepare token to create entries
        var token = getToken();

        // 3) Build two EntryCreateDto payloads
        EntryCreateDto entryCreateDto1 = CreateEntryDtoUtil.getCorrectCreateEntryDto();

        EntryCreateDto entryCreateDto2 = CreateEntryDtoUtil.getCorrectCreateEntryDto();

        // 4) Create entries
        Response createResp1 =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + listId + "/entries"),
                        token,
                        entryCreateDto1);
        createResp1.then().statusCode(HttpStatus.CREATED.value());

        Response createResp2 =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + listId + "/entries"),
                        token,
                        entryCreateDto2);
        createResp2.then().statusCode(HttpStatus.CREATED.value());
        EntryGetDetailDto createdEntry2 = createResp2.as(EntryGetDetailDto.class);

        // 5) Call the entries search endpoint to fetch entries for this list
        Response entriesPageResp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(20),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(GET_ENTRIES_CONTEXT),
                        token);

        entriesPageResp.then().statusCode(HttpStatus.OK.value());

        EntryPage entriesPage = entriesPageResp.as(EntryPage.class);
        assertThat(entriesPage.getContent()).isNotNull();

        boolean foundCreated2 =
                entriesPage.getContent().stream()
                        .anyMatch(e -> createdEntry2.getId().equals(e.getId()));
        assertThat(foundCreated2)
                .withFailMessage("createdEntry2 must be present in entries search results")
                .isTrue();

        UUID idFromSearch =
                entriesPage.getContent().stream()
                        .map(EntryGetSummaryDto::getId)
                        .filter(id -> createdEntry2.getId().equals(id))
                        .findFirst()
                        .orElseThrow(
                                () -> new AssertionError("Entry id not found by entries search"));

        // 6) soft-delete the entry
        aleRepository.softDeleteByUuid(idFromSearch);
        aleRepository.flush(); // ensure DB is updated for subsequent controller query

        // 7) Call the GET /application-lists endpoint
        Response pageResp =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        getLocalUrl("application-lists"),
                        token,
                        rs -> rs.header("Accept", VND_JSON_V1).queryParam("description", prefix),
                        null);

        // 8) Assert that entriesCount excludes the deleted entry
        pageResp.then().statusCode(HttpStatus.OK.value());
        ApplicationListPage page = pageResp.as(ApplicationListPage.class);

        // find summary to assert against
        ApplicationListGetSummaryDto summaryToAssertDto =
                page.getContent().stream()
                        .filter(summary -> summary.getId().equals(listId))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("List id not found"));

        Assertions.assertEquals(1, summaryToAssertDto.getEntriesCount());
        assertThat(summaryToAssertDto.getEntriesCount())
                .withFailMessage("entriesCount should exclude the deleted entry removed via repo")
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("GET Application List")
    @StabilityTest
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
        assertThat(dto.getCjaCode()).isEqualToIgnoringCase(VALID_CJA_CODE);
        assertThat(dto.getEntriesCount()).isEqualTo(0);
        assertThat(dto.getEntriesSummary()).isNotNull();

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.APPICATION_LIST,
                "id",
                null,
                id.toString(),
                AppListAuditOperation.GET_APP_LIST.getType().name(),
                AppListAuditOperation.GET_APP_LIST.getEventName()));
    }

    @Test
    @DisplayName("GET Application List")
    void givenValidRequest_whenGetApplicationList_then400IdFormatting() throws Exception {
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

        // fire test
        resp = restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT + "/232322"), token);

        // assert success
        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        assertThat(problemDetail.getType().toString())
                .isEqualTo(CommonAppError.TYPE_MISMATCH_ERROR.getCode().getAppCode());
        assertThat(problemDetail.getDetail())
                .contains("Problem with value 232322 for parameter listId");
        assertThat(problemDetail.getStatus()).isEqualTo(400);

        // Making sure there is no audit log for this failed request.
        assertThatThrownBy(() -> differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.APPICATION_LIST,
                "id",
                null,
                "232322",
                AppListAuditOperation.GET_APP_LIST.getType().name(),
                AppListAuditOperation.GET_APP_LIST.getEventName())))
            .isInstanceOf(java.lang.AssertionError.class)
            .hasMessage("We did not found expected logs");
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
    @DisplayName("GET Application List: 404 when list soft deleted")
    void givenSoftDeletedApplicationList_whenGetApplicationList_then404() throws Exception {
        ApplicationListGetDetailDto created =
                createWithCourt("soft-deleted-get", TEST_DATE, TEST_TIME);
        UUID id = created.getId();

        var token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response deleteResp =
                restAssuredClient.executeDeleteRequest(getLocalUrl(WEB_CONTEXT + "/" + id), token);
        deleteResp.then().statusCode(HttpStatus.NO_CONTENT.value());

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
    @DisplayName(
            "GET Application List: entriesSummary and entriesCount exclude soft-deleted entries")
    void givenEntrySoftDeleted_whenGetApplicationList_thenDeletedEntryExcludedFromSummaryAndCount()
            throws Exception {

        var token = getToken();

        // create list
        UUID listId = createApplicationList(token, uniquePrefix("get-by-id-exclude-deleted"));

        // create two entries
        final EntryGetDetailDto entry1 = createEntry(listId);
        final EntryGetDetailDto entry2 = createEntry(listId);

        // sanity-check that initial GET shows two entries
        ApplicationListGetDetailDto initial = getApplicationListDetail(listId, token);
        assertThat(initial.getEntriesCount()).isEqualTo(2L);
        assertThat(initial.getEntriesSummary()).isNotNull();
        assertThat(initial.getEntriesSummary().size()).isGreaterThanOrEqualTo(2);

        // soft-delete 2nd entry
        softDeleteEntry(entry2.getId());

        // GET again and assert
        ApplicationListGetDetailDto after = getApplicationListDetail(listId, token);
        assertThat(after.getEntriesCount())
                .withFailMessage("entriesCount should exclude the soft-deleted entry")
                .isEqualTo(1L);

        assertThat(after.getEntriesSummary())
                .withFailMessage("entriesSummary must be present")
                .isNotNull();

        List<UUID> returnedEntryIds =
                after.getEntriesSummary().stream()
                        .map(ApplicationListEntrySummary::getUuid)
                        .toList();

        assertThat(returnedEntryIds)
                .withFailMessage("Soft-deleted entry must not appear in entriesSummary")
                .doesNotContain(entry2.getId());

        // sanity: remaining entry should be the first created one
        assertThat(returnedEntryIds).contains(entry1.getId());
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
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + id + "/print"), token);

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
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + id + "/print"), token);

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
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + id + "/print"), token);

        // assert success
        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        ProblemDetail problemDetail = resp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.LIST_NOT_FOUND.getCode().getAppCode(),
                problemDetail.getType().toString());
    }

    @Test
    @DisplayName("Print Application List: entries exclude soft-deleted entries")
    void givenEntrySoftDeleted_whenPrintApplicationList_thenDeletedEntryExcludedFromPrint()
            throws Exception {

        var token = getToken();

        // create list
        UUID listId = createApplicationList(token, uniquePrefix("print-exclude-deleted"));

        // create two entries
        final EntryGetDetailDto entry1 = createEntry(listId);
        final EntryGetDetailDto entry2 = createEntry(listId);

        // soft-delete 2nd entry
        softDeleteEntry(entry2.getId());

        // call print endpoint
        ApplicationListGetPrintDto printDto = getApplicationListPrint(listId, token);

        assertThat(printDto.getEntries())
                .withFailMessage("entries in print output must not be null")
                .isNotNull();

        List<UUID> returnedEntryIds =
                printDto.getEntries().stream().map(EntryGetPrintDto::getId).toList();

        assertThat(returnedEntryIds)
                .withFailMessage("Soft-deleted entry must not appear in print entries")
                .doesNotContain(entry2.getId());

        // sanity: remaining printed entry should include the first created one
        assertThat(returnedEntryIds).contains(entry1.getId());
    }

    @Test
    @DisplayName("GET by id with full data set")
    void givenGetById_whenGet_then200AndAllDataIsReturned() throws Exception {
        // setup a record for deletion
        String prefix = uniquePrefix("soft-deleted");
        ApplicationListGetDetailDto dto =
                createWithCourt(
                        prefix + " - Zebra", LocalDate.of(2025, 10, 15), LocalTime.of(10, 30));
        UUID id = dto.getId();

        // create a single entry
        final EntryGetDetailDto entryGetDetailDto = createEntry(dto.getId());

        var userToken =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();

        Response resp =
                restAssuredClient.executeGetRequest(getLocalUrl(WEB_CONTEXT + "/" + id), userToken);

        // make the assertions on the response
        resp.then().statusCode(HttpStatus.OK.value()).contentType(VND_JSON_V1);
        ApplicationListGetDetailDto page = resp.as(ApplicationListGetDetailDto.class);

        assertThat(page.getEntriesSummary().size()).isEqualTo(1);
        Assertions.assertTrue(page.getDescription().startsWith("soft-deleted ::"));
        Assertions.assertEquals(ApplicationListStatus.OPEN, page.getStatus());
        Assertions.assertEquals(1, page.getEntriesCount());
        Assertions.assertEquals("CCC003", page.getCourtCode());
        Assertions.assertEquals("Cardiff Crown Court", page.getCourtName());
        Assertions.assertEquals(1, page.getEntriesSummary().size());
        Assertions.assertEquals(
                "Copy documents", page.getEntriesSummary().get(0).getApplicationTitle());
        Assertions.assertEquals(
                entryGetDetailDto.getAccountNumber(),
                page.getEntriesSummary().get(0).getAccountNumber().get());
        Assertions.assertEquals(1, page.getEntriesSummary().get(0).getSequenceNumber());
        Assertions.assertEquals(
                entryGetDetailDto.getRespondent().getPerson().getContactDetails().getPostcode(),
                page.getEntriesSummary().get(0).getPostCode().get());
        Assertions.assertEquals(
                page.getEntriesSummary().get(0).getApplicant().get(),
                entryGetDetailDto.getApplicant().getPerson().getName().getSurname()
                        + ", "
                        + entryGetDetailDto.getApplicant().getPerson().getName().getFirstForename()
                        + ", "
                        + entryGetDetailDto.getApplicant().getPerson().getName().getTitle());

        Assertions.assertEquals(
                page.getEntriesSummary().get(0).getRespondent().get(),
                entryGetDetailDto.getRespondent().getPerson().getName().getSurname()
                        + ", "
                        + entryGetDetailDto.getRespondent().getPerson().getName().getFirstForename()
                        + ", "
                        + entryGetDetailDto.getRespondent().getPerson().getName().getTitle());

        differenceLogAsserter.assertDataAuditChange(
            AuditLogAsserter.getDataAuditAssertion(
                TableNames.APPICATION_LIST,
                "id",
                null,
                id.toString(),
                AppListAuditOperation.GET_APP_LIST.getType().name(),
                AppListAuditOperation.GET_APP_LIST.getEventName()));
    }

    @Test
    public void givenEntryUpdate_whenOpeningClosedList_then400() throws Exception {
        var token = getToken();

        // create list
        UUID listId = createApplicationList(token, uniquePrefix("update-open-closed-list"));

        // update list to closed
        var updateReq =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Updated description")
                        .status(ApplicationListStatus.CLOSED)
                        .courtLocationCode(VALID_COURT_CODE2)
                        .durationHours(1)
                        .durationMinutes(0);

        Response updateResp =
                restAssuredClient.executePutRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + listId), token, updateReq);
        updateResp.then().statusCode(HttpStatus.OK.value());

        // attempt to update back to open
        var reopenReq =
                new ApplicationListUpdateDto()
                        .date(TEST_DATE2)
                        .time(TEST_TIME2)
                        .description("Updated description")
                        .status(ApplicationListStatus.OPEN)
                        .durationHours(1)
                        .durationMinutes(0);

        Response reopenResp =
                restAssuredClient.executePutRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + listId), token, reopenReq);
        reopenResp.then().statusCode(HttpStatus.BAD_REQUEST.value());

        // Assert failure is due to invalid list status for update
        ProblemDetail problemDetail = reopenResp.as(ProblemDetail.class);
        Assertions.assertEquals(
                ApplicationListError.INVALID_LIST_STATUS.getCode().getAppCode(),
                problemDetail.getType().toString());
    }
}
