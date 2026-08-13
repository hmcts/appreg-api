package uk.gov.hmcts.appregister.controller.applicationentryresult;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.appregister.common.enumeration.Status.OPEN;
import static uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.applicationentryresult.audit.AppListEntryResultAuditOperation;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.data.ResolutionCodeTestData;
import uk.gov.hmcts.appregister.generated.model.BulkDeleteResultItemDto;
import uk.gov.hmcts.appregister.generated.model.BulkDeleteResultsDto;

class ApplicationEntryResultControllerBulkDeleteTest
        extends AbstractApplicationEntryResultCrudTest {

    @Test
    void givenValidRequest_whenBulkDelete_thenDeletesAllResultsAndWritesSingleBulkAudit()
            throws Exception {
        var list = createAndSaveList(OPEN);
        var firstEntry = createEntry(list);
        var secondEntry = createEntry(list);
        persistance.save(firstEntry);
        persistance.save(secondEntry);

        var firstResult =
                createAndSaveResolution(firstEntry, new ResolutionCodeTestData().someComplete());
        var secondResult =
                createAndSaveResolution(secondEntry, new ResolutionCodeTestData().someComplete());

        clearDataAudits(dataAuditRepository);

        deleteBulkResult(
                        getToken(),
                        new BulkDeleteResultsDto()
                                .results(
                                        List.of(
                                                requestItem(
                                                        list.getUuid(),
                                                        firstEntry.getUuid(),
                                                        firstResult.getUuid()),
                                                requestItem(
                                                        list.getUuid(),
                                                        secondEntry.getUuid(),
                                                        secondResult.getUuid()))))
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        assertThat(appListEntryResolutionRepository.findByUuid(firstResult.getUuid())).isEmpty();
        assertThat(appListEntryResolutionRepository.findByUuid(secondResult.getUuid())).isEmpty();

        var bulkAuditRow =
                awaitBulkDeleteAuditRow(
                        firstResult.getUuid(), secondResult.getUuid(), firstEntry.getUuid());
        assertThat(bulkAuditRow.getEventName())
                .isEqualTo(
                        AppListEntryResultAuditOperation.BULK_DELETE_APP_LIST_ENTRY_RESULT
                                .getEventName());
        assertThat(valueOrClob(bulkAuditRow.getNewValue(), bulkAuditRow.getNewClobValue()))
                .contains(firstResult.getUuid().toString(), secondResult.getUuid().toString());
        assertThat(noPerResultDeleteAuditRows()).isTrue();
    }

    @Test
    void givenInvalidRequest_whenBulkDelete_thenRollsBackEverything() throws Exception {
        var list = createAndSaveList(OPEN);
        var entry = createEntry(list);
        persistance.save(entry);

        var result = createAndSaveResolution(entry, new ResolutionCodeTestData().someComplete());

        var response =
                deleteBulkResult(
                        getToken(),
                        new BulkDeleteResultsDto()
                                .results(
                                        List.of(
                                                requestItem(
                                                        list.getUuid(),
                                                        entry.getUuid(),
                                                        result.getUuid()),
                                                requestItem(
                                                        list.getUuid(),
                                                        entry.getUuid(),
                                                        UUID.randomUUID()))));

        response.then().statusCode(HttpStatus.NOT_FOUND.value());
        assertEquals(
                ApplicationListEntryResultError.APPLICATION_ENTRY_RESULT_DOES_NOT_EXIST.getCode(),
                response);

        assertThat(appListEntryResolutionRepository.findByUuid(result.getUuid())).isPresent();
        awaitDataAudits();
        assertThat(
                        dataAuditRepository.findAll().stream()
                                .noneMatch(
                                        row ->
                                                AppListEntryResultAuditOperation
                                                        .BULK_DELETE_APP_LIST_ENTRY_RESULT
                                                        .getEventName()
                                                        .equals(row.getEventName())))
                .isTrue();
    }

    private BulkDeleteResultItemDto requestItem(UUID listId, UUID entryId, UUID resultId) {
        return new BulkDeleteResultItemDto().listId(listId).entryId(entryId).resultId(resultId);
    }

    private DataAudit awaitBulkDeleteAuditRow(UUID resultId, UUID resultId1, UUID entryId) {
        awaitDataAudits();
        for (int attempt = 0; attempt < 10; attempt++) {
            Optional<DataAudit> auditRow =
                    dataAuditRepository.findAll().stream()
                            .filter(
                                    audit ->
                                            TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS.equals(
                                                            audit.getTableName())
                                                    && "bulk_delete_results"
                                                            .equals(audit.getColumnName())
                                                    && AppListEntryResultAuditOperation
                                                            .BULK_DELETE_APP_LIST_ENTRY_RESULT
                                                            .getEventName()
                                                            .equals(audit.getEventName()))
                            .filter(
                                    audit -> {
                                        var auditValue =
                                                valueOrClob(
                                                        audit.getNewValue(),
                                                        audit.getNewClobValue());
                                        return auditValue.contains(resultId.toString())
                                                && auditValue.contains(resultId1.toString())
                                                && auditValue.contains(entryId.toString());
                                    })
                            .findFirst();
            if (auditRow.isPresent()) {
                return auditRow.get();
            }
        }

        throw new AssertionError("Expected a bulk delete audit row with deleted result details");
    }

    private boolean noPerResultDeleteAuditRows() {
        awaitDataAudits();
        return dataAuditRepository.findAll().stream()
                .noneMatch(
                        auditRow ->
                                AppListEntryResultAuditOperation.DELETE_APP_LIST_ENTRY_RESULT
                                                .getEventName()
                                                .equals(auditRow.getEventName())
                                        && TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS.equals(
                                                auditRow.getTableName()));
    }

    private String valueOrClob(String value, String clobValue) {
        return value != null ? value : clobValue;
    }
}
