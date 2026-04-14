package uk.gov.hmcts.appregister.controller.applicationentryresult;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.hmcts.appregister.common.enumeration.Status.CLOSED;
import static uk.gov.hmcts.appregister.common.enumeration.Status.OPEN;
import static uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil.assertEquals;

import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.applicationentryresult.audit.AppListEntryResultAuditOperation;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.util.EtagUtil;
import uk.gov.hmcts.appregister.generated.model.ResultGetDto;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;
import uk.gov.hmcts.appregister.testutils.util.DataAuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.TemplateAssertion;

public class ApplicationEntryResultControllerUpdateTest
        extends AbstractApplicationEntryResultCrudTest {

    @Test
    @DisplayName("Update Application List Entry Result: 200 when valid request + If-Match matches")
    void givenValidRequest_whenUpdate_then200() throws Exception {
        var existingEntry = givenExistingEntry();

        var createPayload =
                buildCreatePayload(
                        APPC_CODE,
                        List.of(
                                new TemplateSubstitution(
                                        APPC_WORDING_KEY, "Central Criminal Court")));

        Response createResp =
                createResult(
                        existingEntry.list().getUuid(),
                        existingEntry.entry().getUuid(),
                        existingEntry.token(),
                        createPayload);

        createResp.then().statusCode(HttpStatus.CREATED.value());
        createResp.then().header(HttpHeaders.ETAG, notNullValue());

        UUID resultUuid = UUID.fromString(createResp.jsonPath().getString("id"));
        String currentEtag = createResp.getHeader(HttpHeaders.ETAG);

        var updatePayload =
                buildUpdatePayload(
                        FRO_CODE,
                        List.of(
                                new TemplateSubstitution(
                                        FRO_WORDING_KEY, "Caseworker discretion")));

        Response updateResp =
                updateResult(
                        existingEntry.list().getUuid(),
                        existingEntry.entry().getUuid(),
                        resultUuid,
                        existingEntry.token(),
                        updatePayload,
                        currentEtag);

        updateResp.then().statusCode(HttpStatus.OK.value());
        updateResp.then().header(HttpHeaders.LOCATION, notNullValue());
        updateResp.then().header(HttpHeaders.ETAG, notNullValue());

        updateResp.then().body("id", equalTo(resultUuid.toString()));
        updateResp.then().body("entryId", equalTo(existingEntry.entry().getUuid().toString()));
        updateResp.then().body("resultCode", equalTo(FRO_CODE));

        ResultGetDto resultGetDto = updateResp.as(ResultGetDto.class);
        TemplateAssertion.assertTemplateWithValues(
                "Fee remitted. Reason: {{Reason text}}.",
                updatePayload.getWordingFields(),
                resultGetDto.getWording());

        differenceLogAsserter.assertNoErrors();

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                        "al_entry_resolution_wording",
                        null,
                        null,
                        AppListEntryResultAuditOperation.UPDATE_APP_LIST_ENTRY_RESULT
                                .getType()
                                .name(),
                        AppListEntryResultAuditOperation.UPDATE_APP_LIST_ENTRY_RESULT
                                .getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                        "al_entry_resolution_officer",
                        null,
                        null,
                        AppListEntryResultAuditOperation.UPDATE_APP_LIST_ENTRY_RESULT
                                .getType()
                                .name(),
                        AppListEntryResultAuditOperation.UPDATE_APP_LIST_ENTRY_RESULT
                                .getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                        "version",
                        null,
                        null,
                        AppListEntryResultAuditOperation.UPDATE_APP_LIST_ENTRY_RESULT
                                .getType()
                                .name(),
                        AppListEntryResultAuditOperation.UPDATE_APP_LIST_ENTRY_RESULT
                                .getEventName()));
    }

    @Test
    @DisplayName("Update Application List Entry Result: 409 when list unknown")
    void givenUnknownList_whenUpdate_thenError() throws Exception {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();

        var payload = buildUpdatePayload(APPC_CODE, List.of());

        Response resp =
                updateResult(listId, entryId, resultId, getToken(), payload, "\"any-etag\"");

        resp.then().statusCode(HttpStatus.CONFLICT.value());
        assertEquals(
                ApplicationListEntryResultError.APPLICATION_LIST_DOES_NOT_EXIST.getCode(), resp);
    }

    @Test
    @DisplayName("Update Application List Entry Result: 409 when list closed")
    void givenClosedList_whenUpdate_then400() throws Exception {
        var existingResult = givenExistingEntryResult(CLOSED);

        var payload = buildUpdatePayload(APPC_CODE, List.of());

        Response resp =
                updateResult(
                        existingResult.list().getUuid(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        existingResult.token(),
                        payload,
                        "\"any-etag\"");

        resp.then().statusCode(HttpStatus.CONFLICT.value());
        assertEquals(
                ApplicationListEntryResultError.APPLICATION_LIST_STATE_IS_INCORRECT.getCode(),
                resp);
    }

    @Test
    @DisplayName("Update Application List Entry Result: 409 when entry not in list")
    void givenEntryNotInList_whenUpdate_then409() throws Exception {
        var existingResult = givenExistingEntryResult();

        var otherList = createAndSaveList(OPEN);
        var entryInOtherList = createEntry(otherList);
        persistance.save(entryInOtherList);

        var payload = buildUpdatePayload(APPC_CODE, List.of());

        Response resp =
                updateResult(
                        existingResult.list().getUuid(),
                        entryInOtherList.getUuid(),
                        existingResult.entryResult().getUuid(),
                        existingResult.token(),
                        payload,
                        "\"any-etag\"");

        resp.then().statusCode(HttpStatus.CONFLICT.value());
        assertEquals(
                ApplicationListEntryResultError.APPLICATION_ENTRY_DOES_NOT_EXIST.getCode(), resp);
    }

    @Test
    @DisplayName("Update Application List Entry Result: 404 when resolution code unknown")
    void givenUnknownResolutionCode_whenUpdate_then400() throws Exception {
        var existingResult = givenExistingEntryResult();

        String currentEtag = EtagUtil.generateEtag(List.of(existingResult.entryResult()));

        var payload = buildUpdatePayload("UNKNOWN", List.of());

        Response resp =
                updateResult(
                        existingResult.list().getUuid(),
                        existingResult.entry().getUuid(),
                        existingResult.entryResult().getUuid(),
                        existingResult.token(),
                        payload,
                        currentEtag);

        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        assertEquals(
                ApplicationListEntryResultError.RESOLUTION_CODE_DOES_NOT_EXIST.getCode(), resp);
    }

    @Test
    @DisplayName(
            "Update Application List Entry Result: prefers active ResolutionCode with endDate NULL")
    void givenMultipleActiveResolutionCodes_whenUpdate_thenPrefersNullEndDate() throws Exception {
        var existingResult = givenExistingEntryResult();
        String currentEtag = EtagUtil.generateEtag(List.of(existingResult.entryResult()));
        LocalDate today = LocalDate.now();

        saveActiveResolutionCode("DUP1", today.minusDays(10), null);
        saveActiveResolutionCode("DUP1", today.minusDays(10), today.plusDays(10));

        var payload = buildUpdatePayload("DUP1", List.of());

        Response resp =
                updateResult(
                        existingResult.list().getUuid(),
                        existingResult.entry().getUuid(),
                        existingResult.entryResult().getUuid(),
                        existingResult.token(),
                        payload,
                        currentEtag);

        resp.then().statusCode(HttpStatus.OK.value());
        resp.then().body("resultCode", equalTo("DUP1"));

        var saved =
                appListEntryResolutionRepository
                        .findByUuidAndApplicationList_Uuid(
                                existingResult.entryResult().getUuid(),
                                existingResult.entry().getUuid())
                        .orElseThrow(() -> new AssertionError("Updated result not found"));

        var preferredId =
                resolutionCodeRepository
                        .findActiveResolutionCodesByCodeAndDate("DUP1", today)
                        .stream()
                        .filter(rc -> rc.getEndDate() == null)
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected active ResolutionCode with null endDate not found"))
                        .getId();

        Assertions.assertEquals(preferredId, saved.getResolutionCode().getId());
    }

    @Test
    @DisplayName("Update Application List Entry Result: 412 when If-Match ETag does not match")
    void givenEtagMismatch_whenUpdate_then412() throws Exception {
        var existingResult = givenExistingEntryResult();

        var payload = buildUpdatePayload(FRO_CODE, List.of());

        Response resp =
                updateResult(
                        existingResult.list().getUuid(),
                        existingResult.entry().getUuid(),
                        existingResult.entryResult().getUuid(),
                        existingResult.token(),
                        payload,
                        "never going to match");

        resp.then().statusCode(HttpStatus.PRECONDITION_FAILED.value());
        assertEquals(CommonAppError.MATCH_ETAG_FAILURE.getCode(), resp);
    }
}
