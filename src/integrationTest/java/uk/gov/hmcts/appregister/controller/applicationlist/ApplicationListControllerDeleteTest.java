package uk.gov.hmcts.appregister.controller.applicationlist;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.testutils.util.DataAuditLogAsserter;
import uk.gov.hmcts.appregister.util.CreateEntryDtoUtil;

public class ApplicationListControllerDeleteTest extends AbstractApplicationListControllerCrudTest {

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
            DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPICATION_LIST,
                        "al_id",
                        null,
                        null,
                        "DELETE",
                        "Delete Application List"));
        differenceLogAsserter.assertDataAuditChange(
            DataAuditLogAsserter.getDataAuditAssertion(
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
}
