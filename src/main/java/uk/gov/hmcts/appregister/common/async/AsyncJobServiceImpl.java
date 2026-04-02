package uk.gov.hmcts.appregister.common.async;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncLifecycle;
import uk.gov.hmcts.appregister.common.async.lifecycle.LifecycleEvent;
import uk.gov.hmcts.appregister.common.async.model.JobIdRequest;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;
import uk.gov.hmcts.appregister.common.async.reader.DataReader;
import uk.gov.hmcts.appregister.common.async.reader.PageRead;
import uk.gov.hmcts.appregister.generated.model.JobStatus;
import uk.gov.hmcts.appregister.generated.model.JobType;

/**
 * A default implementation of the {@link AsyncJobService} interface.
 */
@Component
@RequiredArgsConstructor
public class AsyncJobServiceImpl<T> implements AsyncJobService<T> {
    /** decouples the core lifecyle of an async job from its state management. */
    private JobStatusPersistence persistence;

    /**
     * A shared executor. We use virtual threads asa most of the processing will be IO which should
     * mean our service can scale.
     */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();


    @Override
    @Transactional
    public JobStatusResponse startJob(
            JobTypeRequest jobRequest,
            DataReader<T> dataReader,
            PageRead<T> pageImport,
            Class<T> cls,
            AsyncLifecycle<T> asyncLifecycle) {
        // start job in the database
        JobIdRequest id = persistence.startJob(jobRequest);

        fireEventAndChangeState(
                asyncLifecycle, id, jobRequest.getJobType(), JobStatus.RECEIVED, null);

        // validate
        fireEventAndChangeState(
                asyncLifecycle, id, jobRequest.getJobType(), JobStatus.VALIDATING, null);

        // the core import logic will be processed in a seperate thread
        try {
                executor.submit(
                        () -> {
                            dataReader.readData(
                                    jobRequest,
                                    cls,
                                    (pos, data) -> {

                                        // fire the lifecycle event
                                        fireEventAndChangeState(
                                                asyncLifecycle,
                                                id,
                                                jobRequest.getJobType(),
                                                JobStatus.PROCESSING,
                                                data);

                                        // process the data
                                        pageImport.readData(pos, data);
                                    });

                            fireEventAndChangeState(
                                    asyncLifecycle,
                                    id,
                                    jobRequest.getJobType(),
                                    JobStatus.COMPLETED,
                                    null);
                            return true;
                        });
        } catch (Exception e) {
            fireEventAndChangeState(
                    asyncLifecycle, id, jobRequest.getJobType(), JobStatus.COMPLETED, null);
        }

        return getJobStatus(id).get();
    }

    @Transactional
    @Override
    public Optional<JobStatusResponse> getJobStatus(JobIdRequest jobId) {
        return persistence.getJobStatus(jobId);
    }


    private void fireEventAndChangeState(
        AsyncLifecycle<T> asyncLifecycle,
        JobIdRequest id,
        JobType jobType,
        JobStatus status,
        List<T> data) {
        asyncLifecycle.lifeCycleEventPerformed(new LifecycleEvent<>(id, jobType, status, data));
        persistence.setJobStatus(id, status);
    }
}
