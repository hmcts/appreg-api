package uk.gov.hmcts.appregister.applicationentry.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

class BulkImportAuditTest {

    @Test
    void givenCompletedImport_whenExtractingAuditData_thenReturnsJobSummary() {
        UUID listId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        var audit = new BulkImportAudit(listId, jobId, 25);

        assertThat(audit.getId()).isNull();
        assertThat(audit.extractAuditData(CrudEnum.CREATE))
                .containsExactly(
                        new AuditableData(
                                TableNames.APPLICATION_LISTS_ENTRY,
                                "application_list_id",
                                listId.toString()),
                        new AuditableData(
                                TableNames.APPLICATION_LISTS_ENTRY,
                                "bulk_import_job_id",
                                jobId.toString()),
                        new AuditableData(
                                TableNames.APPLICATION_LISTS_ENTRY, "imported_entry_count", "25"));
    }
}
