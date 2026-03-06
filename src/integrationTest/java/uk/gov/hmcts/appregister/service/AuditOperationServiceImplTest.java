package uk.gov.hmcts.appregister.service;

import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.appregister.audit.event.OperationStatus;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.audit.service.AuditOperationServiceImpl;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetSummaryDto;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;

/**
 * A test class that allows us to verify the core audit service.
 */
@Slf4j
public class AuditOperationServiceImplTest extends BaseIntegration {

    @MockitoBean private UserProvider provider;

    @Autowired private DataAuditRepository dataAuditRepository;

    @Autowired private AuditOperationService auditOperationService;

    @BeforeEach
    public void setUp() {
        // setup the trace id in the log MDC
        MDC.put(AuditOperationServiceImpl.TRACE_ID, "test-trace-id");
        when(provider.getUserId()).thenReturn("user");
        when(provider.getEmail()).thenReturn("email");
        when(provider.getRoles()).thenReturn(new String[] {"role"});
    }

    @Test
    void testOldValueAudit() throws Exception {
        final UUID pkId = UUID.randomUUID();

        // run the audit operation
        AppListTestData appListTestData = new AppListTestData();
        ApplicationList applicationList = appListTestData.someComplete();
        applicationList.setUuid(pkId);
        applicationList.setId(20L);

        new TransactionalUnitOfWork()
                .inTransaction(
                        () -> {
                            auditOperationService.processAudit(
                                    applicationList,
                                    TestAuditOperation.TEST_AUDIT_DELETE,
                                    (event) -> {
                                        return Optional.empty();
                                    });
                        });

        // assert that we have logged activity and data audit
        DataAudit dataAudit =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndOldValue(
                                TableNames.APPICATION_LIST, "id", pkId.toString())
                        .get();

        // assert the data audit table
        Assertions.assertNotNull(dataAudit);
        Assertions.assertEquals(pkId.toString(), dataAudit.getOldValue());
        Assertions.assertEquals("email", dataAudit.getCreatedUser());
        Assertions.assertEquals("", dataAudit.getNewValue());
        Assertions.assertEquals("id", dataAudit.getColumnName());
        Assertions.assertEquals("test-trace-id", dataAudit.getLink());
        Assertions.assertEquals(
                TestAuditOperation.TEST_AUDIT_DELETE.type, dataAudit.getUpdateType());
        Assertions.assertEquals(
                TestAuditOperation.TEST_AUDIT_DELETE.eventName, dataAudit.getEventName());
    }

    @Test
    void testNewValueAudit() throws Exception {
        final UUID pkId = UUID.randomUUID();

        // run the audit operation
        AppListTestData appListTestData = new AppListTestData();
        ApplicationList applicationList = appListTestData.someComplete();
        applicationList.setUuid(pkId);
        applicationList.setId(20L);

        ApplicationListGetSummaryDto applicationListGetSummaryDto =
                new ApplicationListGetSummaryDto();
        applicationListGetSummaryDto.setLocation("location");

        new TransactionalUnitOfWork()
                .inTransaction(
                        () -> {
                            auditOperationService.processAudit(
                                    null,
                                    TestAuditOperation.TEST_AUDIT_CREATE,
                                    (event) -> {
                                        return Optional.of(
                                                new AuditableResult<>(
                                                        applicationListGetSummaryDto,
                                                        applicationList));
                                    });
                        });

        // assert that we have logged activity and data audit
        DataAudit dataAudit =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndNewValue(
                                TableNames.APPICATION_LIST, "id", pkId.toString())
                        .get();

        // assert the data audit table
        Assertions.assertNotNull(dataAudit);
        Assertions.assertEquals(pkId.toString(), dataAudit.getNewValue());
        Assertions.assertEquals("email", dataAudit.getCreatedUser());
        Assertions.assertEquals("", dataAudit.getOldValue());
        Assertions.assertEquals("id", dataAudit.getColumnName());
        Assertions.assertEquals("test-trace-id", dataAudit.getLink());
        Assertions.assertEquals(
                TestAuditOperation.TEST_AUDIT_CREATE.type, dataAudit.getUpdateType());
        Assertions.assertEquals(
                TestAuditOperation.TEST_AUDIT_CREATE.eventName, dataAudit.getEventName());

        activityAuditLogAsserter.assertCompletedLogContainsWithUnknownMessageId(
                TestAuditOperation.TEST_AUDIT_CREATE.getEventName(),
                Integer.valueOf(OperationStatus.COMPLETED.getStatus()).toString(),
                mapper.writeValueAsString(applicationListGetSummaryDto));

        // assert the the activity log is entered
        activityAuditLogAsserter.assertCompletedLogContains(
                TestAuditOperation.TEST_AUDIT_CREATE.getEventName(),
                "test-trace-id",
                Integer.valueOf(OperationStatus.COMPLETED.getStatus()).toString(),
                mapper.writeValueAsString(applicationListGetSummaryDto));
    }

    @Test
    void testOldAndNewValueAudit() throws Exception {
        // setup the old data
        final UUID oldPkId = UUID.randomUUID();
        AppListTestData appListTestData = new AppListTestData();
        ApplicationList applicationList = appListTestData.someComplete();
        applicationList.setUuid(oldPkId);
        applicationList.setId(20L);

        // setup the new data
        final UUID newPkId = UUID.randomUUID();
        ApplicationList newApplicationList = appListTestData.someComplete();
        newApplicationList.setUuid(newPkId);
        newApplicationList.setId(20L);

        new TransactionalUnitOfWork()
                .inTransaction(
                        () -> {
                            auditOperationService.processAudit(
                                    applicationList,
                                    TestAuditOperation.TEST_AUDIT_UPDATE,
                                    (event) -> {
                                        return Optional.of(
                                                new AuditableResult<>(null, newApplicationList));
                                    });
                        });

        // assert that we have logged activity and data audit
        DataAudit dataAudit =
                dataAuditRepository
                        .findDataAuditForTableAndColumnAndOldValueAndNewValue(
                                TableNames.APPICATION_LIST,
                                "id",
                                oldPkId.toString(),
                                newPkId.toString())
                        .get();

        // assert that we have logged activity and data audit
        Assertions.assertEquals(oldPkId.toString(), applicationList.getUuid().toString());
        Assertions.assertEquals(newPkId.toString(), dataAudit.getNewValue().toString());
        Assertions.assertEquals("email", dataAudit.getCreatedUser());
        Assertions.assertEquals("id", dataAudit.getColumnName());
        Assertions.assertEquals("test-trace-id", dataAudit.getLink());
        Assertions.assertEquals(
                TestAuditOperation.TEST_AUDIT_UPDATE.type, dataAudit.getUpdateType());
        Assertions.assertEquals(
                TestAuditOperation.TEST_AUDIT_UPDATE.eventName, dataAudit.getEventName());

        Assertions.assertNotNull(dataAudit);
        activityAuditLogAsserter.assertCompletedLogContains(
                TestAuditOperation.TEST_AUDIT_UPDATE.getEventName(),
                "test-trace-id",
                Integer.valueOf(OperationStatus.COMPLETED.getStatus()).toString(),
                "NULL");
    }

    @RequiredArgsConstructor
    @Getter
    public enum TestAuditOperation implements AuditOperation {
        TEST_AUDIT_DELETE("This is a test", CrudEnum.DELETE),
        TEST_AUDIT_UPDATE("This is a test", CrudEnum.UPDATE),
        TEST_AUDIT_CREATE("This is a test", CrudEnum.CREATE);

        private final String eventName;

        private final CrudEnum type;
    }
}
