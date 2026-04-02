package uk.gov.hmcts.appregister.common.async;

import uk.gov.hmcts.appregister.common.async.model.JobIdRequest;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;
import uk.gov.hmcts.appregister.generated.model.JobStatus;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

/**
 * This is a job status persistance interface that allows us to persist the job status and
 * get job status.
 */
public interface JobStatusPersistence {

    /**
     * runs the job with the state event passed to it
     * @param jobType The job type
     * @param jobStatus The job status
     */
    void setJobStatus(JobIdRequest jobType, JobStatus jobStatus);

    /**
     * gets the response
     * @param id The id of the job
     */
    Optional<JobStatusResponse> getJobStatus(JobIdRequest id);


    JobIdRequest startJob(JobTypeRequest request);

    void writeBlob(JobIdRequest jobIdRequest, InputStream inputStream);

    void readBlob(JobIdRequest jobIdRequest, OutputStream outputStream);
}
