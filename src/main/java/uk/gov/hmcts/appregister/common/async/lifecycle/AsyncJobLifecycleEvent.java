package uk.gov.hmcts.appregister.common.async.lifecycle;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;

/**
 * Represents a lifecycle event for an asynchronous job.
 */
@RequiredArgsConstructor
@Getter
public class AsyncJobLifecycleEvent<T> {

    /** The current status of the job (including the job details). */
    private final JobStatusResponse response;

    /** The page of data that is being read. */
    private final List<T> data;

    /** The context for the job. We can control the flow of events from here */
    private final JobContext context;

    /** The status that is being transitioned to. */
    private final JobStatus1 jobStatus;
}
