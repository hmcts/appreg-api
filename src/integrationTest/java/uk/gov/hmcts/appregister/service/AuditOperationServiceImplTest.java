package uk.gov.hmcts.appregister.service;

import lombok.Builder;
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
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.testutils.util.ActivityAuditLogAsserter;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;

/**
 * A test class that allows us to verify the core audit service.
 */
@Slf4j
public class AuditOperationServiceImplTest extends BaseIntegration {

    @MockitoBean
    private UserProvider provider;

    @Autowired
    private DataAuditRepository dataAuditRepository;

    @Autowired
    private AuditOperationService auditOperationService;

    private ActivityAuditLogAsserter asserter = new ActivityAuditLogAsserter();

    @BeforeEach
    public void setUp() {
        // setup the trace id in the log MDC
        MDC.put(AuditOperationServiceImpl.TRACE_ID, "test-trace-id");
        when(provider.getUserId()).thenReturn("user");
        when(provider.getEmail()).thenReturn("email");
        when(provider.getRoles()).thenReturn(new String[]{"role"});
    }

    @Test
    void testOldValueAudit() throws Exception {
        final UUID pkId = UUID.randomUUID();

        // run the audit operation
        AppListTestData appListTestData = new AppListTestData();
        ApplicationList applicationList = appListTestData.someComplete();
        applicationList.setUuid(pkId);
        applicationList.setId(20L);

        new TransactionalUnitOfWork().inTransaction(() -> {
            auditOperationService.processAudit(
                applicationList,
                TestAuditOperation.TEST_AUDIT_DELETE,
                (event) -> {
                    return Optional.empty();
                }
            );
        });

        // assert that we have logged activity and data audit
        DataAudit dataAudit = dataAuditRepository
            .findDataAuditForTableAndColumnAndOldValue(
                TableNames.APPICATION_LIST,
                "id",
                pkId.toString()
            ).get();
        Assertions.assertNotNull(dataAudit);

        // assert the the activity log is entered
        activityAuditLogAsserter.assertCompletedLogContains(
            TestAuditOperation
                .TEST_AUDIT_DELETE.getEventName(),
            "test-trace-id",
            Integer.valueOf(OperationStatus
                                .COMPLETED.getStatus()).toString(),
            "NULL"
        );
    }

    @Test
    void testNewValueAudit() throws Exception {
        final UUID pkId = UUID.randomUUID();

        // run the audit operation
        AppListTestData appListTestData = new AppListTestData();
        ApplicationList applicationList = appListTestData.someComplete();
        applicationList.setUuid(pkId);
        applicationList.setId(20L);

        ApplicationCodeTestData applicationCodeTestData = new ApplicationCodeTestData();
        ApplicationCode applicationCode = applicationCodeTestData.someComplete();

        new TransactionalUnitOfWork().inTransaction(() -> {
            auditOperationService.processAudit(
                null,
                TestAuditOperation.TEST_AUDIT_CREATE,
                (event) -> {
                    return Optional.of(new AuditableResult<>(
                        applicationCode,
                        applicationList
                    ));
                }
            );
        });

        // assert that we have logged activity and data audit
        DataAudit dataAudit = dataAuditRepository
            .findDataAuditForTableAndColumnAndNewValue(
                TableNames.APPICATION_LIST,
                "id",
                pkId.toString()
            ).get();
        Assertions.assertNotNull(dataAudit);

        activityAuditLogAsserter
            .assertCompletedLogContainsWithUnknownMessageId(
                TestAuditOperation
                    .TEST_AUDIT_DELETE.getEventName(),
                Integer.valueOf(OperationStatus
                                    .COMPLETED.getStatus()).toString(),
                "\\{\"name\":\"testName\"\\}");

        // assert the the activity log is entered
        activityAuditLogAsserter.assertCompletedLogContains(
            TestAuditOperation
                .TEST_AUDIT_DELETE.getEventName(),
            "test-trace-id",
            Integer.valueOf(OperationStatus
                                .COMPLETED.getStatus()).toString(),
            "{\"name\":\"testName\"}"
        );
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

        new TransactionalUnitOfWork().inTransaction(() -> {
            auditOperationService.processAudit(
                applicationList,
                TestAuditOperation.TEST_AUDIT_UPDATE,
                (event) -> {
                    return Optional.of(new AuditableResult<>(null, newApplicationList));
                }
            );
        });

        // assert that we have logged activity and data audit
        Assertions.assertEquals(oldPkId, applicationList.getUuid());

        // assert that the data audit is entered
        DataAudit dataAudit = dataAuditRepository
            .findDataAuditForTableAndColumnAndOldValueAndNewValue(
                TableNames.APPICATION_LIST,
                "id",
                oldPkId.toString(),
                newPkId.toString()
            ).get();

        Assertions.assertNotNull(dataAudit);
        activityAuditLogAsserter.assertCompletedLogContains(
            TestAuditOperation
                .TEST_AUDIT_UPDATE.getEventName(),
            "test-trace-id",
            Integer.valueOf(OperationStatus
                                .COMPLETED.getStatus()).toString(),
            "NULL"
        );
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

