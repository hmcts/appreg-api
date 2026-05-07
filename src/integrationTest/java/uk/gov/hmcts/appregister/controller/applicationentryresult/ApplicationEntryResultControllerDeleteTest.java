package uk.gov.hmcts.appregister.controller.applicationentryresult;

import static uk.gov.hmcts.appregister.common.enumeration.Status.CLOSED;
import static uk.gov.hmcts.appregister.common.enumeration.Status.OPEN;
import static uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil.assertEquals;

import io.restassured.response.Response;
import java.util.List;
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.applicationentryresult.audit.AppListEntryResultAuditOperation;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.util.EtagUtil;
import uk.gov.hmcts.appregister.data.ResolutionCodeTestData;

public class ApplicationEntryResultControllerDeleteTest
        extends AbstractApplicationEntryResultCrudTest {

    @Test
    @DisplayName("Delete Application List Entry Result: 204 when valid IDs")
    void givenValidIds_whenDelete_then204() throws Exception {
        val list = createAndSaveList(OPEN);
        val entry = createEntry(list);
        persistance.save(entry);

        val resolutionCode = new ResolutionCodeTestData().someComplete();
        val entryResult = createAndSaveResolution(entry, resolutionCode);

        val token = getToken();

        // compute the current server-style ETag
        val expectedEtag = EtagUtil.generateEtag(List.of(entryResult));

        // Remove the setup rows so the assertions below only examine the delete request.
        dataAuditRepository.deleteAll();

        // Call the real endpoint so the delete runs through etag checks, repository delete and
        // the audit listeners before we inspect DATA_AUDIT.
        val resp =
                deleteResult(
                        list.getUuid(),
                        entry.getUuid(),
                        entryResult.getUuid(),
                        token,
                        expectedEtag);

        resp.then().statusCode(HttpStatus.NO_CONTENT.value());

        // Delete audit should include the generated resolution identifier so the removed row can
        // be tied back to the original database record.
        val resultIdAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndOldValue(
                                TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                                "aler_id",
                                entryResult.getId().toString())
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected an app_list_entry_resolutions.aler_id delete audit row"));
        Assertions.assertEquals(
                AppListEntryResultAuditOperation.DELETE_APP_LIST_ENTRY_RESULT.getEventName(),
                resultIdAuditRow.getEventName());

        // The owning entry id should also be written on delete now that ale_ale_id has delete
        // audit coverage.
        val entryIdAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndOldValue(
                                TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                                "ale_ale_id",
                                entry.getId().toString())
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected an app_list_entry_resolutions.ale_ale_id delete audit row"));
        Assertions.assertEquals(
                AppListEntryResultAuditOperation.DELETE_APP_LIST_ENTRY_RESULT.getEventName(),
                entryIdAuditRow.getEventName());

        // The resolution code foreign key is part of the deleted row and should be captured too.
        val resolutionCodeAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndOldValue(
                                TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                                "rc_rc_id",
                                entryResult.getResolutionCode().getId().toString())
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected an app_list_entry_resolutions.rc_rc_id delete audit row"));
        Assertions.assertEquals(
                AppListEntryResultAuditOperation.DELETE_APP_LIST_ENTRY_RESULT.getEventName(),
                resolutionCodeAuditRow.getEventName());

        // Delete audit should preserve the wording that was on the row before it was removed.
        val missingWordingAuditMessage =
                "Expected an app_list_entry_resolutions.al_entry_resolution_wording delete audit row";
        val wordingAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndOldValue(
                                TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                                "al_entry_resolution_wording",
                                entryResult.getResolutionWording())
                        .orElseThrow(() -> new AssertionError(missingWordingAuditMessage));
        Assertions.assertEquals(
                AppListEntryResultAuditOperation.DELETE_APP_LIST_ENTRY_RESULT.getEventName(),
                wordingAuditRow.getEventName());

        // Officer is another database-backed field on the removed row and should be persisted in
        // the audit trail.
        val missingOfficerAuditMessage =
                "Expected an app_list_entry_resolutions.al_entry_resolution_officer delete audit row";
        val officerAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndOldValue(
                                TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                                "al_entry_resolution_officer",
                                entryResult.getResolutionOfficer())
                        .orElseThrow(() -> new AssertionError(missingOfficerAuditMessage));
        Assertions.assertEquals(
                AppListEntryResultAuditOperation.DELETE_APP_LIST_ENTRY_RESULT.getEventName(),
                officerAuditRow.getEventName());

        // Version is the final lifecycle field we expect to retain when the row is deleted.
        val versionAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndOldValue(
                                TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                                "version",
                                entryResult.getVersion().toString())
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected an app_list_entry_resolutions.version delete audit row"));
        Assertions.assertEquals(
                AppListEntryResultAuditOperation.DELETE_APP_LIST_ENTRY_RESULT.getEventName(),
                versionAuditRow.getEventName());
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
