package uk.gov.hmcts.appregister.admin.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
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
import uk.gov.hmcts.appregister.common.entity.repository.DatabaseJobRepository;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.generated.model.AdminJobType;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class DatabaseJobsServiceImplTest {
    private AdminAPIServiceImpl service;

    @Mock private DatabaseJobRepository databaseJobRepository;

    @Spy private final DatabaseJobsMapper mapper = new DatabaseJobsMapperImpl();

    @Mock private Clock clock;

    @BeforeEach
    public void setUp() {

        service =
                new AdminAPIServiceImpl(
                        databaseJobRepository,
                        mapper,
                        new AuditOperationServiceImpl(new ObjectMapper(), List.of()),
                        List.of());
    }

    @Test
    public void testGetDatabaseJobStatusByName() {
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(Clock.systemUTC().getZone());

        val testJob = new DatabaseJob();
        testJob.setName(AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue());
        testJob.setLastRan(OffsetDateTime.now(clock));
        testJob.setId(1L);
        testJob.setEnabled(YesOrNo.YES);

        when(databaseJobRepository.findByName(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue()))
                .thenReturn(testJob);
        service =
                new AdminAPIServiceImpl(
                        databaseJobRepository,
                        mapper,
                        new AuditOperationServiceImpl(new ObjectMapper(), List.of()),
                        List.of());

        val status =
                service.getDatabaseJobStatusByName(AdminJobType.APPLICATION_LISTS_DATABASE_JOB);

        assertNotNull(status);
        assertNotNull(status.getLastRan());
        assertEquals(OffsetDateTime.now(clock), status.getLastRan());
        assertEquals(true, status.getEnabled());
    }

    @Test
    public void testEnableDatabaseJobByName() {
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(Clock.systemUTC().getZone());

        val testJob = new DatabaseJob();
        testJob.setName(AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue());
        testJob.setLastRan(OffsetDateTime.now(clock));
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
    public void testDisableDatabaseJobByName() {
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(Clock.systemUTC().getZone());

        val testJob = new DatabaseJob();
        testJob.setName(AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue());
        testJob.setLastRan(OffsetDateTime.now(clock));
        testJob.setId(1L);
        testJob.setEnabled(YesOrNo.YES);

        when(databaseJobRepository.findByName(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue()))
                .thenReturn(testJob);
        service =
                new AdminAPIServiceImpl(
                        databaseJobRepository,
                        mapper,
                        new AuditOperationServiceImpl(new ObjectMapper(), List.of()),
                        List.of());

        service.enableDisableDatabaseJobByName(AdminJobType.APPLICATION_LISTS_DATABASE_JOB, false);

        val status =
                service.getDatabaseJobStatusByName(AdminJobType.APPLICATION_LISTS_DATABASE_JOB);
        assertNotNull(status);
        assertEquals(false, status.getEnabled());
    }

    @Test
    public void testGetDatabaseJobStatusByName_auditsRequestedJobType() {
        val testJob = new DatabaseJob();
        testJob.setName(AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue());
        testJob.setEnabled(YesOrNo.YES);

        when(databaseJobRepository.findByName(any())).thenReturn(testJob);

        val listener = new CapturingAuditListener();
        service =
                new AdminAPIServiceImpl(
                        databaseJobRepository,
                        mapper,
                        new AuditOperationServiceImpl(new ObjectMapper(), List.of(listener)),
                        List.of(listener));

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
