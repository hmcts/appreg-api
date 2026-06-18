package uk.gov.hmcts.appregister.audit;

import java.util.List;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.audit.service.DataAuditPersistenceService;
import uk.gov.hmcts.appregister.audit.service.NestedAuditPersistenceManager;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.TableNames;

@ExtendWith(MockitoExtension.class)
class NestedAuditPersistenceManagerTest {
    @Mock private DataAuditPersistenceService dataAuditPersistenceService;

    @Test
    void testBufferedAuditSaveFailureLogsEveryBufferedRow() {
        var logCaptor = LogCaptor.forClass(NestedAuditPersistenceManager.class);
        logCaptor.clearLogs();
        var firstAudit = new DataAudit();
        firstAudit.setColumnName("field");
        firstAudit.setTableName(TableNames.APPLICATION_CODES);
        var secondAudit = new DataAudit();
        secondAudit.setColumnName("field1");
        secondAudit.setTableName(TableNames.APPLICATION_CODES);

        Mockito.doThrow(new RuntimeException("audit persistence failed"))
                .when(dataAuditPersistenceService)
                .persist(List.of(firstAudit, secondAudit));

        var manager = new NestedAuditPersistenceManager(dataAuditPersistenceService);
        manager.enter();
        manager.persistOrBuffer(List.of(firstAudit, secondAudit));
        manager.exit();

        var errorLogs = logCaptor.getErrorLogs();
        Assertions.assertTrue(
                errorLogs.stream()
                        .anyMatch(
                                log ->
                                        log.contains("Failed to persist buffered audit field field")
                                                && log.contains(
                                                        "on table "
                                                                + TableNames.APPLICATION_CODES)));
        Assertions.assertTrue(
                errorLogs.stream()
                        .anyMatch(
                                log ->
                                        log.contains(
                                                        "Failed to persist buffered audit field field1")
                                                && log.contains(
                                                        "on table "
                                                                + TableNames.APPLICATION_CODES)));
    }
}
