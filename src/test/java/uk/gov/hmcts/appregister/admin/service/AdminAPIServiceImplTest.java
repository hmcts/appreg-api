package uk.gov.hmcts.appregister.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.admin.mapper.DatabaseJobsMapper;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.DatabaseJob;
import uk.gov.hmcts.appregister.common.entity.RetentionPolicy;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.repository.DatabaseJobRepository;
import uk.gov.hmcts.appregister.common.entity.repository.RetentionPolicyRepository;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.generated.model.AdminJobType;
import uk.gov.hmcts.appregister.generated.model.JobStatus;

@ExtendWith(MockitoExtension.class)
class AdminAPIServiceImplTest {

    @Mock private DatabaseJobRepository databaseJobRepository;
    @Mock private RetentionPolicyRepository retentionPolicyRepository;
    @Mock private DatabaseJobsMapper databaseJobsMapper;
    @Mock private AuditOperationService auditService;

    @Test
    void getDatabaseJobStatusByName_returnsMappedStatus() {
        var databaseJob = new DatabaseJob();
        var jobStatus = new JobStatus().enabled(true);
        when(databaseJobRepository.findByName(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue()))
                .thenReturn(databaseJob);
        when(databaseJobsMapper.toDatabaseJobStatus(databaseJob)).thenReturn(jobStatus);
        when(databaseJobsMapper.toEntity(AdminJobType.APPLICATION_LISTS_DATABASE_JOB))
                .thenReturn(databaseJob);
        when(auditService.processAudit(any(), any()))
                .thenAnswer(invocation -> runReadAudit(invocation.getArgument(1)));

        var result =
                service().getDatabaseJobStatusByName(AdminJobType.APPLICATION_LISTS_DATABASE_JOB);

        assertSame(jobStatus, result);
    }

    @ParameterizedTest
    @MethodSource("enableFlags")
    void enableDisableDatabaseJobByName_persistsExpectedFlag(Boolean enable, YesOrNo expected) {
        var databaseJob = new DatabaseJob();
        databaseJob.setEnabled(YesOrNo.NO);
        when(databaseJobRepository.findByName(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue()))
                .thenReturn(databaseJob);
        when(databaseJobRepository.save(any(DatabaseJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(auditService.processAudit(any(), any(), any()))
                .thenAnswer(invocation -> runUpdateAudit(invocation.getArgument(2)));

        service()
                .enableDisableDatabaseJobByName(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB, enable);

        assertEquals(expected, databaseJob.getEnabled());
        verify(databaseJobRepository).save(databaseJob);
    }

    @Test
    void getDatabaseJobRetentionPeriodByName_returnsFirstPolicyValue() {
        var retentionPolicy = new RetentionPolicy();
        retentionPolicy.setConfigValue("365");
        when(retentionPolicyRepository.countByJobNameAndConfigKey(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue(),
                        "RETENTION_PERIOD_DAYS"))
                .thenReturn(1L);
        when(retentionPolicyRepository.findByJobNameAndConfigKeyOrderByIdAsc(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue(),
                        "RETENTION_PERIOD_DAYS"))
                .thenReturn(List.of(retentionPolicy));
        when(auditService.processAudit(any(), any()))
                .thenAnswer(invocation -> runReadAudit(invocation.getArgument(1)));

        var result =
                service()
                        .getDatabaseJobRetentionPeriodByName(
                                AdminJobType.APPLICATION_LISTS_DATABASE_JOB);

        assertEquals(365, result.getRetentionPeriodDays());
    }

    @Test
    void getDatabaseJobRetentionPeriodByName_throwsWhenMissing() {
        var service = service();
        when(retentionPolicyRepository.countByJobNameAndConfigKey(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue(),
                        "RETENTION_PERIOD_DAYS"))
                .thenReturn(0L);
        when(retentionPolicyRepository.findByJobNameAndConfigKeyOrderByIdAsc(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue(),
                        "RETENTION_PERIOD_DAYS"))
                .thenReturn(List.of());
        when(auditService.processAudit(any(), any()))
                .thenAnswer(invocation -> runReadAudit(invocation.getArgument(1)));

        var ex =
                assertThrows(
                        AppRegistryException.class,
                        () ->
                                service.getDatabaseJobRetentionPeriodByName(
                                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB));

        assertEquals(CommonAppError.INTERNAL_SERVER_ERROR, ex.getCode());
    }

    @Test
    void updateDatabaseJobRetentionPeriodByName_persistsNewValue() {
        var retentionPolicy = new RetentionPolicy();
        retentionPolicy.setConfigValue("365");
        when(retentionPolicyRepository.countByJobNameAndConfigKey(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue(),
                        "RETENTION_PERIOD_DAYS"))
                .thenReturn(1L);
        when(retentionPolicyRepository.findByJobNameAndConfigKeyOrderByIdAsc(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB.getValue(),
                        "RETENTION_PERIOD_DAYS"))
                .thenReturn(List.of(retentionPolicy));
        when(retentionPolicyRepository.save(any(RetentionPolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(auditService.processAudit(any(), any(), any()))
                .thenAnswer(invocation -> runUpdateAudit(invocation.getArgument(2)));

        service()
                .updateDatabaseJobRetentionPeriodByName(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB, 730);

        assertEquals("730", retentionPolicy.getConfigValue());
        verify(retentionPolicyRepository).save(retentionPolicy);
    }

    private AdminAPIServiceImpl service() {
        return new AdminAPIServiceImpl(
                databaseJobRepository, retentionPolicyRepository, databaseJobsMapper, auditService);
    }

    private static Stream<Arguments> enableFlags() {
        return Stream.of(
                Arguments.of(Boolean.TRUE, YesOrNo.YES),
                Arguments.of(Boolean.FALSE, YesOrNo.NO),
                Arguments.of(null, YesOrNo.NO));
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Keyable> T runReadAudit(Object execution) {
        return ((java.util.function.Function<BaseAuditEvent, Optional<AuditableResult<T, E>>>)
                        execution)
                .apply(null)
                .orElseThrow()
                .getResultingValue();
    }

    @SuppressWarnings("unchecked")
    private static <E extends Keyable> Void runUpdateAudit(Object execution) {
        return ((java.util.function.Function<BaseAuditEvent, Optional<AuditableResult<Void, E>>>)
                        execution)
                .apply(null)
                .orElseThrow()
                .getResultingValue();
    }
}
