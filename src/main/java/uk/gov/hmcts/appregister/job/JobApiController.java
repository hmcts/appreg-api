package uk.gov.hmcts.appregister.job;

import org.springframework.http.ResponseEntity;

import uk.gov.hmcts.appregister.generated.api.JobsApi;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;

import java.util.UUID;

public class JobApiController implements JobsApi {
    @Override
    public ResponseEntity<JobAcknowledgement> getJobStatusById(UUID jobId) {
        return JobsApi.super.getJobStatusById(jobId);
    }
}
