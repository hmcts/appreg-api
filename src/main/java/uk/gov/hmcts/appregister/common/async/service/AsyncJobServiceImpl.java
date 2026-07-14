package uk.gov.hmcts.appregister.common.async.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.TransactionUnitOfWork;
import uk.gov.hmcts.appregister.common.async.exception.JobException;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycle;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.common.async.model.JobIdRequest;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;
import uk.gov.hmcts.appregister.common.async.model.TrackJobStatusResponse;
import uk.gov.hmcts.appregister.common.async.reader.DataReader;
import uk.gov.hmcts.appregister.common.async.reader.PageReader;
import uk.gov.hmcts.appregister.common.async.reader.ReadPagePosition;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;

/**
 * A default implementation of the {@link AsyncJobService} interface.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Setter
public class AsyncJobServiceImpl implements AsyncJobService {
    /** decouples the core lifecycles of an async job from its state management. */
    private final AsyncJobPersistenceService persistence;

    private final TransactionUnitOfWork transactionalUnitOfWork;

    /**
     * If executed within a Spring context then ensure that the job read page size is set in yaml.
     */
    @Value("${appreg.job.page-size}")
    private int pageSize;

    /**
     * A shared executor. We use virtual threads here as most of the processing will be IO which
     * should mean our service can scale better than a more traditional thread pool. If we later
     * want the service to stop owning thread policy directly, move this async boundary to a
     * Spring-managed TaskExecutor/@Async flow.
     */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public <T> TrackJobStatusResponse startJob(
            JobTypeRequest jobTypeRequest,
            DataReader<T> dataReader,
            AsyncJobLifecycle<T> lifecycle) {
        return startJob(jobTypeRequest, dataReader, null, lifecycle, pageSize);
    }

    @Override
    public <T> TrackJobStatusResponse startJob(
            JobTypeRequest jobTypeRequest,
            DataReader<T> dataReader,
            AsyncJobLifecycle<T> lifecycle,
            int pageSize) {
        return startJob(jobTypeRequest, dataReader, null, lifecycle, pageSize);
    }

    public <T> TrackJobStatusResponse startJob(
            JobTypeRequest jobRequest,
            DataReader<T> dataReader,
            PageReader<T> pageImport,
            AsyncJobLifecycle<T> lifecycle) {
        return startJob(jobRequest, dataReader, pageImport, lifecycle, pageSize);
    }

    private <T> TrackJobStatusResponse startJob(
            JobTypeRequest jobRequest,
            DataReader<T> dataReader,
            PageReader<T> pageImport,
            AsyncJobLifecycle<T> lifecycle,
            int pageSize) {

        JobContext jobContext = new JobContext();

        // start job synchronously to this thread
        JobIdRequest id = persistence.startJob(jobRequest);

        // get the response data
        JobStatusResponse jobStatusResponse =
                JobStatusResponse.builder()
                        .uuid(id.getId())
                        .persistence(persistence)
                        .type(jobRequest.getJobType())
                        .userName(jobRequest.getUserName())
                        .status(JobStatus1.RECEIVED)
                        .build();

        ReadPagePosition position = new ReadPagePosition(pageSize, 0);

        AsyncLifecycleProcessor<T> process =
                new AsyncLifecycleProcessor<>(
                        position,
                        dataReader,
                        jobStatusResponse,
                        lifecycle,
                        jobContext,
                        pageImport,
                        SecurityContextHolder.getContext());

        // the core import logic will be processed in a seperate thread
        Future<?> futureJobOutcome = executor.submit(process);

        return new TrackJobStatusResponse(jobStatusResponse, futureJobOutcome);
    }

    @Override
    public Optional<JobStatusResponse> getJobStatus(JobIdRequest jobId) {
        return persistence.getJobStatus(jobId);
    }

    /**
     * A runnable implementation that will process the async job within its own database
     * transaction.
     */
    @RequiredArgsConstructor
    class AsyncLifecycleProcessor<T> implements Runnable {

        private final ReadPagePosition position;

        private final DataReader<T> dataReader;

        private final JobStatusResponse jobStatusResponse;

        private final AsyncJobLifecycle<T> lifecycle;

        private final JobContext jobContext;

        private final PageReader<T> pageRead;

        private final SecurityContext authentication;

        @Override
        public void run() {
            // set the authentication on the thread local of this thread.
            SecurityContextHolder.setContext(authentication);
            long start = System.nanoTime();

            try {
                // run the runnable inside of a database transaction
                transactionalUnitOfWork.inTransaction(
                        () -> {
                            try {

                                fireEventAndChangeState(
                                        jobStatusResponse,
                                        null,
                                        JobStatus1.RECEIVED,
                                        lifecycle,
                                        jobContext);

                                dataReader.readData(
                                        position,
                                        (data, ctxt) -> {

                                            // validate the read page of data
                                            fireEventAndChangeState(
                                                    jobStatusResponse,
                                                    data,
                                                    JobStatus1.VALIDATING,
                                                    lifecycle,
                                                    jobContext);

                                            // if we dont have a page read callback then do not
                                            // call.
                                            if (pageRead != null) {
                                                // process the data
                                                pageRead.readData(data, jobContext);
                                            }

                                            // if a failure has been detected then stop processing
                                            // and just ensure that
                                            // validation is captured for all. We do not support
                                            // partial fails at this point in time
                                            if (!jobContext.hasFailure()) {
                                                // process the state with the read data
                                                fireEventAndChangeState(
                                                        jobStatusResponse,
                                                        data,
                                                        JobStatus1.PROCESSING,
                                                        lifecycle,
                                                        jobContext);
                                            }
                                        },
                                        jobContext);

                                // if a failure was detected then fail else complete the job
                                if (jobContext.hasFailure()) {
                                    throw new JobException(
                                            "Failed to process job: "
                                                    + jobStatusResponse.getJobId().getId());
                                } else {
                                    fireEventAndChangeState(
                                            jobStatusResponse,
                                            null,
                                            JobStatus1.COMPLETED,
                                            lifecycle,
                                            jobContext);
                                }
                            } catch (Exception t) {
                                processError(t);
                            }
                        });
            } finally {
                try {
                    dataReader.close();
                    long end = System.nanoTime();
                    log.info(
                            "Job {} completed in {} seconds",
                            jobStatusResponse.getJobId().getId(), (end - start) / 1_000_000_000.0);
                } catch (IOException e) {
                    log.warn("Failed to clean up async job data reader", e);
                }
            }
        }

        private void processError(Throwable t) {
            log.error("Error processing job", t);

            try {
                fireEventAndChangeState(
                        jobStatusResponse, null, JobStatus1.FAILED, lifecycle, jobContext);
            } catch (IOException e) {
                log.error("Error calling failure lifecycle", e);
            }

            // if this is a job exception then log the error
            if (t instanceof JobException) {
                jobContext.logFailure(t.getMessage());
            }

            // set the fail state
            persistence.setFailure(
                    jobStatusResponse.getJobId(),
                    jobContext.getCommaDelimitedFailureMessage() == null
                            ? "Failed with unknown error"
                            : jobContext.getCommaDelimitedFailureMessage());

            // now force a failure to roll back any database transactional data that
            // was commited
            // in this transaction. This sits irrespective to
            // any state transitions that may have been made for the job process.
            throw new RuntimeException(t);
        }

        /**
         * fire an event and change the state of the underlying job.
         *
         * @param response The jobn to fire the event for and the forthcoming state transition.
         * @param data The read data to pass to the lifecycle event.
         * @param status The status to set the job to.
         * @param lifecycle The lifecycle to fire the event for.
         * @param context The job context to pass to the lifecycle event.
         */
        private void fireEventAndChangeState(
                JobStatusResponse response,
                List<T> data,
                JobStatus1 status,
                AsyncJobLifecycle<T> lifecycle,
                JobContext context)
                throws IOException {
            log.debug("Processing {} for job {}", status, response.getJobId());

            // fire the lifecycle event
            lifecycle.lifeCycleEventPerformed(
                    new AsyncJobLifecycleEvent<>(response, data, context, status));

            handleFailure(jobContext, jobStatusResponse, status);

            // ensure we set the status
            persistence.setJobStatus(response.getJobId(), status);

            log.info("Job {} is now {}", response.getJobId(), status);

            log.debug("Processed {} for job {}", status, response.getJobId());
        }

        /**
         * Decide how to handle a failure.
         *
         * @param jobContext The job context containing the failure message.
         * @param jobStatusResponse The job status response containing the job id.
         */
        private void handleFailure(
                JobContext jobContext, JobStatusResponse jobStatusResponse, JobStatus1 status)
                throws JobException {
            // if we have a failure but we want to validate all other
            // results then keep going.
            if (jobContext.hasFailure() && jobContext.isStoppedValidating()) {
                throw new JobException(
                        "Job failed during %s for job %s. Forced termination"
                                .formatted(status, jobStatusResponse.getJobId().getId()));
            }
        }
    }
}
