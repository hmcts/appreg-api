package uk.gov.hmcts.appregister.admin.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.admin.audit.AdminAuditOperation;
import uk.gov.hmcts.appregister.admin.mapper.DatabaseJobsMapper;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.repository.DatabaseJobRepository;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.generated.model.AdminJobType;
import uk.gov.hmcts.appregister.generated.model.JobStatus;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAPIServiceImpl implements AdminAPIService {
    private final DatabaseJobRepository databaseJobRepository;
    private final DatabaseJobsMapper databaseJobsMapper;
    private final AuditOperationService auditService;
    private final List<AuditOperationLifecycleListener> auditLifecycleListeners;

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
                                        databaseJobsMapper.toEntity(jobName))),
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }

    @Override
    public void enableDisableDatabaseJobByName(AdminJobType jobName, Boolean enable) {
        var databaseJob = databaseJobRepository.findByName(jobName.getValue());
        databaseJob.setEnabled(enable ? YesOrNo.YES : YesOrNo.NO);
        databaseJobRepository.save(databaseJob);
    }
}
