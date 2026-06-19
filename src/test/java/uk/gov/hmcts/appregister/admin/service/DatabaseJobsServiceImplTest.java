package uk.gov.hmcts.appregister.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.admin.mapper.DatabaseJobsMapper;
import uk.gov.hmcts.appregister.admin.mapper.DatabaseJobsMapperImpl;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.service.AuditOperationServiceImpl;
import uk.gov.hmcts.appregister.common.entity.DatabaseJob;
import uk.gov.hmcts.appregister.common.entity.RetentionPolicy;
import uk.gov.hmcts.appregister.common.entity.repository.DatabaseJobRepository;
import uk.gov.hmcts.appregister.common.entity.repository.RetentionPolicyRepository;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.generated.model.AdminJobType;
import uk.gov.hmcts.appregister.generated.model.JobRetentionPolicy;

@Slf4j
@ExtendWith(MockitoExtension.class)
class DatabaseJobsServiceImplTest {
    private static final OffsetDateTime LAST_RAN = OffsetDateTime.parse("2025-01-02T03:04:05Z");

    private AdminAPIServiceImpl service;

    @Mock private DatabaseJobRepository databaseJobRepository;
    @Mock private RetentionPolicyRepository retentionPolicyRepository;

    @Spy private final DatabaseJobsMapper mapper = new DatabaseJobsMapperImpl();

    @BeforeEach
    void setUp() {

        service =
                new AdminAPIServiceImpl(
                        databaseJobRepository,
                        retentionPolicyRepository,
                        mapper,
                        new AuditOperationServiceImpl(List.of()));
    }

    @Test
    void testGetDatabaseJobStatusByName() {
        val testJob = new DatabaseJob();
        testJob.setName(AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue());
        testJob.setLastRan(LAST_RAN);
        testJob.setId(1L);
        testJob.setEnabled(YesOrNo.YES);

        when(databaseJobRepository.findByName(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue()))
                .thenReturn(testJob);
        service =
                new AdminAPIServiceImpl(
                        databaseJobRepository,
                        retentionPolicyRepository,
                        mapper,
                        new AuditOperationServiceImpl(List.of()));

        val status =
                service.getDatabaseJobStatusByName(AdminJobType.APPLICATION_LISTS_DATABASE_JOB);

        assertNotNull(status);
        assertNotNull(status.getLastRan());
        assertEquals(LAST_RAN, status.getLastRan());
        assertEquals(true, status.getEnabled());
    }

    @Test
    void testEnableDatabaseJobByName() {
        val testJob = new DatabaseJob();
        testJob.setName(AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue());
        testJob.setLastRan(LAST_RAN);
        testJob.setId(2L);
        testJob.setEnabled(YesOrNo.NO);

        when(databaseJobRepository.findByName(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue()))
                .thenReturn(testJob);

        service.enableDisableDatabaseJobByName(AdminJobType.APPLICATION_LISTS_DATABASE_JOB, true);

        val status =
                service.getDatabaseJobStatusByName(AdminJobType.APPLICATION_LISTS_DATABASE_JOB);
        assertNotNull(status);
        assertEquals(true, status.getEnabled());
    }

    @Test
    void testUpdateDatabaseJobRetentionPeriodByName() {
        var retentionPolicyEntity = new RetentionPolicy();
        retentionPolicyEntity.setConfigValue("1825");
        when(retentionPolicyRepository.countByJobNameAndConfigKey(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue(),
                        "RETENTION_PERIOD_DAYS"))
                .thenReturn(1L);
        when(retentionPolicyRepository.findByJobNameAndConfigKeyOrderByIdAsc(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue(),
                        "RETENTION_PERIOD_DAYS"))
                .thenReturn(List.of(retentionPolicyEntity));

        service.updateDatabaseJobRetentionPeriodByName(
                AdminJobType.APPLICATION_LISTS_DATABASE_JOB, 365);
        assertEquals("365", retentionPolicyEntity.getConfigValue());
    }

