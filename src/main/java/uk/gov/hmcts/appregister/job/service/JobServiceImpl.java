package uk.gov.hmcts.appregister.job.service;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.entity.repository.AsyncJobAppListEntryRepository;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.job.audit.JobAuditOperation;
import uk.gov.hmcts.appregister.job.mapper.JobMapper;
import uk.gov.hmcts.appregister.job.validator.JobExistanceValidator;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {
    private final JobMapper jobMapper;

    private final JobExistanceValidator statusJobValidator;

    private final AuditOperationService auditService;

    private final AsyncJobAppListEntryRepository jobEntryRepository;

    @Override
    public JobAcknowledgement getJobAckById(UUID jobId) {
        return auditService.processAudit(
                JobAuditOperation.GET_JOB_STATUS_AUDIT_EVENT,
                unused -> {
                    JobStatusResponse jobStatusResponse = getJobStatusById(jobId);

                    var acknowledgement = jobMapper.toDto(jobStatusResponse);
                    if (jobStatusResponse.getStatus() == JobStatus.COMPLETED
                            && jobStatusResponse.getType() == JobType.BULK_UPLOAD_ENTRIES) {
                        var totals = jobEntryRepository.getFeeTotals(jobId);
                        acknowledgement.setMainFeeTotal(totals.getMainFeeTotal());
                        acknowledgement.setOffsiteFeeTotal(totals.getOffsiteFeeTotal());
                        acknowledgement.setTotalFeeValue(
                                totals.getMainFeeTotal().add(totals.getOffsiteFeeTotal()));
                    }
                    return Optional.of(
                            new AuditableResult<>(acknowledgement, jobMapper.toEntity(jobId)));
                });
    }

    @Override
    public JobStatusResponse getJobStatusById(UUID jobId) {
        return statusJobValidator.validate(
                jobId, (uuid, success) -> success.getJobStatusResponse());
    }
}
