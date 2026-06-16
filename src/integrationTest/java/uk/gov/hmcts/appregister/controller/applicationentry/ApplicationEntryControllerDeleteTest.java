package uk.gov.hmcts.appregister.controller.applicationentry;

import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;
import uk.gov.hmcts.appregister.testutils.util.DataAuditLogAsserter;
import uk.gov.hmcts.appregister.testutils.util.HeaderUtil;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;
import uk.gov.hmcts.appregister.util.CreateEntryDtoUtil;

class ApplicationEntryControllerDeleteTest extends AbstractApplicationEntryCrudTest {

    @Test
    void givenValidRequest_whenDeleteWithValidId_then204() throws Exception {
        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("test wording");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now(java.time.ZoneOffset.UTC).toString());

        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        String surnameToLookup = UUID.randomUUID().toString();

        entryCreateDto.setWordingFields(List.of(substitution, substitution1));

        var tokenGenerator = createAdminToken();

        entryCreateDto.setLodgementDate(LocalDate.now(java.time.ZoneOffset.UTC).minusDays(1));
        AbstractApplicationEntryCrudTest.SuccessCreateEntryResponse createdDto =
                createEntryWithUniqueSurname(tokenGenerator, entryCreateDto, surnameToLookup);

        Assertions.assertNotNull(HeaderUtil.getETag(createdDto.response()));

        validateEntryCreationResponse(
                entryCreateDto,
                createdDto.getDetailDto(),
                "Application for a warrant to enter premises at {{Premises Address}} for date {{Premises Date}}");

        // delete the entry
        Response responseSpecDelete =
                restAssuredClient.executeDeleteRequest(
                        getLocalUrl(
                                DELETE_ENTRY_CONTEXT.formatted(
                                        createdDto.getDetailDto().getListId(),
                                        createdDto.getDetailDto().getId())),
                        tokenGenerator.fetchTokenForRole());

        responseSpecDelete.then().statusCode(204);

