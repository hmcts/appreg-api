package uk.gov.hmcts.appregister.audit;

import java.util.List;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.audit.service.DataAuditPersistenceQueue;
import uk.gov.hmcts.appregister.audit.service.NestedAuditPersistenceManager;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.TableNames;

@ExtendWith(MockitoExtension.class)
class NestedAuditPersistenceManagerTest {
    @Mock private DataAuditPersistenceQueue dataAuditPersistenceQueue;

    @Test
    void testNonNestedAuditIsSubmittedImmediately() {
        var audit = new DataAudit();
        var manager = new NestedAuditPersistenceManager(dataAuditPersistenceQueue);

        manager.persistOrBuffer(List.of(audit));

        Mockito.verify(dataAuditPersistenceQueue).submit(List.of(audit));
    }

    @Test
    void testNestedAuditIsSubmittedWhenOutermostScopeExits() {
        var audit = new DataAudit();
        Mockito.doAnswer(
                        invocation -> {
                            Assertions.assertEquals(List.of(audit), invocation.getArgument(0));
                            return null;
                        })
                .when(dataAuditPersistenceQueue)
                .submit(Mockito.anyList());
        var manager = new NestedAuditPersistenceManager(dataAuditPersistenceQueue);
        manager.enter();
        manager.enter();
        manager.persistOrBuffer(List.of(audit));

        manager.exit();
        Mockito.verifyNoInteractions(dataAuditPersistenceQueue);

        manager.exit();
        Mockito.verify(dataAuditPersistenceQueue).submit(Mockito.anyList());
    }

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
                .when(dataAuditPersistenceQueue)
                .submit(Mockito.anyList());

        var manager = new NestedAuditPersistenceManager(dataAuditPersistenceQueue);
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
