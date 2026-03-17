package uk.gov.hmcts.appregister.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.appregister.common.enumeration.Status.CLOSED;
import static uk.gov.hmcts.appregister.common.enumeration.Status.OPEN;
import static uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil.assertEquals;

import io.restassured.response.Response;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.appregister.applicationentryresult.audit.AppListEntryResultAuditOperation;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.base.TableNames;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.common.util.EtagUtil;
import uk.gov.hmcts.appregister.data.AppListEntryResolutionTestData;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.data.ResolutionCodeTestData;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;
import uk.gov.hmcts.appregister.testutils.controller.AbstractSecurityControllerTest;
import uk.gov.hmcts.appregister.testutils.controller.RestEndpointDescription;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;
import uk.gov.hmcts.appregister.testutils.util.DataAuditLogAsserter;

public class ApplicationEntryResultControllerTest extends AbstractSecurityControllerTest {

    @MockitoBean private UserProvider provider;

    @Autowired private AppListEntryResolutionRepository appListEntryResolutionRepository;

    @Autowired private ResolutionCodeRepository resolutionCodeRepository;

    private static final String WEB_CONTEXT = "application-lists";
    private static final String APPC_CODE = "APPC";
    private static final String WORDING_KEY = "Name of Crown Court";

    @BeforeEach
    public void before() {
        when(provider.getUserId()).thenReturn("user");
        when(provider.getEmail()).thenReturn("email");
        when(provider.getRoles()).thenReturn(new String[] {"role"});
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Delete Application List Entry Result: 204 when valid IDs")
    void givenValidIds_whenDelete_then204() throws Exception {
        var list = createAndSaveList(OPEN);
        var entry = createEntry(list);
        persistance.save(entry);

        var resolutionCode = new ResolutionCodeTestData().someComplete();
        var entryResult = createAndSaveResolution(entry, resolutionCode);

        var token = getToken();

        // --- compute the server-side ETag for the entity so we can match it in the request ---
        // EtagUtil.generateEtag expects a List<Keyable> just like the service uses.
        String expectedEtag = EtagUtil.generateEtag(List.of(entryResult));

        // fire test (supply correct If-Match header so delete passes the match)
        Response resp =
                deleteResult(
                        list.getUuid(),
                        entry.getUuid(),
                        entryResult.getUuid(),
                        token,
                        expectedEtag);

        // assert success
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
    @DisplayName("Delete Application List Entry Result: 409 when list unknown")
    void givenUnknownList_whenDelete_then404() throws Exception {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        var token = getToken();

        Response resp = deleteResult(listId, entryId, resultId, token);

        resp.then().statusCode(HttpStatus.CONFLICT.value());
        assertEquals(
                ApplicationListEntryResultError.APPLICATION_LIST_DOES_NOT_EXIST.getCode(), resp);
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

    @Test
    @DisplayName("Delete Application List Entry Result: 400 when entry not in list")
    void givenEntryNotInList_whenDelete_then400() throws Exception {
        var list = createAndSaveList(OPEN);
        var entry = createEntry(list);
        persistance.save(entry);

        UUID listId = list.getUuid();
        UUID entryId = entry.getUuid();
        UUID resultId = UUID.randomUUID();
        var token = getToken();

        Response resp = deleteResult(listId, entryId, resultId, token);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
        assertEquals(ApplicationListEntryResultError.LIST_ENTRY_RESULT_NOT_FOUND.getCode(), resp);
    }

    @Test
    @DisplayName("Delete Application List Entry Result: 400 when entry result not related to entry")
    void givenUnrelatedEntry_whenDelete_then400() throws Exception {
        var list = createAndSaveList(OPEN);
        var unrelatedEntry = createEntry(list);
        persistance.save(unrelatedEntry);
        var entry = createEntry(list);

        var resolutionCode = new ResolutionCodeTestData().someComplete();
        var entryResult = createAndSaveResolution(entry, resolutionCode);

        UUID listId = list.getUuid();
        UUID entryId = unrelatedEntry.getUuid();
        UUID resultId = entryResult.getUuid();
        var token = getToken();

        Response resp = deleteResult(listId, entryId, resultId, token);

        resp.then().statusCode(HttpStatus.BAD_REQUEST.value());
        assertEquals(ApplicationListEntryResultError.LIST_ENTRY_RESULT_NOT_FOUND.getCode(), resp);
    }

    @Test
    @DisplayName("Delete Application List Entry Result: 412 when If-Match ETag does not match")
    void givenEtagMismatch_whenDelete_then412() throws Exception {
        // arrange - create list, entry and a saved resolution (same pattern as other tests)
        var list = createAndSaveList(OPEN);
        var entry = createEntry(list);
        persistance.save(entry);

        var resolutionCode = new ResolutionCodeTestData().someComplete();
        var entryResult = createAndSaveResolution(entry, resolutionCode);

        var token = getToken();

        // act - call delete with an ETag value that will never match the generated ETag
        Response resp =
                deleteResult(
                        list.getUuid(),
                        entry.getUuid(),
                        entryResult.getUuid(),
                        token,
                        "never going to match");

        // assert - server should reject the request with 412 and MATCH_ETAG_FAILURE problem code
        resp.then().statusCode(HttpStatus.PRECONDITION_FAILED.value());
        assertEquals(CommonAppError.MATCH_ETAG_FAILURE.getCode(), resp);
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

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
                                        WORDING_KEY, "The Central Criminal Court")));

        Response resp = createResult(list.getUuid(), entry.getUuid(), token, payload);

        resp.then().statusCode(HttpStatus.CREATED.value());
        resp.then().header(HttpHeaders.LOCATION, notNullValue());
        resp.then().header(HttpHeaders.ETAG, notNullValue());

        resp.then().body("id", notNullValue());
        resp.then().body("entryId", equalTo(entry.getUuid().toString()));
        resp.then().body("resultCode", equalTo(APPC_CODE));

        resp.then().body("wordingFields", equalTo(List.of("Name of Crown Court")));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                        "version",
                        null,
                        null,
                        AppListEntryResultAuditOperation.CREATE_APP_LIST_ENTRY_RESULT
                                .getType()
                                .name(),
                        AppListEntryResultAuditOperation.CREATE_APP_LIST_ENTRY_RESULT
                                .getEventName()));
    }

    @Test
    @DisplayName("Create Application List Entry Result: 409 when list unknown")
    void givenUnknownList_whenCreate_then409() throws Exception {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();

        var token = getToken();

        var payload =
                buildCreatePayload(
                        APPC_CODE, List.of(new TemplateSubstitution(WORDING_KEY, "test wording")));

        Response resp = createResult(listId, entryId, token, payload);

        resp.then().statusCode(HttpStatus.CONFLICT.value());
        assertEquals(
                ApplicationListEntryResultError.APPLICATION_LIST_DOES_NOT_EXIST.getCode(), resp);
    }

    @Test
    @DisplayName("Create Application List Entry Result: 409 when list closed")
    void givenClosedList_whenCreate_then409() throws Exception {
        var list = createAndSaveList(CLOSED);

        var token = getToken();

        var payload =
                buildCreatePayload(
                        APPC_CODE, List.of(new TemplateSubstitution(WORDING_KEY, "test wording")));

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
                        APPC_CODE, List.of(new TemplateSubstitution(WORDING_KEY, "test wording")));

        Response resp = createResult(list.getUuid(), entry.getUuid(), token, payload);

        resp.then().statusCode(HttpStatus.CONFLICT.value());
        assertEquals(
                ApplicationListEntryResultError.APPLICATION_ENTRY_DOES_NOT_EXIST.getCode(), resp);
    }

    @Test
    @DisplayName("Create Application List Entry Result: 404 when resolution code unknown")
    void givenUnknownResolutionCode_whenCreate_then404() throws Exception {
        var list = createAndSaveList(OPEN);
        var entry = createEntry(list);
        persistance.save(entry);

        var token = getToken();

        var payload =
                buildCreatePayload(
                        "UNKNOWN", List.of(new TemplateSubstitution(WORDING_KEY, "test wording")));

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
        resp.then().body("wordingFields", equalTo(List.of())); // expect empty

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
                                                "Expected active ResolutionCode with null endDate"
                                                        + "not found"))
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

    // -------------------------------------------------------------------------
    // ENDPOINT DESCRIPTIONS (SECURITY)
    // -------------------------------------------------------------------------

    @Override
    protected Stream<RestEndpointDescription> getDescriptions() throws Exception {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();

        Map<String, Object> postPayload =
                Map.of(
                        "resultCode", "SOME_CODE",
                        "resolutionWording", "Some wording");

        return Stream.of(
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrl(
                                        WEB_CONTEXT
                                                + "/"
                                                + listId
                                                + "/entries/"
                                                + entryId
                                                + "/results"))
                        .method(HttpMethod.POST)
                        .payload(postPayload)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build(),
                RestEndpointDescription.builder()
                        .url(
                                getLocalUrl(
                                        WEB_CONTEXT
                                                + "/"
                                                + listId
                                                + "/entries/"
                                                + entryId
                                                + "/results/"
                                                + resultId))
                        .method(HttpMethod.DELETE)
                        .successRole(RoleEnum.USER)
                        .successRole(RoleEnum.ADMIN)
                        .build());
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private Response deleteResult(UUID listId, UUID entryId, UUID resultId, TokenAndJwksKey token)
            throws MalformedURLException {
        return restAssuredClient.executeDeleteRequest(
                getLocalUrl(
                        WEB_CONTEXT
                                + "/"
                                + listId
                                + "/entries/"
                                + entryId
                                + "/results/"
                                + resultId),
                token);
    }

    private Response deleteResult(
            UUID listId, UUID entryId, UUID resultId, TokenAndJwksKey token, String ifMatch)
            throws MalformedURLException {
        return restAssuredClient.executeDeleteRequest(
                getLocalUrl(
                        WEB_CONTEXT
                                + "/"
                                + listId
                                + "/entries/"
                                + entryId
                                + "/results/"
                                + resultId),
                token,
                ifMatch);
    }

    private ApplicationList createAndSaveList(Status status) {
        var list = new AppListTestData().someMinimal().status(status).build();
        persistance.save(list);
        return list;
    }

    private ApplicationListEntry createEntry(ApplicationList list) {
        return new AppListEntryTestData().someMinimal().applicationList(list).build();
    }

    private AppListEntryResolution createAndSaveResolution(
            ApplicationListEntry entry, ResolutionCode resolutionCode) {
        var resolution =
                new AppListEntryResolutionTestData()
                        .someMinimal()
                        .resolutionWording(AppListEntryResolutionTestData.WORDING_1)
                        .applicationList(entry)
                        .resolutionCode(resolutionCode)
                        .build();
        return persistance.save(resolution);
    }

    private Response createResult(UUID listId, UUID entryId, TokenAndJwksKey token, Object body)
            throws MalformedURLException {

        return restAssuredClient.executePostRequest(
                getLocalUrl(WEB_CONTEXT + "/" + listId + "/entries/" + entryId + "/results"),
                token,
                body);
    }

    private ResultCreateDto buildCreatePayload(
            String resultCode, List<TemplateSubstitution> wordingFields) {
        return new ResultCreateDto(resultCode, wordingFields);
    }

    private ResolutionCode saveActiveResolutionCode(String code, LocalDate start, LocalDate end) {
        ResolutionCode rc = new ResolutionCodeTestData().someComplete();
        rc.setResultCode(code);
        rc.setStartDate(start);
        rc.setEndDate(end);
        return persistance.save(rc);
    }
}
