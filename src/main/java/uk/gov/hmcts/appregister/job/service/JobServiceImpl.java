package uk.gov.hmcts.appregister.job.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.job.audit.JobAuditOperation;
import uk.gov.hmcts.appregister.job.mapper.JobMapper;
import uk.gov.hmcts.appregister.job.validator.JobExistanceValidator;

@Component
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {
    private final JobMapper jobMapper;

    private final JobExistanceValidator statusJobValidator;

    private final AuditOperationService auditService;

    private final List<AuditOperationLifecycleListener> auditLifecycleListeners;

    @Override
    public JobAcknowledgement getJobAckById(UUID jobId) {
        return auditService.processAudit(
                JobAuditOperation.GET_JOB_STATUS_AUDIT_EVENT,
                unused -> {
                    JobStatusResponse jobStatusResponse = getJobStatusById(jobId);

                    return Optional.of(
                            new AuditableResult<>(
                                    jobMapper.toDto(jobStatusResponse), jobMapper.toEntity(jobId)));
                },
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }

    @Override
    public JobStatusResponse getJobStatusById(UUID jobId) {
        return statusJobValidator.validate(
                jobId, (uuid, success) -> success.getJobStatusResponse());
    }
}
