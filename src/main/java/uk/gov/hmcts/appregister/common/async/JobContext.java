package uk.gov.hmcts.appregister.common.async;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * A context that allows us to control the flow of an asynchronous flow. We can use this context to
 * set validation failures and stop the flow. The context is passed to the lifecycle events {@link
 * uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycle}
 */
@Getter
@Setter
public class JobContext {
    private List<String> validationFailureMessages = new ArrayList<>();

    /**
     * Allows the developer to stop the job processing, now or carry on validating each page of data
     * until the end. This is false by default, so the underlying job will continue to validate each
     * page of data until the end.
     */
    private boolean isStoppedValidating = false;

    /**
     * determines if any failures have been made.
     *
     * @return true if there are failures
     */
    public boolean hasFailure() {
        return !validationFailureMessages.isEmpty();
    }

    /**
     * gets the failure message that is comma delimited.
     *
     * @return The comma seperated failure messages
     */
    public String getCommaDelimitedFailureMessage() {
        return String.join(", ", validationFailureMessages);
    }

    /**
     * log a failure to the context.
     *
     * @param errorMsg The error message to log.
     */
    public void logFailure(String errorMsg) {
        if (validationFailureMessages.stream().filter(msg -> msg.equals(errorMsg)).count() == 0) {
            validationFailureMessages.add(errorMsg);
        }
    }
}
