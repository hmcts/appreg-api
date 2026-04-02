package uk.gov.hmcts.appregister.common.async;

import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncLifecycle;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.async.reader.DataReader;
import uk.gov.hmcts.appregister.common.async.model.JobIdRequest;
import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;
import uk.gov.hmcts.appregister.common.async.reader.PageRead;

import java.util.Optional;

/**
 * This interface is used to run async jobs.
 */
public interface AsyncJobService<T> {

    /**
     * runs the job with a csv stream passed to it
     * @param jobRequest The job type
     * @param reader The reader to read the data.
     * @return The job status report response
     */
    JobStatusResponse startJob(
        JobTypeRequest jobRequest,
        DataReader<T> dataReader,
        PageRead<T> pageImport,
        Class<T> cls,
        AsyncLifecycle<T> asyncLifecycle);

    /**
     * runs the job with a csv stream passed to it
     * @param jobId The job id
     * @return The job status report response
     */
    Optional<JobStatusResponse> getJobStatus(JobIdRequest jobId);
}
