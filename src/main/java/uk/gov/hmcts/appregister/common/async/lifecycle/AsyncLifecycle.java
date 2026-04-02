package uk.gov.hmcts.appregister.common.async.lifecycle;

import uk.gov.hmcts.appregister.generated.model.JobStatus;

public interface AsyncLifecycle<T> {

    default void lifeCycleEventPerformed(LifecycleEvent<T> lifecycleEvent) {
        if (lifecycleEvent.getResponse().getStatus() == JobStatus.COMPLETED) {
            completed(lifecycleEvent);
        } else if (lifecycleEvent.getResponse().getStatus()== JobStatus.FAILED) {
            failed(lifecycleEvent);
        } else if (lifecycleEvent.getResponse().getStatus() == JobStatus.PROCESSING) {
            processing(lifecycleEvent);
        } else if (lifecycleEvent.getResponse().getStatus() == JobStatus.RECEIVED) {
            recieved(lifecycleEvent);
        } else if (lifecycleEvent.getResponse().getStatus()== JobStatus.VALIDATING) {
            validating(lifecycleEvent);
        }
    }

    default void validating(LifecycleEvent<T> event) {

    }

    default void started(LifecycleEvent<T> event) {

    }

    default void processing(LifecycleEvent<T> event) {

    }

    default void recieved(LifecycleEvent<T> event) {

    }

    default void failed(LifecycleEvent<T> event) {

    }

    default void completed(LifecycleEvent<T> event) {

    }
}
