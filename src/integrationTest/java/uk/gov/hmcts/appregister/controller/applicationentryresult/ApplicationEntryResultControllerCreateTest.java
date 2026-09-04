package uk.gov.hmcts.appregister.controller.applicationentryresult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.hmcts.appregister.common.enumeration.Status.CLOSED;
import static uk.gov.hmcts.appregister.common.enumeration.Status.OPEN;
import static uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil.assertEquals;

import io.restassured.response.Response;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.applicationentryresult.audit.AppListEntryResultAuditOperation;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.generated.model.BulkResultDto;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;
import uk.gov.hmcts.appregister.generated.model.ResultGetDto;
import uk.gov.hmcts.appregister.generated.model.ResultPage;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;
import uk.gov.hmcts.appregister.testutils.util.TemplateAssertion;

class ApplicationEntryResultControllerCreateTest extends AbstractApplicationEntryResultCrudTest {

    @Test
    @DisplayName("Create Application List Entry Result: 201 when valid request")
    void givenValidRequest_whenCreate_then201() throws Exception {
        val list = createAndSaveList(OPEN);
        val entry = createEntry(list);
        persistance.save(entry);

        val token = getToken();

        val payload =
                buildCreatePayload(
                        APPC_CODE,
                        List.of(
                                new TemplateSubstitution(
                                        APPC_WORDING_KEY, "The Central Criminal Court")));

        // Clear setup rows so the assertions below only inspect the create request under test.
        clearDataAudits(dataAuditRepository);

        // Drive the real endpoint so the result creation passes through validation, persistence
        // and the audit listeners before we inspect DATA_AUDIT directly.
        val resp = createResult(list.getUuid(), entry.getUuid(), token, payload);

        resp.then().statusCode(HttpStatus.CREATED.value());
        resp.then().header(HttpHeaders.LOCATION, notNullValue());
        resp.then().header(HttpHeaders.ETAG, notNullValue());

        resp.then().body("id", notNullValue());
        resp.then().body("entryId", equalTo(entry.getUuid().toString()));
        resp.then().body("resultCode", equalTo(APPC_CODE));
        resp.then().body("updatedDateTime", notNullValue());

        val resultGetDto = resp.as(ResultGetDto.class);

        TemplateAssertion.assertTemplateWithValues(
                "Appeal forwarded to {{Name of Crown Court}}.",
                List.of(new TemplateSubstitution(APPC_WORDING_KEY, "The Central Criminal Court")),
                resultGetDto.getWording());

        val createdResolution =
                appListEntryResolutionRepository
                        .findByUuidAndApplicationList_Uuid(resultGetDto.getId(), entry.getUuid())
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Created AppListEntryResolution could not be reloaded"));
        Assertions.assertEquals(
                createdResolution.getChangedDate(), resultGetDto.getUpdatedDateTime());
        awaitDataAudits();

        // The resolution row itself should record its generated identifier on create.
        val resultIdAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndNewValue(
                                TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                                "aler_id",
                                createdResolution.getId().toString())
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected an app_list_entry_resolutions.aler_id create audit row"));
        Assertions.assertEquals(
                AppListEntryResultAuditOperation.CREATE_APP_LIST_ENTRY_RESULT.getEventName(),
                resultIdAuditRow.getEventName());

        // The owning entry id is stored through the foreign key column ale_ale_id.
        val entryIdAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndNewValue(
                                TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                                "ale_ale_id",
                                createdResolution.getApplicationList().getId().toString())
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected an app_list_entry_resolutions.ale_ale_id create audit row"));
        Assertions.assertEquals(
                AppListEntryResultAuditOperation.CREATE_APP_LIST_ENTRY_RESULT.getEventName(),
                entryIdAuditRow.getEventName());

        // The selected resolution code should be recorded via the rc_rc_id join column.
        val resolutionCodeAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndNewValue(
                                TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                                "rc_rc_id",
                                createdResolution.getResolutionCode().getId().toString())
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected an app_list_entry_resolutions.rc_rc_id create audit row"));
        Assertions.assertEquals(
                AppListEntryResultAuditOperation.CREATE_APP_LIST_ENTRY_RESULT.getEventName(),
                resolutionCodeAuditRow.getEventName());

        // The substituted wording is stored directly on the resolution row.
        val missingWordingAuditMessage =
                "Expected an app_list_entry_resolutions.al_entry_resolution_wording create audit row";
        val wordingAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndNewValue(
                                TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                                "al_entry_resolution_wording",
                                createdResolution.getResolutionWording())
                        .orElseThrow(() -> new AssertionError(missingWordingAuditMessage));
        Assertions.assertEquals(
                AppListEntryResultAuditOperation.CREATE_APP_LIST_ENTRY_RESULT.getEventName(),
                wordingAuditRow.getEventName());

        // The service stamps the acting user into the officer column on create.
        val missingOfficerAuditMessage =
                "Expected an app_list_entry_resolutions.al_entry_resolution_officer create audit row";
        val officerAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndNewValue(
                                TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                                "al_entry_resolution_officer",
                                "email")
                        .orElseThrow(() -> new AssertionError(missingOfficerAuditMessage));
        Assertions.assertEquals(
                AppListEntryResultAuditOperation.CREATE_APP_LIST_ENTRY_RESULT.getEventName(),
                officerAuditRow.getEventName());

        // Version is database-backed and should be written alongside the other create audit rows.
        val missingVersionAuditMessage =
                "Expected an app_list_entry_resolutions.version create audit row";
        val versionAuditRow =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndNewValue(
                                TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                                "version",
                                createdResolution.getVersion().toString())
                        .orElseThrow(() -> new AssertionError(missingVersionAuditMessage));
        Assertions.assertEquals(
                AppListEntryResultAuditOperation.CREATE_APP_LIST_ENTRY_RESULT.getEventName(),
                versionAuditRow.getEventName());
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

        resp.then().statusCode(HttpStatus.NOT_FOUND.value());
        assertEquals(
                ApplicationListEntryResultError.APPLICATION_LIST_DOES_NOT_EXIST.getCode(), resp);
    }

