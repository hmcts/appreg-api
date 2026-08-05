package uk.gov.hmcts.appregister.common.async.lifecycle;

import java.io.IOException;
import org.slf4j.Logger;
import uk.gov.hmcts.appregister.generated.model.JobStatus;

/**
 * The lifecycle for an asynchronous job.
 */
public interface AsyncJobLifecycle<T> {
    Logger logger = org.slf4j.LoggerFactory.getLogger(AsyncJobLifecycle.class);

    /**
     * The lifecycle event that is being performed. This default function will decompose the
     * individual lifecycle events into their respective phases.
     *
     * @param lifecycleEvent The lifecycle event.
     * @throws IOException Any IO exception that occurs.
     */
    default void lifeCycleEventPerformed(AsyncJobLifecycleEvent<T> lifecycleEvent)
            throws IOException {
        if (lifecycleEvent.getJobStatus() == JobStatus.COMPLETED) {
            logger.debug("Job completed");
            completed(lifecycleEvent);
        } else if (lifecycleEvent.getJobStatus() == JobStatus.FAILED) {
            logger.debug("Job failed");
            failed(lifecycleEvent);
        } else if (lifecycleEvent.getJobStatus() == JobStatus.PROCESSING) {
            logger.debug("Job processing");
            processing(lifecycleEvent);
        } else if (lifecycleEvent.getJobStatus() == JobStatus.RECEIVED) {
            logger.debug("Job received");
            received(lifecycleEvent);
        } else if (lifecycleEvent.getJobStatus() == JobStatus.VALIDATING) {
            logger.debug("Job validating");
            validating(lifecycleEvent);
        }
    }

    /**
     * The validating phase where we validate the data.
     *
     * @param event The lifecycle event.
     * @throws IOException Any IO exception that occurs.
     */
    default void validating(AsyncJobLifecycleEvent<T> event) throws IOException {
        // do nothing
    }

    /**
     * The processing phase where we process the data.
     *
     * @param event The lifecycle event.
     * @throws IOException Any IO exception that occurs. *
     */
    default void processing(AsyncJobLifecycleEvent<T> event) throws IOException {
        // do nothing
    }

    /**
     * The initial received phase which is called before any else.
     *
     * @param event The lifecycle event.
     * @throws IOException Any IO exception that occurs.
     */
    default void received(AsyncJobLifecycleEvent<T> event) throws IOException {
        // do nothing
    }

    /**
     * This is called when the job fails.
     *
     * @param event The lifecycle event.
     * @throws IOException Any IO exception that occurs.
     */
    default void failed(AsyncJobLifecycleEvent<T> event) throws IOException {
        // do nothing
    }

    /**
     * called when the job has completed.
     *
     * @param event The lifecycle event.
     * @throws IOException Any IO exception that occurs.
     */
    default void completed(AsyncJobLifecycleEvent<T> event) throws IOException {
        // do nothing
    }
}