    @Test
    void testGetDatabaseJobRetentionPeriodByName() {
        var retentionPolicy = new RetentionPolicy();
        retentionPolicy.setConfigValue("1825");
        when(retentionPolicyRepository.countByJobNameAndConfigKey(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue(),
                        "RETENTION_PERIOD_DAYS"))
                .thenReturn(1L);
        when(retentionPolicyRepository.findByJobNameAndConfigKeyOrderByIdAsc(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue(),
                        "RETENTION_PERIOD_DAYS"))
                .thenReturn(List.of(retentionPolicy));

        JobRetentionPolicy retentionPolicyResponse =
                service.getDatabaseJobRetentionPeriodByName(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB);

        assertNotNull(retentionPolicyResponse);
        assertEquals(Integer.valueOf(1825), retentionPolicyResponse.getRetentionPeriodDays());
    }

    @Test
    void testGetDatabaseJobRetentionPeriodByName_whenMissingConfig_throwsException() {
        when(retentionPolicyRepository.countByJobNameAndConfigKey(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue(),
                        "RETENTION_PERIOD_DAYS"))
                .thenReturn(0L);
        when(retentionPolicyRepository.findByJobNameAndConfigKeyOrderByIdAsc(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue(),
                        "RETENTION_PERIOD_DAYS"))
                .thenReturn(List.of());

        var exception =
                assertThrows(
                        AppRegistryException.class,
                        () ->
                                service.getDatabaseJobRetentionPeriodByName(
                                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB));

        assertEquals(CommonAppError.INTERNAL_SERVER_ERROR, exception.getCode());
        assertEquals(
                "Retention configuration RETENTION_PERIOD_DAYS was not found for"
                        + " administrative job APPLICATION_LISTS_DATABASE_JOB",
                exception.getMessage());
    }

    @Test
    void testUpdateDatabaseJobRetentionPeriodByName_whenMissingConfig_throwsException() {
        when(retentionPolicyRepository.countByJobNameAndConfigKey(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue(),
                        "RETENTION_PERIOD_DAYS"))
                .thenReturn(0L);
        when(retentionPolicyRepository.findByJobNameAndConfigKeyOrderByIdAsc(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue(),
                        "RETENTION_PERIOD_DAYS"))
                .thenReturn(List.of());

        var exception =
                assertThrows(
                        AppRegistryException.class,
                        () ->
                                service.updateDatabaseJobRetentionPeriodByName(
                                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB, 365));

        assertEquals(CommonAppError.INTERNAL_SERVER_ERROR, exception.getCode());
        assertEquals(
                "Retention configuration RETENTION_PERIOD_DAYS was not found for"
                        + " administrative job APPLICATION_LISTS_DATABASE_JOB",
                exception.getMessage());
    }

    @Test
    void testGetDatabaseJobStatusByName_auditsRequestedJobType() {
        val testJob = new DatabaseJob();
        testJob.setName(AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue());
        testJob.setEnabled(YesOrNo.YES);

        when(databaseJobRepository.findByName(any())).thenReturn(testJob);

        val listener = new CapturingAuditListener();
        service =
                new AdminAPIServiceImpl(
                        databaseJobRepository,
                        retentionPolicyRepository,
                        mapper,
                        new AuditOperationServiceImpl(List.of(listener)));

        // Execute the same service method used by the controller and capture the completed audit
        // event so we can inspect the surrogate entity sent to data audit.
        val status =
                service.getDatabaseJobStatusByName(AdminJobType.APPLICATION_LISTS_DATABASE_JOB);

        // The business response should still contain the mapped job status for the admin page.
        assertNotNull(status);
        assertEquals(true, status.getEnabled());

        // The audit surrogate should carry the requested job name so the data-audit layer can
        // persist a GET row for database_jobs.job_name.
        assertNotNull(listener.getCompleteEvent());
        val audited = (DatabaseJob) listener.getCompleteEvent().getNewValue();
        assertEquals(AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue(), audited.getName());
    }

    private static final class CapturingAuditListener implements AuditOperationLifecycleListener {
        private CompleteEvent completeEvent;

        @Override
        public void eventPerformed(BaseAuditEvent event) {
            if (event instanceof CompleteEvent complete) {
                completeEvent = complete;
            }
        }

        private CompleteEvent getCompleteEvent() {
            return completeEvent;
        }
    }
}