        // assert the auditing works correctly
        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "id",
                        "",
                        null,
                        AppListEntryAuditOperation.DELETE_ENTRY.getType().name(),
                        AppListEntryAuditOperation.DELETE_ENTRY.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "al_id",
                        "",
                        null,
                        AppListEntryAuditOperation.DELETE_ENTRY.getType().name(),
                        AppListEntryAuditOperation.DELETE_ENTRY.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "version",
                        "",
                        null,
                        AppListEntryAuditOperation.DELETE_ENTRY.getType().name(),
                        AppListEntryAuditOperation.DELETE_ENTRY.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "r_na_id",
                        "",
                        null,
                        AppListEntryAuditOperation.DELETE_ENTRY.getType().name(),
                        AppListEntryAuditOperation.DELETE_ENTRY.getEventName()));

        differenceLogAsserter.assertDataAuditChange(
                DataAuditLogAsserter.getDataAuditAssertion(
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "a_na_id",
                        "",
                        null,
                        AppListEntryAuditOperation.DELETE_ENTRY.getType().name(),
                        AppListEntryAuditOperation.DELETE_ENTRY.getEventName()));

        // ensure that we error if we try to delete the entry again
        responseSpecDelete =
                restAssuredClient.executeDeleteRequest(
                        getLocalUrl(
                                DELETE_ENTRY_CONTEXT.formatted(
                                        createdDto.getDetailDto().getListId(),
                                        createdDto.getDetailDto().getId())),
                        tokenGenerator.fetchTokenForRole());

        ProblemAssertUtil.assertEquals(
                AppListEntryError.DELETION_ALREADY_IN_DELETABLE_STATE.getCode(),
                responseSpecDelete);
    }

    @Test
    void givenValidRequest_whenDeleteWithInvalidListId_then404() throws Exception {
        var tokenGenerator = createAdminToken();

        // delete the entry
        Response responseSpecDelete =
                restAssuredClient.executeDeleteRequest(
                        getLocalUrl(
                                DELETE_ENTRY_CONTEXT.formatted(
                                        UUID.randomUUID(), UUID.randomUUID())),
                        tokenGenerator.fetchTokenForRole());

        responseSpecDelete.then().statusCode(404);
        ProblemAssertUtil.assertEquals(
                AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST.getCode(), responseSpecDelete);
    }

    @Test
    void givenValidRequest_whenDeleteWithInvalidEntryId_then404() throws Exception {
        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("test wording");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now(java.time.ZoneOffset.UTC).toString());

        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        String surnameToLookup = UUID.randomUUID().toString();

        entryCreateDto.setWordingFields(List.of(substitution, substitution1));

        var tokenGenerator = createAdminToken();

        entryCreateDto.setLodgementDate(LocalDate.now(java.time.ZoneOffset.UTC).minusDays(1));
        AbstractApplicationEntryCrudTest.SuccessCreateEntryResponse createdDto =
                createEntryWithUniqueSurname(tokenGenerator, entryCreateDto, surnameToLookup);

        Assertions.assertNotNull(HeaderUtil.getETag(createdDto.response()));

        validateEntryCreationResponse(
                entryCreateDto,
                createdDto.getDetailDto(),
                "Application for a warrant to enter premises at {{Premises Address}} for date {{Premises Date}}");

        // delete the entry
        Response responseSpecDelete =
                restAssuredClient.executeDeleteRequest(
                        getLocalUrl(
                                DELETE_ENTRY_CONTEXT.formatted(
                                        createdDto.getDetailDto().getListId(), UUID.randomUUID())),
                        tokenGenerator.fetchTokenForRole());
        responseSpecDelete.then().statusCode(404);
        ProblemAssertUtil.assertEquals(
                AppListEntryError.LIST_ENTRY_NOT_FOUND.getCode(), responseSpecDelete);
    }

    @Test
    void givenValidRequest_whenDeleteWithDeletedEntryId_then409() throws Exception {
        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("test wording");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now(java.time.ZoneOffset.UTC).toString());

        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        String surnameToLookup = UUID.randomUUID().toString();

        entryCreateDto.setWordingFields(List.of(substitution, substitution1));

        var tokenGenerator = createAdminToken();

        entryCreateDto.setLodgementDate(LocalDate.now(java.time.ZoneOffset.UTC).minusDays(1));
        AbstractApplicationEntryCrudTest.SuccessCreateEntryResponse createdDto =
                createEntryWithUniqueSurname(tokenGenerator, entryCreateDto, surnameToLookup);

        int rowsUpdated =
                unitOfWork.inTransaction(
                        () ->
                                applicationListEntryRepository.softDeleteByUuid(
                                        createdDto.getDetailDto().getId()));
        Assertions.assertEquals(1, rowsUpdated);

        Response responseSpecDelete =
                restAssuredClient.executeDeleteRequest(
                        getLocalUrl(
                                DELETE_ENTRY_CONTEXT.formatted(
                                        createdDto.getDetailDto().getListId(),
                                        createdDto.getDetailDto().getId())),
                        tokenGenerator.fetchTokenForRole());
        responseSpecDelete.then().statusCode(409);
        ProblemAssertUtil.assertEquals(
                AppListEntryError.DELETION_ALREADY_IN_DELETABLE_STATE.getCode(),
                responseSpecDelete);
    }

    @Test
    void givenValidRequest_whenDeleteWithEntryNotInList_then409() throws Exception {
        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Premises Address");
        substitution.setValue("test wording");

        TemplateSubstitution substitution1 = new TemplateSubstitution();
        substitution1.setKey("Premises Date");
        substitution1.setValue(LocalDate.now(java.time.ZoneOffset.UTC).toString());

        EntryCreateDto entryCreateDto = CreateEntryDtoUtil.getCorrectCreateEntryDto();

        String surnameToLookup = UUID.randomUUID().toString();

        entryCreateDto.setWordingFields(List.of(substitution, substitution1));

        var tokenGenerator = createAdminToken();

        entryCreateDto.setLodgementDate(LocalDate.now(java.time.ZoneOffset.UTC).minusDays(1));
        AbstractApplicationEntryCrudTest.SuccessCreateEntryResponse createdDto =
                createEntryWithUniqueSurname(tokenGenerator, entryCreateDto, surnameToLookup);

        Assertions.assertNotNull(HeaderUtil.getETag(createdDto.response()));

        validateEntryCreationResponse(
                entryCreateDto,
                createdDto.getDetailDto(),
                "Application for a warrant to enter premises at {{Premises Address}} for date {{Premises Date}}");

        // create a new list directory in the db that we know is not the one we are trying to delete
        // from
        ApplicationList applicationListWithNoEntry = createAndSaveList(Status.OPEN);

        // delete the entry
        Response responseSpecDelete =
                restAssuredClient.executeDeleteRequest(
                        getLocalUrl(
                                DELETE_ENTRY_CONTEXT.formatted(
                                        applicationListWithNoEntry.getUuid(),
                                        createdDto.getDetailDto().getId())),
                        tokenGenerator.fetchTokenForRole());

        responseSpecDelete.then().statusCode(409);
        ProblemAssertUtil.assertEquals(
                AppListEntryError.ENTRY_IS_NOT_WITHIN_LIST.getCode(), responseSpecDelete);
    }
}
