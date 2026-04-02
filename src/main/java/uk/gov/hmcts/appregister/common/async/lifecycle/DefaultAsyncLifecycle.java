package uk.gov.hmcts.appregister.common.async.lifecycle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.appregister.common.async.exception.JobError;
import uk.gov.hmcts.appregister.common.async.writer.PageWrite;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

import java.io.IOException;

/**
 * Defines some basic lifecycle events that can be used by any job. If specifics are required then
 * new classes can override these methods to suite their needs.
 */
@RequiredArgsConstructor
@Slf4j
public class DefaultAsyncLifecycle<T> implements AsyncLifecycle<T> {
    private final PageWrite<T> pageWrite;

    /**
     * If we have a writer implementation then use this to update the
     * job blob with any written data. If there is no writer implementation
     * then we do not need to update the blob and can just return the response.
     */
    @Override
    public void completed(LifecycleEvent<T> event) {
        if (pageWrite != null) {
            try (pageWrite) {
                event.getResponse().write(pageWrite.getInputStream());
            } catch (IOException io) {
                log.error("Failed to write blob", io);
                throw new AppRegistryException(JobError.JOB_DOES_NOT_EXIST_OR_NOT_FOR_USER,
                                               "Failed to write blob to job {}".formatted(event.getResponse()
                                                   .getJobId().getId()));
            }
        }
    }
}
