package uk.gov.hmcts.appregister.controller.applicationentryresult;

import static uk.gov.hmcts.appregister.common.enumeration.Status.CLOSED;
import static uk.gov.hmcts.appregister.common.enumeration.Status.OPEN;
import static uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil.assertEquals;

import io.restassured.response.Response;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.applicationentryresult.audit.AppListEntryResultAuditOperation;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.util.EtagUtil;
import uk.gov.hmcts.appregister.data.ResolutionCodeTestData;
import uk.gov.hmcts.appregister.testutils.util.DataAuditLogAsserter;

public class ApplicationEntryResultControllerDeleteTest
        extends AbstractApplicationEntryResultCrudTest {

    @Test
    @DisplayName("Delete Application List Entry Result: 204 when valid IDs")
    void givenValidIds_whenDelete_then204() throws Exception {
        var list = createAndSaveList(OPEN);
        var entry = createEntry(list);
        persistance.save(entry);

        var resolutionCode = new ResolutionCodeTestData().someComplete();
        var entryResult = createAndSaveResolution(entry, resolutionCode);

        var token = getToken();

        // compute the current server-style ETag
        String expectedEtag = EtagUtil.generateEtag(List.of(entryResult));

        Response resp =
                deleteResult(
                        list.getUuid(),
                        entry.getUuid(),
                        entryResult.getUuid(),
                        token,
                        expectedEtag);

        resp.then().statusCode(HttpStatus.NO_CONTENT.value());

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                        "version",
                        null,
                        null,
                        AppListEntryResultAuditOperation.DELETE_APP_LIST_ENTRY_RESULT
                                .getType()
                                .name(),
                        AppListEntryResultAuditOperation.DELETE_APP_LIST_ENTRY_RESULT
                                .getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                        "aler_id",
                        null,
                        null,
                        AppListEntryResultAuditOperation.DELETE_APP_LIST_ENTRY_RESULT
                                .getType()
                                .name(),
                        AppListEntryResultAuditOperation.DELETE_APP_LIST_ENTRY_RESULT
                                .getEventName()));
    }

    @Test
    @DisplayName("Delete Application List Entry Result: 404 when list unknown")
    void givenUnknownList_whenDelete_then404() throws Exception {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();

        Response resp = deleteResult(listId, entryId, resultId, getToken());

        resp.then().statusCode(HttpStatus.CONFLICT.value());
        assertEquals(
                ApplicationListEntryResultError.APPLICATION_LIST_DOES_NOT_EXIST.getCode(), resp);
    }

    @Test
    @DisplayName("Delete Application List Entry Result: 400 when entry not in list")
    void givenEntryNotInList_whenDelete_then400() throws Exception {
        var list = createAndSaveList(OPEN);
        var entry = createEntry(list);
        persistance.save(entry);

        Response resp =
                deleteResult(list.getUuid(), entry.getUuid(), UUID.randomUUID(), getToken());

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
        assertEquals(ApplicationListEntryResultError.LIST_ENTRY_RESULT_NOT_FOUND.getCode(), resp);
    }

    @Test
    @DisplayName("Delete Application List Entry Result: 400 when entry result not related to entry")
    void givenUnrelatedEntry_whenDelete_then400() throws Exception {
        var list = createAndSaveList(OPEN);

        var unrelatedEntry = createEntry(list);
        persistance.save(unrelatedEntry);

        // create resolution for a DIFFERENT entry
        var entry = createEntry(list);
        var resolutionCode = new ResolutionCodeTestData().someComplete();
        var entryResult = createAndSaveResolution(entry, resolutionCode);

        Response resp =
                deleteResult(
                        list.getUuid(),
                        unrelatedEntry.getUuid(),
                        entryResult.getUuid(),
                        getToken());

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
        assertEquals(ApplicationListEntryResultError.LIST_ENTRY_RESULT_NOT_FOUND.getCode(), resp);
    }

    @Test
    @DisplayName("Delete Application List Entry Result: 412 when If-Match ETag does not match")
    void givenEtagMismatch_whenDelete_then412() throws Exception {
        var list = createAndSaveList(OPEN);
        var entry = createEntry(list);
        persistance.save(entry);

        var resolutionCode = new ResolutionCodeTestData().someComplete();
        var entryResult = createAndSaveResolution(entry, resolutionCode);

        Response resp =
                deleteResult(
                        list.getUuid(),
                        entry.getUuid(),
                        entryResult.getUuid(),
                        getToken(),
                        "never going to match");

        resp.then().statusCode(HttpStatus.PRECONDITION_FAILED.value());
        assertEquals(CommonAppError.MATCH_ETAG_FAILURE.getCode(), resp);
    }

    @Test
    @DisplayName("Delete Application List Entry Result: 400 when list closed")
    void givenClosedList_whenDelete_then409() throws Exception {
        var list = createAndSaveList(CLOSED);

        UUID listId = list.getUuid();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        var token = getToken();

        Response resp = deleteResult(listId, entryId, resultId, token);

        resp.then().statusCode(HttpStatus.CONFLICT.value());
        assertEquals(
                ApplicationListEntryResultError.APPLICATION_LIST_STATE_IS_INCORRECT.getCode(),
                resp);
    }
}
