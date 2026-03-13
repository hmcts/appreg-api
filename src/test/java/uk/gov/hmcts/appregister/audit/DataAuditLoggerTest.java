package uk.gov.hmcts.appregister.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.applicationcode.audit.AppCodeAuditOperation;
import uk.gov.hmcts.appregister.applicationlist.audit.AppListAuditOperation;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.event.FailEvent;
import uk.gov.hmcts.appregister.audit.event.StartEvent;
import uk.gov.hmcts.appregister.audit.listener.DataAuditLogger;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditor;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;

@ExtendWith(MockitoExtension.class)
public class DataAuditLoggerTest {
    @Mock private Auditor auditDifferentiator;

    @Mock private DataAuditRepository dataAuditRepository;

    @InjectMocks private DataAuditLogger dataAuditLogger;

    private ArgumentCaptor<DataAudit> auditCaptor = ArgumentCaptor.forClass(DataAudit.class);

    private ArgumentCaptor<DataAudit> auditCaptor2 = ArgumentCaptor.forClass(DataAudit.class);

    @Test
    public void testStartOperationTest() {
        StartEvent startEvent =
                new StartEvent(AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT, "ID", null);

        new DataAuditLogger(auditDifferentiator, dataAuditRepository).eventPerformed(startEvent);

        verify(dataAuditRepository, never()).save(any());
    }

    @Test
    public void testFailOperationTest() {
        ApplicationCodeTestData testData = new ApplicationCodeTestData();
        StartEvent startEvent =
                new StartEvent(
                        AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT,
                        "ID",
                        testData.someComplete());
        FailEvent auditRequest = new FailEvent(startEvent);

        new DataAuditLogger(auditDifferentiator, dataAuditRepository).eventPerformed(auditRequest);

        verify(dataAuditRepository, never()).save(any());
    }

