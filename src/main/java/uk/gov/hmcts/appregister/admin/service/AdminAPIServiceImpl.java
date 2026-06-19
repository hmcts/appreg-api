package uk.gov.hmcts.appregister.admin.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.admin.audit.AdminAuditOperation;
import uk.gov.hmcts.appregister.admin.mapper.DatabaseJobsMapper;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.RetentionPolicy;
import uk.gov.hmcts.appregister.common.entity.repository.DatabaseJobRepository;
import uk.gov.hmcts.appregister.common.entity.repository.RetentionPolicyRepository;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.generated.model.AdminJobType;
import uk.gov.hmcts.appregister.generated.model.JobRetentionPolicy;
import uk.gov.hmcts.appregister.generated.model.JobStatus;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAPIServiceImpl implements AdminAPIService {
    private static final String RETENTION_PERIOD_DAYS = "RETENTION_PERIOD_DAYS";

    private final DatabaseJobRepository databaseJobRepository;
    private final RetentionPolicyRepository retentionPolicyRepository;
    private final DatabaseJobsMapper databaseJobsMapper;
    private final AuditOperationService auditService;

    @Override
    public JobStatus getDatabaseJobStatusByName(AdminJobType jobName) {
        return auditService.processAudit(
                AdminAuditOperation.GET_DATABASE_JOB_STATUS_AUDIT_EVENT,
                unused ->
                        Optional.of(
                                new AuditableResult<>(
                                        databaseJobsMapper.toDatabaseJobStatus(
                                                databaseJobRepository.findByName(
                                                        jobName.getValue())),
                                        databaseJobsMapper.toEntity(jobName))));
    }

    @Override
    public void enableDisableDatabaseJobByName(AdminJobType jobName, Boolean enable) {
        var databaseJob = databaseJobRepository.findByName(jobName.getValue());
        databaseJob.setEnabled(Boolean.TRUE.equals(enable) ? YesOrNo.YES : YesOrNo.NO);
        databaseJobRepository.save(databaseJob);
    }

    @Override
    public JobRetentionPolicy getDatabaseJobRetentionPeriodByName(AdminJobType jobName) {
        var retentionPolicy = getRetentionPolicyByJobName(jobName);

        return new JobRetentionPolicy()
                .retentionPeriodDays(Integer.valueOf(retentionPolicy.getConfigValue()));
    }

    @Override
    @Transactional
    public void updateDatabaseJobRetentionPeriodByName(
            AdminJobType jobName, Integer retentionPeriodDays) {
        var retentionPolicy = getRetentionPolicyByJobName(jobName);
        retentionPolicy.setConfigValue(retentionPeriodDays.toString());
        retentionPolicyRepository.save(retentionPolicy);
    }

    private RetentionPolicy getRetentionPolicyByJobName(AdminJobType jobName) {
        var duplicatePolicies =
                retentionPolicyRepository.countByJobNameAndConfigKey(
                        jobName.getValue(), RETENTION_PERIOD_DAYS);
        if (duplicatePolicies > 1) {
            log.warn(
                    "Multiple retention policy rows found for job {} and key {}; using first by rp_id",
                    jobName.getValue(),
                    RETENTION_PERIOD_DAYS);
        }

        var retentionPolicies =
                retentionPolicyRepository.findByJobNameAndConfigKeyOrderByIdAsc(
                        jobName.getValue(), RETENTION_PERIOD_DAYS);
        return retentionPolicies.stream()
                .findFirst()
                .orElseThrow(
                        () ->
                                new AppRegistryException(
                                        CommonAppError.INTERNAL_SERVER_ERROR,
                                        "Retention configuration %s was not found for administrative job %s"
                                                .formatted(
                                                        RETENTION_PERIOD_DAYS,
                                                        jobName.getValue())));
    }
}
