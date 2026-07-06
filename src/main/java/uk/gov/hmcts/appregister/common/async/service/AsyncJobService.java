package uk.gov.hmcts.appregister.common.async.service;

import java.util.Optional;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycle;
import uk.gov.hmcts.appregister.common.async.model.JobIdRequest;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;
import uk.gov.hmcts.appregister.common.async.model.TrackJobStatusResponse;
import uk.gov.hmcts.appregister.common.async.reader.DataReader;

/**
 * This interface is used to run async jobs.
 */
public interface AsyncJobService {

    /**
     * starts the job with a csv stream passed to it.
     *
     * @param jobType The job type
     * @param dataReader The reader to read the data.
     * @param lifecycle The lifecycle to run the job. The lifecycle will page data for the
     *     JobStatus.VALIDATING and JobStatus.PROCESSING phases, so these lifecycle phases will be
     *     hit multiple times.
     * @return The job status report response
     */
    <T> TrackJobStatusResponse startJob(
            JobTypeRequest jobType, DataReader<T> dataReader, AsyncJobLifecycle<T> lifecycle);

    /**
     * starts the job with a csv stream passed to it.
     *
     * @param jobType The job type
     * @param dataReader The reader to read the data.
     * @param lifecycle The lifecycle to run the job.
     * @param pageSize The number of records to read in each page.
     * @return The job status report response
     */
    <T> TrackJobStatusResponse startJob(
            JobTypeRequest jobType,
            DataReader<T> dataReader,
            AsyncJobLifecycle<T> lifecycle,
            int pageSize);

    /**
     * runs the job with a csv stream passed to it.
     *
     * @param jobId The job id
     * @return The job status report response. This may return an empty optional if the job has not
     *     yet
     */
    Optional<JobStatusResponse> getJobStatus(JobIdRequest jobId);
}