    /** This is a programmatic error as both new and old audit values should NEVER be null. */
    @Test
    public void testFailOldAndNewEntityNull() {
        StartEvent startEvent = new StartEvent(AppListAuditOperation.CREATE_APP_LIST, "ID", null);
        CompleteEvent auditRequest = new CompleteEvent(startEvent, null, null);

        AppRegistryException ex =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                new DataAuditLogger(auditDifferentiator, dataAuditRepository)
                                        .eventPerformed(auditRequest));
        Assertions.assertEquals(CommonAppError.INTERNAL_SERVER_ERROR, ex.getCode());
    }

    /**
     * This is a programmatic error as both new and old audit values should NEVER be different
     * types.
     */
    @Test
    public void testFailOldAndNewEntityDifferentTypes() {
        StartEvent startEvent =
                new StartEvent(AppListAuditOperation.CREATE_APP_LIST, "ID", new ApplicationList());
        CompleteEvent auditRequest = new CompleteEvent(startEvent, null, new ApplicationCode());

        AppRegistryException ex =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                new DataAuditLogger(auditDifferentiator, dataAuditRepository)
                                        .eventPerformed(auditRequest));
        Assertions.assertEquals(CommonAppError.INTERNAL_SERVER_ERROR, ex.getCode());
    }

    /**
     * This is a programmatic error as both new and old audit values should NEVER have different
     * ids.
     */
    @Test
    public void testFailOldAndNewEntityWithDifferentIds() {
        ApplicationList applicationList = new ApplicationList();
        applicationList.setId(1L);

        ApplicationList applicationList2 = new ApplicationList();
        applicationList2.setId(2L);

        StartEvent startEvent =
                new StartEvent(AppListAuditOperation.CREATE_APP_LIST, "ID", applicationList);
        CompleteEvent auditRequest = new CompleteEvent(startEvent, null, applicationList2);

        AppRegistryException ex =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                new DataAuditLogger(auditDifferentiator, dataAuditRepository)
                                        .eventPerformed(auditRequest));
        Assertions.assertEquals(CommonAppError.INTERNAL_SERVER_ERROR, ex.getCode());
    }

    @Test
    public void testSuccessOperationForGetWithDataAuditSaveTest() {
        ApplicationCodeTestData testData = new ApplicationCodeTestData();
        ApplicationCode oldCode = null;
        ApplicationCode newCode = testData.someComplete();

        StartEvent startEvent =
                new StartEvent(
                        AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT, "ID", oldCode);
        CompleteEvent auditRequest = new CompleteEvent(startEvent, null, newCode);
        new DataAuditLogger(auditDifferentiator, dataAuditRepository).eventPerformed(auditRequest);

        // repo was not called as this is a get operation
        verify(dataAuditRepository, never()).save(any());
    }

    @Test
    public void testSuccessOperationForCreateTest() {
        ApplicationCodeTestData testData = new ApplicationCodeTestData();
        ApplicationCode newCode = testData.someComplete();
        Long id = 123L;
        newCode.setId(id);

        StartEvent startEvent = new StartEvent(AppListAuditOperation.CREATE_APP_LIST, "ID", null);
        CompleteEvent auditRequest = new CompleteEvent(startEvent, null, newCode);

        String tableName = TableNames.APPLICATION_CODES;
        String field = "field";
        String newValue = "value";

        String field1 = "field1";
        String newValue2 = "value2";

        when(auditDifferentiator.extractAuditData(CrudEnum.CREATE, newCode))
                .thenReturn(
                        List.of(
                                new AuditableData(tableName, field, newValue),
                                new AuditableData(tableName, field1, newValue2)));
        new DataAuditLogger(auditDifferentiator, dataAuditRepository).eventPerformed(auditRequest);

        // repo was not called as this is a get operation
        verify(dataAuditRepository, times(2)).save(auditCaptor.capture());

        DataAudit dataAudit = auditCaptor.getAllValues().get(0);
        Assertions.assertEquals(id, dataAudit.getRelatedKey());
        Assertions.assertEquals(field, dataAudit.getColumnName());
        Assertions.assertEquals(newValue, dataAudit.getNewValue());
        Assertions.assertEquals(DataAuditLogger.EMPTY_VALUE, dataAudit.getOldValue());
        Assertions.assertEquals(TableNames.APPLICATION_CODES, dataAudit.getTableName());
        Assertions.assertEquals(CrudEnum.CREATE, dataAudit.getUpdateType());

        DataAudit dataAudit1 = auditCaptor.getAllValues().get(1);
        Assertions.assertEquals(id, dataAudit1.getRelatedKey());
        Assertions.assertEquals(field1, dataAudit1.getColumnName());
        Assertions.assertEquals(newValue2, dataAudit1.getNewValue());
        Assertions.assertEquals("", dataAudit1.getOldValue());
        Assertions.assertEquals(TableNames.APPLICATION_CODES, dataAudit1.getTableName());
        Assertions.assertEquals(CrudEnum.CREATE, dataAudit1.getUpdateType());
    }

    @Test
    public void testSuccessOperationForUpdateWhereNewLargerThanOldTest() {
        ApplicationCodeTestData testData = new ApplicationCodeTestData();
        ApplicationCode newCode = testData.someComplete();
        ApplicationCode oldCode = testData.someComplete();

        Long id = 123L;
        newCode.setId(id);
        oldCode.setId(id);

        StartEvent startEvent = new StartEvent(TestAuditOperation.UPDATE, "ID", oldCode);
        CompleteEvent auditRequest = new CompleteEvent(startEvent, null, newCode);

        String tableName = TableNames.APPLICATION_CODES;
        String field = "field";
        String newValue = "value";

        String field1 = "field1";
        String newValue1 = "value2";

        String field2 = "field2";
        String newValue2 = "value2";

        String oneValue = "ovalue";

        String oneValue1 = "ovalue2";

        when(auditDifferentiator.extractAuditData(eq(CrudEnum.UPDATE), refEq(newCode)))
                .thenReturn(
                        List.of(
                                new AuditableData(tableName, field, newValue),
                                new AuditableData(tableName, field1, newValue1),
                                new AuditableData(tableName, field2, newValue2)));

        when(auditDifferentiator.extractAuditData(eq(CrudEnum.UPDATE), refEq(oldCode)))
                .thenReturn(
                        List.of(
                                new AuditableData(tableName, field, oneValue),
                                new AuditableData(tableName, field1, oneValue1)));

        new DataAuditLogger(auditDifferentiator, dataAuditRepository).eventPerformed(auditRequest);

        // repo was not called as this is a get operation
        verify(dataAuditRepository, times(3)).save(auditCaptor.capture());

        DataAudit dataAudit = auditCaptor.getAllValues().get(0);
        Assertions.assertEquals(id, dataAudit.getRelatedKey());
        Assertions.assertEquals(field, dataAudit.getColumnName());
        Assertions.assertEquals(newValue, dataAudit.getNewValue());
        Assertions.assertEquals(oneValue, dataAudit.getOldValue());
        Assertions.assertEquals(TableNames.APPLICATION_CODES, dataAudit.getTableName());
        Assertions.assertEquals(CrudEnum.UPDATE, dataAudit.getUpdateType());

        DataAudit dataAudit1 = auditCaptor.getAllValues().get(1);
        Assertions.assertEquals(id, dataAudit1.getRelatedKey());
        Assertions.assertEquals(field1, dataAudit1.getColumnName());
        Assertions.assertEquals(newValue1, dataAudit1.getNewValue());
        Assertions.assertEquals(oneValue1, dataAudit1.getOldValue());
        Assertions.assertEquals(TableNames.APPLICATION_CODES, dataAudit1.getTableName());
        Assertions.assertEquals(CrudEnum.UPDATE, dataAudit1.getUpdateType());

        DataAudit dataAudit3 = auditCaptor.getAllValues().get(2);
        Assertions.assertEquals(id, dataAudit3.getRelatedKey());
        Assertions.assertEquals(field2, dataAudit3.getColumnName());
        Assertions.assertEquals(newValue2, dataAudit3.getNewValue());
        Assertions.assertEquals("", dataAudit3.getOldValue());
        Assertions.assertEquals(TableNames.APPLICATION_CODES, dataAudit3.getTableName());
        Assertions.assertEquals(CrudEnum.UPDATE, dataAudit3.getUpdateType());
    }

    @Test
    public void testSuccessOperationForUpdateWhereOldLargerThanNewDiffTest() {
        ApplicationCodeTestData testData = new ApplicationCodeTestData();
        ApplicationCode newCode = testData.someComplete();
        ApplicationCode oldCode = testData.someComplete();

        Long id = 123L;
        newCode.setId(id);

        Long id1 = 123L;
        oldCode.setId(id1);

        StartEvent startEvent = new StartEvent(TestAuditOperation.UPDATE, "ID", oldCode);
        CompleteEvent auditRequest = new CompleteEvent(startEvent, null, newCode);

        String tableName = TableNames.APPLICATION_CODES;
        String field = "field";
        String newValue = "value";

        String field1 = "field1";
        String newValue1 = "value2";

        String field2 = "field2";
        String newValue2 = "value2";

        String oneValue = "ovalue";

        String oneValue1 = "ovalue2";

        when(auditDifferentiator.extractAuditData(eq(CrudEnum.UPDATE), refEq(oldCode)))
                .thenReturn(
                        List.of(
                                new AuditableData(tableName, field, newValue),
                                new AuditableData(tableName, field1, newValue1),
                                new AuditableData(tableName, field2, newValue2)));

        when(auditDifferentiator.extractAuditData(eq(CrudEnum.UPDATE), refEq(newCode)))
                .thenReturn(
                        List.of(
                                new AuditableData(tableName, field, oneValue),
                                new AuditableData(tableName, field1, oneValue1)));

        new DataAuditLogger(auditDifferentiator, dataAuditRepository).eventPerformed(auditRequest);

        // repo was not called as this is a get operation
        verify(dataAuditRepository, times(3)).save(auditCaptor.capture());

        DataAudit dataAudit = auditCaptor.getAllValues().get(0);
        Assertions.assertEquals(id, dataAudit.getRelatedKey());
        Assertions.assertEquals(field, dataAudit.getColumnName());
        Assertions.assertEquals(newValue, dataAudit.getOldValue());
        Assertions.assertEquals(oneValue, dataAudit.getNewValue());
        Assertions.assertEquals(TableNames.APPLICATION_CODES, dataAudit.getTableName());
        Assertions.assertEquals(CrudEnum.UPDATE, dataAudit.getUpdateType());

        DataAudit dataAudit1 = auditCaptor.getAllValues().get(1);
        Assertions.assertEquals(id, dataAudit1.getRelatedKey());
        Assertions.assertEquals(field1, dataAudit1.getColumnName());
        Assertions.assertEquals(newValue1, dataAudit1.getOldValue());
        Assertions.assertEquals(oneValue1, dataAudit1.getNewValue());
        Assertions.assertEquals(TableNames.APPLICATION_CODES, dataAudit1.getTableName());
        Assertions.assertEquals(CrudEnum.UPDATE, dataAudit1.getUpdateType());

        DataAudit dataAudit3 = auditCaptor.getAllValues().get(2);
        Assertions.assertEquals(id, dataAudit3.getRelatedKey());
        Assertions.assertEquals(field2, dataAudit3.getColumnName());
        Assertions.assertEquals(newValue2, dataAudit3.getOldValue());
        Assertions.assertEquals("", dataAudit3.getNewValue());
        Assertions.assertEquals(TableNames.APPLICATION_CODES, dataAudit3.getTableName());
        Assertions.assertEquals(CrudEnum.UPDATE, dataAudit3.getUpdateType());
    }

    @Test
    public void testSuccessOperationForSoftDeleteTest() {
        ApplicationCodeTestData testData = new ApplicationCodeTestData();
        ApplicationCode newCode = testData.someComplete();
        ApplicationCode oldCode = testData.someComplete();

        Long id = 123L;
        newCode.setId(id);

        Long id1 = 123L;
        oldCode.setId(id1);

        StartEvent startEvent = new StartEvent(TestAuditOperation.DELETE, "ID", oldCode);
        CompleteEvent auditRequest = new CompleteEvent(startEvent, null, null);

        String tableName = TableNames.APPLICATION_CODES;
        String field = "field";
        String oldValue = "value old";

        String field1 = "field1";
        String oldValue2 = "value old2 ";

        when(auditDifferentiator.extractAuditData(CrudEnum.DELETE, oldCode))
                .thenReturn(
                        List.of(
                                new AuditableData(tableName, field, oldValue),
                                new AuditableData(tableName, field1, oldValue2)));
        new DataAuditLogger(auditDifferentiator, dataAuditRepository).eventPerformed(auditRequest);

        // repo was not called as this is a get operation
        verify(dataAuditRepository, times(2)).save(auditCaptor.capture());
        verify(auditDifferentiator, times(1)).extractAuditData(any(), any());

        DataAudit dataAudit1 = auditCaptor.getAllValues().get(0);
        Assertions.assertEquals(id, dataAudit1.getRelatedKey());
        Assertions.assertEquals(field, dataAudit1.getColumnName());
        Assertions.assertEquals(DataAuditLogger.EMPTY_VALUE, dataAudit1.getNewValue());
        Assertions.assertEquals(oldValue, dataAudit1.getOldValue());
        Assertions.assertEquals(TableNames.APPLICATION_CODES, dataAudit1.getTableName());
        Assertions.assertEquals(CrudEnum.DELETE, dataAudit1.getUpdateType());
    }

    @Test
    public void testSuccessOperationForSoftDeleteTestWithDifferentiatorEntity() {
        ApplicationCodeTestData testData = new ApplicationCodeTestData();
        ApplicationCode newCode = testData.someComplete();
        ApplicationCode oldCode = testData.someComplete();

        Long id = 123L;
        newCode.setId(id);

        Long id1 = 123L;
        oldCode.setId(id1);

        String tableName = TableNames.APPLICATION_CODES;
        String newValue = "value";
        String oldValue = "value old";

        String field1 = "field1";

        Keyable mockOld =
                Mockito.mock(Keyable.class, withSettings().extraInterfaces(Auditable.class));
        Auditable differentiableOld = (Auditable) mockOld;
        when(mockOld.getId()).thenReturn(id1);

        Keyable mockNew =
                Mockito.mock(Keyable.class, withSettings().extraInterfaces(Auditable.class));
        Auditable differentiableNew = (Auditable) mockNew;
        when(mockNew.getId()).thenReturn(id1);

        StartEvent startEvent = new StartEvent(TestAuditOperation.DELETE, "ID", differentiableOld);
        CompleteEvent auditRequest = new CompleteEvent(startEvent, null, differentiableNew);

        when(differentiableNew.extractAuditData(TestAuditOperation.DELETE.getType()))
                .thenReturn(List.of(new AuditableData(tableName, field1, newValue)));

        when(differentiableOld.extractAuditData(TestAuditOperation.DELETE.getType()))
                .thenReturn(List.of(new AuditableData(tableName, field1, oldValue)));
        new DataAuditLogger(auditDifferentiator, dataAuditRepository).eventPerformed(auditRequest);

        // repo was not called as this is a get operation
        verify(dataAuditRepository, times(1)).save(auditCaptor.capture());
        verify(auditDifferentiator, never()).extractAuditData(any(), any());

        DataAudit dataAudit1 = auditCaptor.getAllValues().get(0);
        Assertions.assertEquals(id, dataAudit1.getRelatedKey());
        Assertions.assertEquals(field1, dataAudit1.getColumnName());
        Assertions.assertEquals(newValue, dataAudit1.getNewValue());
        Assertions.assertEquals(oldValue, dataAudit1.getOldValue());
        Assertions.assertEquals(newValue, dataAudit1.getNewValue());
        Assertions.assertEquals(TableNames.APPLICATION_CODES, dataAudit1.getTableName());
        Assertions.assertEquals(CrudEnum.DELETE, dataAudit1.getUpdateType());
    }
}