    @Test
    @DisplayName("Create Application List Entry Result: 409 when list closed")
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
    @DisplayName("Create Application List Entry Result: 409 when entry not in list")
    void givenEntryNotInList_whenCreate_then409() throws Exception {
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
                ApplicationListEntryResultError.APPLICATION_ENTRY_NOT_WITHIN_LIST.getCode(), resp);
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

        LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);

        saveActiveResolutionCode("DUP1", today.minusDays(10), null);
        saveActiveResolutionCode("DUP1", today.minusDays(10), today.plusDays(10));

        var token = getToken();
        var payload = buildCreatePayload("DUP1", List.of());

        Response resp = createResult(list.getUuid(), entry.getUuid(), token, payload);

        resp.then().statusCode(HttpStatus.CREATED.value());
        resp.then().body("entryId", equalTo(entry.getUuid().toString()));
        resp.then().body("resultCode", equalTo("DUP1"));

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

        LocalDate date = LocalDate.now(java.time.ZoneOffset.UTC);

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

    private static final String RTC_CODE = "RTC";

    @Test
    void givenAValidBulkResultRequest_whenACallIsMadeWithAListAndTwoEntries_thenSuccessOkResponse()
            throws Exception {
        val list = createAndSaveList(OPEN);
        val entry = createEntry(list);

        // save the data
        persistance.save(entry);

        val entry2 = createEntry(list);

        persistance.save(entry2);

        // create the payload to result 2 entries against the list
        BulkResultDto bulkResultDto = new BulkResultDto();
        bulkResultDto.setEntryIds(List.of(entry.getUuid(), entry2.getUuid()));

        ResultCreateDto createDto = new ResultCreateDto();
        createDto.setResultCode(RTC_CODE);
        createDto.setWordingFields(
                List.of(
                        new TemplateSubstitution("Date", "Date"),
                        new TemplateSubstitution("Courthouse", "ch")));
        bulkResultDto.setResult(createDto);
        clearDataAudits(dataAuditRepository);

        val token = getToken();

        // create the app entry
        Response resp = createBulkResult(list.getUuid(), token, bulkResultDto);

        resp.then().statusCode(HttpStatus.OK.value());

        ResultGetDto[] createdResults = resp.as(ResultGetDto[].class);
        Assertions.assertEquals(2, createdResults.length);

        ResultGetDto createdResult = findResultForEntry(createdResults, entry.getUuid());
        Assertions.assertNotNull(createdResult.getId());
        Assertions.assertNotNull(createdResult.getUpdatedDateTime());
        Assertions.assertEquals(RTC_CODE, createdResult.getResultCode());
        Assertions.assertEquals(
                2, createdResult.getWording().getSubstitutionKeyConstraints().size());
        TemplateAssertion.assertTemplateWithValues(
                "Referred for full court hearing on {{Date}} at {{Courthouse}}.",
                List.of(
                        new TemplateSubstitution("Date", "Date"),
                        new TemplateSubstitution("Courthouse", "ch")),
                createdResult.getWording());

        ResultGetDto createdResult1 = findResultForEntry(createdResults, entry2.getUuid());
        Assertions.assertNotNull(createdResult1.getId());
        Assertions.assertNotNull(createdResult1.getUpdatedDateTime());
        Assertions.assertEquals(RTC_CODE, createdResult1.getResultCode());
        Assertions.assertEquals(
                2, createdResult1.getWording().getSubstitutionKeyConstraints().size());
        TemplateAssertion.assertTemplateWithValues(
                "Referred for full court hearing on {{Date}} at {{Courthouse}}.",
                List.of(
                        new TemplateSubstitution("Date", "Date"),
                        new TemplateSubstitution("Courthouse", "ch")),
                createdResult1.getWording());

        // get the information that we should have created
        Response response = getEntryResult(token, list.getUuid(), entry.getUuid(), 1, 0);

        // now assert the result has been applied against the first entry for the result code
        ResultPage page = response.as(ResultPage.class);
        Assertions.assertEquals(1, page.getContent().size());
        Assertions.assertEquals(createdResult.getId(), page.getContent().getFirst().getId());
        Assertions.assertEquals(entry.getUuid(), page.getContent().getFirst().getEntryId());
        assertThat(page.getContent().getFirst().getUpdatedDateTime())
                .isCloseTo(createdResult.getUpdatedDateTime(), within(1, ChronoUnit.MICROS));
        Assertions.assertEquals(
                2,
                page.getContent().getFirst().getWording().getSubstitutionKeyConstraints().size());
        // assert the template detail
        TemplateAssertion.assertTemplateWithValues(
                "Referred for full court hearing on {{Date}} at {{Courthouse}}.",
                List.of(
                        new TemplateSubstitution("Date", "Date"),
                        new TemplateSubstitution("Courthouse", "ch")),
                page.getContent().getFirst().getWording());

        // get and assert the second entry
        Response response1 = getEntryResult(token, list.getUuid(), entry2.getUuid(), 1, 0);

        // now assert the result has been applied against the entry for the result code
        ResultPage page1 = response1.as(ResultPage.class);
        Assertions.assertEquals(1, page1.getContent().size());
        Assertions.assertEquals(createdResult1.getId(), page1.getContent().getFirst().getId());
        Assertions.assertEquals(entry2.getUuid(), page1.getContent().getFirst().getEntryId());
        Assertions.assertEquals(
                2,
                page1.getContent().getFirst().getWording().getSubstitutionKeyConstraints().size());
        // assert the template detail
        TemplateAssertion.assertTemplateWithValues(
                "Referred for full court hearing on {{Date}} at {{Courthouse}}.",
                List.of(
                        new TemplateSubstitution("Date", "Date"),
                        new TemplateSubstitution("Courthouse", "ch")),
                page1.getContent().getFirst().getWording());

        val createdResolution =
                appListEntryResolutionRepository
                        .findByUuidAndApplicationList_Uuid(
                                page.getContent().get(0).getId(),
                                page.getContent().get(0).getEntryId())
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Created AppListEntryResolution could not be reloaded"));

        val createdResolution1 =
                appListEntryResolutionRepository
                        .findByUuidAndApplicationList_Uuid(
                                page1.getContent().get(0).getId(),
                                page1.getContent().get(0).getEntryId())
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Created AppListEntryResolution could not be reloaded"));

        val bulkAuditRow =
                awaitBulkResultAuditRow(
                        entry.getUuid(),
                        entry2.getUuid(),
                        createdResolution.getUuid(),
                        createdResolution1.getUuid());
        Assertions.assertEquals(
                AppListEntryResultAuditOperation.BULK_CREATE_APP_LIST_ENTRY_RESULT.getEventName(),
                bulkAuditRow.getEventName());
        Assertions.assertTrue(
                valueOrClob(bulkAuditRow.getNewValue(), bulkAuditRow.getNewClobValue())
                        .contains(createdResolution.getResolutionWording()));
        Assertions.assertTrue(
                noPerResultCreateAuditRows(),
                "Expected the bulk path to avoid per-result create audit rows");
    }

    @Test
    void
            givenBulkResultRequestWithDuplicateEntryIds_whenACallIsMadeWithAList_thenFailureBadRequestResponse()
                    throws Exception {
        val list = createAndSaveList(OPEN);
        val entry = createEntry(list);

        persistance.save(entry);

        BulkResultDto bulkResultDto = new BulkResultDto();
        bulkResultDto.setEntryIds(List.of(entry.getUuid(), entry.getUuid()));

        ResultCreateDto createDto = new ResultCreateDto();
        createDto.setResultCode(RTC_CODE);
        createDto.setWordingFields(
                List.of(
                        new TemplateSubstitution("Date", "Date"),
                        new TemplateSubstitution("Courthouse", "ch")));
        bulkResultDto.setResult(createDto);
        clearDataAudits(dataAuditRepository);

        Response resp = createBulkResult(list.getUuid(), getToken(), bulkResultDto);

        ProblemAssertUtil.assertEquals(
                ApplicationListError.ENTRY_IDS_MUST_BE_UNIQUE.getCode(), resp);
    }

    @Test
    void
            givenAValidBulkResultRequest_whenACallIsMadeWithAListThatDoesNotExist_thenFailureConflictResponse()
                    throws Exception {
        // create the payload to result 2 entries against the list
        BulkResultDto bulkResultDto = new BulkResultDto();
        bulkResultDto.setEntryIds(List.of(UUID.randomUUID(), UUID.randomUUID()));

        ResultCreateDto createDto = new ResultCreateDto();
        createDto.setResultCode(RTC_CODE);
        createDto.setWordingFields(
                List.of(
                        new TemplateSubstitution("Date", "Date"),
                        new TemplateSubstitution("Courthouse", "ch")));
        bulkResultDto.setResult(createDto);

        val token = getToken();

        // create the app entry
        Response resp = createBulkResult(UUID.randomUUID(), token, bulkResultDto);

        ProblemAssertUtil.assertEquals(
                ApplicationListEntryResultError.APPLICATION_LIST_DOES_NOT_EXIST.getCode(), resp);
    }

    @Test
    void
            givenAValidBulkResultRequest_whenACallIsMadeWithAEntryThatDoesNotExist_thenFailureConflictResponse()
                    throws Exception {
        val list = createAndSaveList(OPEN);
        val entry = createEntry(list);

        // save the data
        persistance.save(entry);

        val entry2 = createEntry(list);

        persistance.save(entry2);

        // create the payload to result 2 entries against the list
        BulkResultDto bulkResultDto = new BulkResultDto();

        // add an entry that does not exist
        bulkResultDto.setEntryIds(List.of(entry.getUuid(), entry2.getUuid(), UUID.randomUUID()));

        ResultCreateDto createDto = new ResultCreateDto();
        createDto.setResultCode(RTC_CODE);
        createDto.setWordingFields(
                List.of(
                        new TemplateSubstitution("Date", "Date"),
                        new TemplateSubstitution("Courthouse", "ch")));
        bulkResultDto.setResult(createDto);

        val token = getToken();

        // create the app entry
        Response resp = createBulkResult(list.getUuid(), token, bulkResultDto);

        ProblemAssertUtil.assertEquals(
                ApplicationListEntryResultError.APPLICATION_ENTRY_DOES_NOT_EXIST.getCode(), resp);
    }

    @Test
    void
            givenAValidBulkResultRequest_whenACallIsMadeWithToANonExistentResultCode_thenFailureConflictResponse()
                    throws Exception {
        val list = createAndSaveList(OPEN);
        val entry = createEntry(list);

        // save the data
        persistance.save(entry);

        val entry2 = createEntry(list);

        persistance.save(entry2);

        // create the payload to result 2 entries against the list
        BulkResultDto bulkResultDto = new BulkResultDto();
        bulkResultDto.setEntryIds(List.of(entry.getUuid(), entry2.getUuid()));

        ResultCreateDto createDto = new ResultCreateDto();
        createDto.setResultCode("NOTEXIST");
        createDto.setWordingFields(
                List.of(
                        new TemplateSubstitution("Date", "Date"),
                        new TemplateSubstitution("Courthouse", "ch")));
        bulkResultDto.setResult(createDto);

        val token = getToken();

        // create the app entry
        Response resp = createBulkResult(list.getUuid(), token, bulkResultDto);

        ProblemAssertUtil.assertEquals(
                ApplicationListEntryResultError.RESOLUTION_CODE_DOES_NOT_EXIST.getCode(), resp);
    }

    @Test
    void
            givenAValidBulkResultRequest_whenACallIsMadeWithIncorrectTemplateValues_thenFailureConflictResponse()
                    throws Exception {
        val list = createAndSaveList(OPEN);
        val entry = createEntry(list);

        // save the data
        persistance.save(entry);

        val entry2 = createEntry(list);

        persistance.save(entry2);

        // create the payload to result 2 entries against the list
        BulkResultDto bulkResultDto = new BulkResultDto();

        // add an entry that does not exist
        bulkResultDto.setEntryIds(List.of(entry.getUuid(), entry2.getUuid()));

        ResultCreateDto createDto = new ResultCreateDto();
        createDto.setResultCode(RTC_CODE);
        createDto.setWordingFields(List.of(new TemplateSubstitution("Date", "Date")));
        bulkResultDto.setResult(createDto);

        val token = getToken();

        // create the app entry
        Response resp = createBulkResult(list.getUuid(), token, bulkResultDto);
        ProblemAssertUtil.assertEqualsIgnoringDetailLineOrder(
                CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH.getCode(),
                "valueSize=1\n" + "templateSize=2\n",
                resp);
    }

    @Test
    void
            givenAValidBulkResultWithNoListRequest_whenACallIsMadeWithTwoEntries_thenSuccessOkResponse()
                    throws Exception {
        val list = createAndSaveList(OPEN);
        val entry = createEntry(list);

        // save the data
        persistance.save(entry);

        val entry2 = createEntry(list);

        persistance.save(entry2);

        // create the payload to result 2 entries against the list
        BulkResultDto bulkResultDto = new BulkResultDto();
        bulkResultDto.setEntryIds(List.of(entry.getUuid(), entry2.getUuid()));

        ResultCreateDto createDto = new ResultCreateDto();
        createDto.setResultCode(RTC_CODE);
        createDto.setWordingFields(
                List.of(
                        new TemplateSubstitution("Date", "Date"),
                        new TemplateSubstitution("Courthouse", "ch")));
        bulkResultDto.setResult(createDto);

        val token = getToken();

        // create the app entry
        Response resp = createBulkResult(token, bulkResultDto);

        resp.then().statusCode(HttpStatus.OK.value());

        ResultGetDto[] createdResults = resp.as(ResultGetDto[].class);
        Assertions.assertEquals(2, createdResults.length);

        ResultGetDto createdResult = findResultForEntry(createdResults, entry.getUuid());
        Assertions.assertNotNull(createdResult.getId());
        Assertions.assertEquals(RTC_CODE, createdResult.getResultCode());
        Assertions.assertEquals(
                2, createdResult.getWording().getSubstitutionKeyConstraints().size());
        TemplateAssertion.assertTemplateWithValues(
                "Referred for full court hearing on {{Date}} at {{Courthouse}}.",
                List.of(
                        new TemplateSubstitution("Date", "Date"),
                        new TemplateSubstitution("Courthouse", "ch")),
                createdResult.getWording());

        ResultGetDto createdResult1 = findResultForEntry(createdResults, entry2.getUuid());
        Assertions.assertNotNull(createdResult1.getId());
        Assertions.assertEquals(RTC_CODE, createdResult1.getResultCode());
        Assertions.assertEquals(
                2, createdResult1.getWording().getSubstitutionKeyConstraints().size());
        TemplateAssertion.assertTemplateWithValues(
                "Referred for full court hearing on {{Date}} at {{Courthouse}}.",
                List.of(
                        new TemplateSubstitution("Date", "Date"),
                        new TemplateSubstitution("Courthouse", "ch")),
                createdResult1.getWording());

        // get the information that we should have created
        Response response = getEntryResult(token, list.getUuid(), entry.getUuid(), 1, 0);

        // now assert the result has been applied against the first entry for the result code
        ResultPage page = response.as(ResultPage.class);
        Assertions.assertEquals(1, page.getContent().size());
        Assertions.assertEquals(createdResult.getId(), page.getContent().getFirst().getId());
        Assertions.assertEquals(entry.getUuid(), page.getContent().getFirst().getEntryId());
        Assertions.assertEquals(
                2,
                page.getContent().getFirst().getWording().getSubstitutionKeyConstraints().size());
        // assert the template detail
        TemplateAssertion.assertTemplateWithValues(
                "Referred for full court hearing on {{Date}} at {{Courthouse}}.",
                List.of(
                        new TemplateSubstitution("Date", "Date"),
                        new TemplateSubstitution("Courthouse", "ch")),
                page.getContent().getFirst().getWording());

        // get and assert the second entry
        Response response1 = getEntryResult(token, list.getUuid(), entry2.getUuid(), 1, 0);

        // now assert the result has been applied against the entry for the result code
        ResultPage page1 = response1.as(ResultPage.class);
        Assertions.assertEquals(1, page1.getContent().size());
        Assertions.assertEquals(createdResult1.getId(), page1.getContent().getFirst().getId());
        Assertions.assertEquals(entry2.getUuid(), page1.getContent().getFirst().getEntryId());
        Assertions.assertEquals(
                2,
                page1.getContent().getFirst().getWording().getSubstitutionKeyConstraints().size());
        // assert the template detail
        TemplateAssertion.assertTemplateWithValues(
                "Referred for full court hearing on {{Date}} at {{Courthouse}}.",
                List.of(
                        new TemplateSubstitution("Date", "Date"),
                        new TemplateSubstitution("Courthouse", "ch")),
                page1.getContent().getFirst().getWording());

        val createdResolution =
                appListEntryResolutionRepository
                        .findByUuidAndApplicationList_Uuid(
                                page.getContent().get(0).getId(),
                                page.getContent().get(0).getEntryId())
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Created AppListEntryResolution could not be reloaded"));

        val createdResolution1 =
                appListEntryResolutionRepository
                        .findByUuidAndApplicationList_Uuid(
                                page1.getContent().get(0).getId(),
                                page1.getContent().get(0).getEntryId())
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Created AppListEntryResolution could not be reloaded"));

        val bulkAuditRow =
                awaitBulkResultAuditRow(
                        entry.getUuid(),
                        entry2.getUuid(),
                        createdResolution.getUuid(),
                        createdResolution1.getUuid());
        Assertions.assertEquals(
                AppListEntryResultAuditOperation.BULK_CREATE_APP_LIST_ENTRY_RESULT.getEventName(),
                bulkAuditRow.getEventName());
        Assertions.assertTrue(
                valueOrClob(bulkAuditRow.getNewValue(), bulkAuditRow.getNewClobValue())
                        .contains(createdResolution.getResolutionWording()));
        Assertions.assertTrue(
                noPerResultCreateAuditRows(),
                "Expected the bulk path to avoid per-result create audit rows");
    }

    private DataAudit awaitBulkResultAuditRow(
            UUID entryId, UUID entryId1, UUID resultId, UUID resultId1) {
        awaitDataAudits();
        for (int attempt = 0; attempt < 20; attempt++) {
            for (var auditRow : dataAuditRepository.findAll()) {
                if (!AppListEntryResultAuditOperation.BULK_CREATE_APP_LIST_ENTRY_RESULT
                        .getEventName()
                        .equals(auditRow.getEventName())) {
                    continue;
                }

                if (!TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS.equals(auditRow.getTableName())
                        || !"bulk_results_created".equals(auditRow.getColumnName())) {
                    continue;
                }

                var auditValue = valueOrClob(auditRow.getNewValue(), auditRow.getNewClobValue());
                if (auditValue.contains(entryId.toString())
                        && auditValue.contains(entryId1.toString())
                        && auditValue.contains(resultId.toString())
                        && auditValue.contains(resultId1.toString())) {
                    return auditRow;
                }
            }
        }

        throw new AssertionError("Expected a bulk result audit row with created result details");
    }

    private boolean noPerResultCreateAuditRows() {
        awaitDataAudits();
        return dataAuditRepository.findAll().stream()
                .noneMatch(
                        auditRow ->
                                AppListEntryResultAuditOperation.CREATE_APP_LIST_ENTRY_RESULT
                                                .getEventName()
                                                .equals(auditRow.getEventName())
                                        && TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS.equals(
                                                auditRow.getTableName()));
    }

    private String valueOrClob(String value, String clobValue) {
        return value != null ? value : clobValue;
    }

    @Test
    void
            givenBulkResultWithNoListRequest_whenACallIsMadeWithAEntryThatDoesNotExist_thenFailureConflictResponse()
                    throws Exception {
        val list = createAndSaveList(OPEN);
        val entry = createEntry(list);

        // save the data
        persistance.save(entry);

        val entry2 = createEntry(list);

        persistance.save(entry2);

        // create the payload to result 2 entries against the list
        BulkResultDto bulkResultDto = new BulkResultDto();

        // add an entry that does not exist
        bulkResultDto.setEntryIds(List.of(entry.getUuid(), entry2.getUuid(), UUID.randomUUID()));

        ResultCreateDto createDto = new ResultCreateDto();
        createDto.setResultCode(RTC_CODE);
        createDto.setWordingFields(
                List.of(
                        new TemplateSubstitution("Date", "Date"),
                        new TemplateSubstitution("Courthouse", "ch")));
        bulkResultDto.setResult(createDto);

        val token = getToken();

        // create the app entry
        Response resp = createBulkResult(token, bulkResultDto);

        ProblemAssertUtil.assertEquals(
                ApplicationListEntryResultError.APPLICATION_ENTRIES_NOT_ALL_EXIST.getCode(), resp);
    }

    @Test
    void
            givenBulkResultWithNoListRequest_whenACallIsMadeWithToANonExistentResultCode_thenFailureConflictResponse()
                    throws Exception {
        val list = createAndSaveList(OPEN);
        val entry = createEntry(list);

        // save the data
        persistance.save(entry);

        val entry2 = createEntry(list);

        persistance.save(entry2);

        // create the payload to result 2 entries against the list
        BulkResultDto bulkResultDto = new BulkResultDto();
        bulkResultDto.setEntryIds(List.of(entry.getUuid(), entry2.getUuid()));

        ResultCreateDto createDto = new ResultCreateDto();
        createDto.setResultCode("NOTEXIST");
        createDto.setWordingFields(
                List.of(
                        new TemplateSubstitution("Date", "Date"),
                        new TemplateSubstitution("Courthouse", "ch")));
        bulkResultDto.setResult(createDto);

        val token = getToken();

        // create the app entry
        Response resp = createBulkResult(token, bulkResultDto);

        ProblemAssertUtil.assertEquals(
                ApplicationListEntryResultError.RESOLUTION_CODE_DOES_NOT_EXIST.getCode(), resp);
    }

    @Test
    void
            givenBulkResultWithNoListRequest_whenACallIsMadeWithIncorrectTemplateValues_thenFailureConflictResponse()
                    throws Exception {
        val list = createAndSaveList(OPEN);
        val entry = createEntry(list);

        // save the data
        persistance.save(entry);

        val entry2 = createEntry(list);

        persistance.save(entry2);

        // create the payload to result 2 entries against the list
        BulkResultDto bulkResultDto = new BulkResultDto();

        // add an entry that does not exist
        bulkResultDto.setEntryIds(List.of(entry.getUuid(), entry2.getUuid()));

        ResultCreateDto createDto = new ResultCreateDto();
        createDto.setResultCode(RTC_CODE);
        createDto.setWordingFields(List.of(new TemplateSubstitution("Date", "Date")));
        bulkResultDto.setResult(createDto);

        val token = getToken();

        // create the app entry
        Response resp = createBulkResult(token, bulkResultDto);
        ProblemAssertUtil.assertEqualsIgnoringDetailLineOrder(
                CommonAppError.WORDING_SUBSTITUTE_SIZE_MISMATCH.getCode(),
                "valueSize=1\n" + "templateSize=2\n",
                resp);
    }

    private static ResultGetDto findResultForEntry(ResultGetDto[] createdResults, UUID entryId) {
        return Arrays.stream(createdResults)
                .filter(result -> entryId.equals(result.getEntryId()))
                .findFirst()
                .orElseThrow(
                        () -> new AssertionError("Expected result response for entry " + entryId));
    }
}
