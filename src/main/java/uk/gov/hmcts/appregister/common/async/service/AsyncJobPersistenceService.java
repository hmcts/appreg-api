package uk.gov.hmcts.appregister.common.async.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import uk.gov.hmcts.appregister.common.async.model.JobIdRequest;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;

/**
 * This is a job status persistence interface that allows us to control the job persistence.
 */
public interface AsyncJobPersistenceService {

    /**
     * sets the job with the state event passed to it.
     *
     * @param jobType The job type
     * @param jobStatus The job status
     */
    void setJobStatus(JobIdRequest jobType, JobStatus1 jobStatus);

    /**
     * Sets the status of the job as failed with the reason for failure.
     *
     * @param jobType The job type
     * @param reasonFailed The reason for failure
     */
    void setFailure(JobIdRequest jobType, String reasonFailed);

    /**
     * gets the response.
     *
     * @param id The id of the job
     * @return The job status response if one exists else an empty
     */
    Optional<JobStatusResponse> getJobStatus(JobIdRequest id);

    /**
     * does the job type exist and is not it finished.
     *
     * @param id The job type
     * @return true if the job type exists and is not yet finished, false otherwise
     */
    boolean isJobTypeFinishedForUser(JobTypeRequest id);

    /**
     * starts a job.
     *
     * @param request The job details to start.
     * @return The job id request to probe the details of the job.
     */
    JobIdRequest startJob(JobTypeRequest request);

    /**
     * writes a clob to the job id request.
     *
     * @param jobIdRequest The job id request.
     * @param inputStream The input stream to write.
     * @throws IOException Any problems
     */
    void writeClob(JobIdRequest jobIdRequest, InputStream inputStream) throws IOException;

    /**
     * reads a clob from the job id request.
     *
     * @param jobIdRequest The job id request.
     * @throws IOException Any problems
     */
    InputStream readClob(JobIdRequest jobIdRequest) throws IOException;
}
