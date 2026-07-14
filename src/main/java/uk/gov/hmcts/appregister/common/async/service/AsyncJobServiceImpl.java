package uk.gov.hmcts.appregister.common.async.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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
        return startJob(jobTypeRequest, dataReader, null, lifecycle, pageSize, false);
    }

    @Override
    public <T> TrackJobStatusResponse startJob(
            JobTypeRequest jobTypeRequest,
            DataReader<T> dataReader,
            AsyncJobLifecycle<T> lifecycle,
            int pageSize) {
        return startJob(jobTypeRequest, dataReader, null, lifecycle, pageSize, false);
    }

    public <T> TrackJobStatusResponse startJob(
            JobTypeRequest jobRequest,
            DataReader<T> dataReader,
            PageReader<T> pageImport,
            AsyncJobLifecycle<T> lifecycle) {
        return startJob(jobRequest, dataReader, pageImport, lifecycle, pageSize, false);
    }

    private <T> TrackJobStatusResponse startJob(
            JobTypeRequest jobRequest,
            DataReader<T> dataReader,
            PageReader<T> pageImport,
            AsyncJobLifecycle<T> lifecycle,
            int pageSize,
            boolean validationFirst) {

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
                        SecurityContextHolder.getContext(),
                        MDC.getCopyOfContextMap(),
                        validationFirst);

        // the core import logic will be processed in a separate thread
        Future<?> futureJobOutcome = executor.submit(process);

        return new TrackJobStatusResponse(jobStatusResponse, futureJobOutcome);
    }

    @Override
    public <T> TrackJobStatusResponse startValidationFirstJob(
            JobTypeRequest jobTypeRequest,
            DataReader<T> dataReader,
            AsyncJobLifecycle<T> lifecycle,
            int pageSize) {
        return startJob(jobTypeRequest, dataReader, null, lifecycle, pageSize, true);
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

        private final Map<String, String> loggingContext;

        private final boolean validationFirst;

        @Override
        public void run() {
            if (loggingContext == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(loggingContext);
            }

            // set the authentication on the thread local of this thread.
            SecurityContextHolder.setContext(authentication);

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

                                if (validationFirst) {
                                    processValidationFirst();
                                } else {
                                    processPageByPage();
                                }

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
                } catch (IOException e) {
                    log.warn("Failed to clean up async job data reader", e);
                } finally {
                    SecurityContextHolder.clearContext();
                    MDC.clear();
                }
            }
        }

        private void processValidationFirst() throws IOException {
            changeState(jobStatusResponse, JobStatus1.VALIDATING);
            readPhase(JobStatus1.VALIDATING);
            failJobIfValidationFailed();

            changeState(jobStatusResponse, JobStatus1.PROCESSING);
            readPhase(JobStatus1.PROCESSING);
        }

        private void readPhase(JobStatus1 status) throws IOException {
            var pageRead = new AtomicBoolean();
            dataReader.readData(
                    position,
                    (data, ctxt) -> {
                        pageRead.set(true);
                        fireLifecycleEvent(jobStatusResponse, data, status, lifecycle, jobContext);
                    },
                    jobContext);

            if (!pageRead.get() && !jobContext.hasFailure()) {
                fireLifecycleEvent(jobStatusResponse, List.of(), status, lifecycle, jobContext);
            }
        }

        private void processPageByPage() throws IOException {
            dataReader.readData(
                    position,
                    (data, ctxt) -> {
                        fireEventAndChangeState(
                                jobStatusResponse,
                                data,
                                JobStatus1.VALIDATING,
                                lifecycle,
                                jobContext);

                        if (pageRead != null) {
                            pageRead.readData(data, jobContext);
                        }

                        if (!jobContext.hasFailure()) {
                            fireEventAndChangeState(
                                    jobStatusResponse,
                                    data,
                                    JobStatus1.PROCESSING,
                                    lifecycle,
                                    jobContext);
                        }
                    },
                    jobContext);
        }

        private void failJobIfValidationFailed() throws JobException {
            if (jobContext.hasFailure()) {
                throw new JobException(
                        "Failed to process job: " + jobStatusResponse.getJobId().getId());
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

            // Field-count mismatches have already been formatted by the lifecycle. Do not append
            // the generic job failure message.
            if (t instanceof JobException && !jobContext.isFieldCountMismatch()) {
                jobContext.logFailure(t.getMessage());
            }

            // set the fail state
            persistence.setFailure(
                    jobStatusResponse.getJobId(),
                    jobContext.getCommaDelimitedFailureMessage() == null
                            ? "Failed with unknown error"
                            : jobContext.getCommaDelimitedFailureMessage());

            // now force a failure to roll back any database transactional data that
            // was committed
            // in this transaction. This sits irrespective to
            // any state transitions that may have been made for the job process.
            throw new RuntimeException(t);
        }

        /**
         * fire an event and change the state of the underlying job.
         *
         * @param response The job to fire the event for and the forthcoming state transition.
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
            fireLifecycleEvent(response, data, status, lifecycle, context);
            changeState(response, status);
        }

        private void fireLifecycleEvent(
                JobStatusResponse response,
                List<T> data,
                JobStatus1 status,
                AsyncJobLifecycle<T> lifecycle,
                JobContext context)
                throws IOException {
            log.debug("Processing {} for job {}", status, response.getJobId().getId());
            lifecycle.lifeCycleEventPerformed(
                    new AsyncJobLifecycleEvent<>(response, data, context, status));
            handleFailure(context, response, status);
            log.debug("Processed {} for job {}", status, response.getJobId().getId());
        }

        private void changeState(JobStatusResponse response, JobStatus1 status) {
            persistence.setJobStatus(response.getJobId(), status);

            if (status == JobStatus1.RECEIVED
                    || status == JobStatus1.COMPLETED
                    || status == JobStatus1.FAILED) {
                log.info("Job {} is now {}", response.getJobId().getId(), status);
            } else {
                log.debug("Job {} is now {}", response.getJobId().getId(), status);
            }
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
