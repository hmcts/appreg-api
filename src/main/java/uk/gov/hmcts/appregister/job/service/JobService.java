package uk.gov.hmcts.appregister.job.service;

import java.util.UUID;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;

public interface JobService {
    JobAcknowledgement getJobAckById(UUID jobId);

    JobStatusResponse getJobStatusById(UUID jobId);
}
