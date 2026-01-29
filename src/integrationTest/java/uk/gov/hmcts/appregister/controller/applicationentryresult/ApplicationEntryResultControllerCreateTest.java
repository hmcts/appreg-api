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
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

public class ApplicationEntryResultControllerCreateTest
        extends AbstractApplicationEntryResultCrudTest {

    @Test
    @DisplayName("Create Application List Entry Result: 201 when valid request")
    void givenValidRequest_whenCreate_then201() throws Exception {
        var list = createAndSaveList(OPEN);
        var entry = createEntry(list);
        persistance.save(entry);

        var token = getToken();

        var payload =
                buildCreatePayload(
                        APPC_CODE,
                        List.of(
                                new TemplateSubstitution(
                                        APPC_WORDING_KEY, "The Central Criminal Court")));

        Response resp = createResult(list.getUuid(), entry.getUuid(), token, payload);

        resp.then().statusCode(HttpStatus.CREATED.value());
        resp.then().header(HttpHeaders.LOCATION, notNullValue());
        resp.then().header(HttpHeaders.ETAG, notNullValue());

        resp.then().body("id", notNullValue());
        resp.then().body("entryId", equalTo(entry.getUuid().toString()));
        resp.then().body("resultCode", equalTo(APPC_CODE));
        resp.then().body("wordingFields", equalTo(List.of(APPC_WORDING_KEY)));
    }

    @Test
    @DisplayName("Create Application List Entry Result: 404 when list unknown")
    void givenUnknownList_whenCreate_then404() throws Exception {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();

        var token = getToken();

        var payload =
                buildCreatePayload(
                        APPC_CODE,
                        List.of(new TemplateSubstitution(APPC_WORDING_KEY, "test wording")));

        Response resp = createResult(listId, entryId, token, payload);

        resp.then().statusCode(HttpStatus.CONFLICT.value());
        assertEquals(
                ApplicationListEntryResultError.APPLICATION_LIST_DOES_NOT_EXIST.getCode(), resp);
    }

    @Test
    @DisplayName("Create Application List Entry Result: 400 when list closed")
    void givenClosedList_whenCreate_then400() throws Exception {
        var list = createAndSaveList(CLOSED);

        var token = getToken();

        var payload =
                buildCreatePayload(
                        APPC_CODE,
                        List.of(new TemplateSubstitution(APPC_WORDING_KEY, "test wording")));

        Response resp = createResult(list.getUuid(), UUID.randomUUID(), token, payload);

        resp.then().statusCode(HttpStatus.CONFLICT.value());
        assertEquals(
                ApplicationListEntryResultError.APPLICATION_LIST_STATE_IS_INCORRECT.getCode(),
                resp);
    }

    @Test
    @DisplayName("Create Application List Entry Result: 400 when entry not in list")
    void givenEntryNotInList_whenCreate_then400() throws Exception {
        var list = createAndSaveList(OPEN);
        var list2 = createAndSaveList(OPEN);

        var entry = createEntry(list2);
        persistance.save(entry);

        var token = getToken();

        var payload =
                buildCreatePayload(
                        APPC_CODE,
                        List.of(new TemplateSubstitution(APPC_WORDING_KEY, "test wording")));

        Response resp = createResult(list.getUuid(), entry.getUuid(), token, payload);

        resp.then().statusCode(HttpStatus.CONFLICT.value());
        assertEquals(
                ApplicationListEntryResultError.APPLICATION_ENTRY_DOES_NOT_EXIST.getCode(), resp);
    }

    @Test
    @DisplayName("Create Application List Entry Result: 400 when resolution code unknown")
    void givenUnknownResolutionCode_whenCreate_then400() throws Exception {
        var list = createAndSaveList(OPEN);
        var entry = createEntry(list);
        persistance.save(entry);

        var token = getToken();

        var payload =
                buildCreatePayload(
                        "UNKNOWN",
                        List.of(new TemplateSubstitution(APPC_WORDING_KEY, "test wording")));

        Response resp = createResult(list.getUuid(), entry.getUuid(), token, payload);

        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        assertEquals(
                ApplicationListEntryResultError.RESOLUTION_CODE_DOES_NOT_EXIST.getCode(), resp);
    }

    @Test
    @DisplayName(
            "Create Application List Entry Result: prefers active ResolutionCode with endDate NULL")
    void givenMultipleActiveResolutionCodes_whenCreate_thenPrefersNullEndDate() throws Exception {
        var list = createAndSaveList(OPEN);
        var entry = createEntry(list);
        persistance.save(entry);

        LocalDate today = LocalDate.now();

        saveActiveResolutionCode("DUP1", today.minusDays(10), null);
        saveActiveResolutionCode("DUP1", today.minusDays(10), today.plusDays(10));

        var token = getToken();
        var payload = buildCreatePayload("DUP1", List.of());

        Response resp = createResult(list.getUuid(), entry.getUuid(), token, payload);

        resp.then().statusCode(HttpStatus.CREATED.value());
        resp.then().body("entryId", equalTo(entry.getUuid().toString()));
        resp.then().body("resultCode", equalTo("DUP1"));
        resp.then().body("wordingFields", equalTo(List.of()));

        UUID resultUuid = UUID.fromString(resp.jsonPath().getString("id"));

        var saved =
                appListEntryResolutionRepository
                        .findByUuidAndApplicationList_Uuid(resultUuid, entry.getUuid())
                        .orElseThrow(
                                () -> new AssertionError("Saved AppListEntryResolution not found"));

        Assertions.assertNotNull(saved.getResolutionCode(), "resolutionCode should be set");

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

        Assertions.assertEquals(
                preferredId,
                saved.getResolutionCode().getId(),
                "Should prefer the ResolutionCode with null endDate");
    }

    @Test
    @DisplayName(
            "Create Application List Entry Result: when no endDate NULL exists, chooses latest endDate")
    void givenMultipleActiveWithoutNullEndDate_whenCreate_thenChoosesLatestEndDate()
            throws Exception {
        var list = createAndSaveList(OPEN);
        var entry = createEntry(list);
        persistance.save(entry);

        LocalDate date = LocalDate.now();

        var older = saveActiveResolutionCode("DUP2", date.minusDays(10), date.plusDays(5));
        var latest = saveActiveResolutionCode("DUP2", date.minusDays(10), date.plusDays(20));

        var token = getToken();
        var payload = buildCreatePayload("DUP2", List.of());

        Response resp = createResult(list.getUuid(), entry.getUuid(), token, payload);

        resp.then().statusCode(HttpStatus.CREATED.value());

        UUID createdId = UUID.fromString(resp.jsonPath().getString("id"));

        AppListEntryResolution created =
                appListEntryResolutionRepository
                        .findByUuidAndApplicationList_Uuid(createdId, entry.getUuid())
                        .orElseThrow(
                                () -> new AssertionError("Saved AppListEntryResolution not found"));

        Long chosenResolutionCodeId = created.getResolutionCode().getId();

        Assertions.assertEquals(
                latest.getId(),
                chosenResolutionCodeId,
                "Should choose the ResolutionCode with the latest endDate");

        Assertions.assertNotEquals(
                older.getId(),
                chosenResolutionCodeId,
                "Should not choose the older ResolutionCode");
    }
}
