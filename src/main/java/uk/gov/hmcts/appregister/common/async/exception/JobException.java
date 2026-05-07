package uk.gov.hmcts.appregister.common.async.exception;

import java.io.IOException;

/**
 * A job specific exception that when thrown can influence the underlying erroneous job state when
 * processing asynchronously. The message will be reflected as an error in the job status. {@link
 * uk.gov.hmcts.appregister.common.async.JobContext#logFailure(String)}
 */
public class JobException extends IOException {
    public JobException(String message, Throwable cause) {
        super(message, cause);
    }

    public JobException(String message) {
        super(message);
    }
}
