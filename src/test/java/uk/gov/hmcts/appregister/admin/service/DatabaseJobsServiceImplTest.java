package uk.gov.hmcts.appregister.admin.service;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.hmcts.appregister.admin.exception.DatabaseJobError;
import uk.gov.hmcts.appregister.admin.mapper.DatabaseJobsMapper;
import uk.gov.hmcts.appregister.admin.mapper.DatabaseJobsMapperImpl;
import uk.gov.hmcts.appregister.common.entity.DatabaseJob;
import uk.gov.hmcts.appregister.common.entity.repository.DatabaseJobRepository;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class DatabaseJobsServiceImplTest {
    private AdminAPIServiceImpl service;

    @Mock private DatabaseJobRepository databaseJobRepository;

    @Spy private final DatabaseJobsMapper mapper = new DatabaseJobsMapperImpl();

    @Mock private Clock clock;

    @BeforeEach
    public void setUp() {

        service = new AdminAPIServiceImpl(databaseJobRepository, mapper);
    }

    @Test
    public void testGetDatabaseJobStatusByName() {
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(Clock.systemUTC().getZone());

        DatabaseJob testJob = new DatabaseJob();
        testJob.setName("TEST_JOB");
        testJob.setLastRan(OffsetDateTime.now(clock));
        testJob.setId(1L);
        testJob.setEnabled(YesOrNo.YES);

        when(databaseJobRepository.findByName("TEST_JOB")).thenReturn(testJob);
        service = new AdminAPIServiceImpl(databaseJobRepository, mapper);

        var status = service.getDatabaseJobStatusByName("TEST_JOB");

        assertNotNull(status);
        assertNotNull(status.getLastRan());
        assertEquals(OffsetDateTime.now(clock), status.getLastRan());
        assertEquals(true, status.getEnabled());
    }

    @Test
    public void testEnableDatabaseJobByName() {
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(Clock.systemUTC().getZone());

        DatabaseJob testJob = new DatabaseJob();
        testJob.setName("TEST_ENABLE_JOB");
        testJob.setLastRan(OffsetDateTime.now(clock));
        testJob.setId(2L);
        testJob.setEnabled(YesOrNo.NO);

        when(databaseJobRepository.findByName("TEST_ENABLE_JOB")).thenReturn(testJob);
        service.enableDisableDatabaseJobByName("TEST_ENABLE_JOB", true);

        var status = service.getDatabaseJobStatusByName("TEST_ENABLE_JOB");
        assertNotNull(status);
        assertEquals(true, status.getEnabled());

    }

    @Test
    public void testDisableDatabaseJobByName() {
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(Clock.systemUTC().getZone());

        DatabaseJob testJob = new DatabaseJob();
        testJob.setName("TEST_JOB");
        testJob.setLastRan(OffsetDateTime.now(clock));
        testJob.setId(1L);
        testJob.setEnabled(YesOrNo.YES);

        when(databaseJobRepository.findByName("TEST_JOB")).thenReturn(testJob);
        service = new AdminAPIServiceImpl(databaseJobRepository, mapper);

        service.enableDisableDatabaseJobByName("TEST_JOB", false);

        var status = service.getDatabaseJobStatusByName("TEST_JOB");
        assertNotNull(status);
        assertEquals(false, status.getEnabled());
    }

    @Test
    public void testGetDatabaseJobStatusByNameNotFound() {
        AppRegistryException exception = assertThrows(AppRegistryException.class, () -> {
            service.getDatabaseJobStatusByName("NON_EXISTENT_JOB");
        });

        Assertions.assertEquals(
            DatabaseJobError.JOB_NOT_FOUND.getCode().getAppCode(),
            exception.getCode().getCode().getAppCode());
    }

     @Test
    public void testEnableDisableDatabaseJobByNameNotFound() {
         AppRegistryException exception = assertThrows(AppRegistryException.class, () -> {
             service.enableDisableDatabaseJobByName("NON_EXISTENT_JOB", false);
         });

         Assertions.assertEquals(
             DatabaseJobError.JOB_NOT_FOUND.getCode().getAppCode(),
             exception.getCode().getCode().getAppCode());
    }
}
