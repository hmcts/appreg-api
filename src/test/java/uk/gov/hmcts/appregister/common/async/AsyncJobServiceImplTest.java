package uk.gov.hmcts.appregister.common.async;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import uk.gov.hmcts.appregister.common.async.exception.JobException;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycle;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.common.async.model.JobIdRequest;
import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;
import uk.gov.hmcts.appregister.common.async.model.TrackJobStatusResponse;
import uk.gov.hmcts.appregister.common.async.reader.CsvReader;
import uk.gov.hmcts.appregister.common.async.reader.DataReader;
import uk.gov.hmcts.appregister.common.async.reader.PageReader;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobPersistenceService;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobServiceImpl;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;

@ExtendWith(MockitoExtension.class)
@Slf4j
class AsyncJobServiceImplTest extends AbstractAsyncTest {
    @Mock private AsyncJobPersistenceService persistence;

    @Spy private TransactionUnitOfWork service = new TransactionUnitOfWork();

    @InjectMocks private AsyncJobServiceImpl asyncJobServiceImpl;

    @Test
    void testAsyncStart() throws Exception {
        // set the page size
        asyncJobServiceImpl.setPageSize(1);

        // setup the user provider to get hold of the user name
        String userId = "userId";
        UUID jobId = UUID.randomUUID();

        // setup the callback
        AsyncJobLifecycle<PersonCsvPojo> lifecycle = mockLifecycle();

        // setup the reader for the csv file
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource("person.csv");
        File fileToLoad = new File(resource.getFile());

        List<PersonCsvPojo> output = new ArrayList<>();
        MDC.put("traceId", "bulk-import-trace");
        CsvReader<PersonCsvPojo> csvReader = new CsvReader<>(fileToLoad, PersonCsvPojo.class);
        try (csvReader) {
            JobIdRequest jobIdRequest = JobIdRequest.builder().id(jobId).userName(userId).build();

            // mock the persistence start job
            when(persistence.startJob(Mockito.notNull())).thenReturn(jobIdRequest);

            JobTypeRequest jobRequest =
                    JobTypeRequest.builder()
                            .jobType(JobType.DURATION_REPORT)
                            .userName(userId)
                            .build();

            // start the job and wait for the async response
            TrackJobStatusResponse trackJobStatusResponse =
                    asyncJobServiceImpl.startJob(
                            jobRequest,
                            csvReader,
                            (data, context) -> {
                                Assertions.assertEquals("bulk-import-trace", MDC.get("traceId"));
                                output.addAll(data);
                            },
                            lifecycle);

            // assert that we synchronously get the response
            Assertions.assertEquals(jobId, trackJobStatusResponse.getJobId().getId());

            // wait for the underlying process
            trackJobStatusResponse.getFuture().get();

            // capture each event in a list so we can asserr
            ArgumentCaptor<AsyncJobLifecycleEvent<PersonCsvPojo>> lifecycleEventArgumentCaptor =
                    lifecycleEventCaptor();
            verify(lifecycle, times(8))
                    .lifeCycleEventPerformed(lifecycleEventArgumentCaptor.capture());

            // verify the events were fired approriately
            verify(persistence)
                    .setJobStatus(
                            JobIdRequest.builder().id(jobId).userName(userId).build(),
                            JobStatus1.RECEIVED);
            verify(persistence, times(3))
                    .setJobStatus(
                            JobIdRequest.builder().id(jobId).userName(userId).build(),
                            JobStatus1.VALIDATING);
            verify(persistence, times(3))
                    .setJobStatus(
                            JobIdRequest.builder().id(jobId).userName(userId).build(),
                            JobStatus1.PROCESSING);
            verify(persistence)
                    .setJobStatus(
                            JobIdRequest.builder().id(jobId).userName(userId).build(),
                            JobStatus1.COMPLETED);

            // assert the number of the events fired
            Assertions.assertEquals(
                    JobStatus1.RECEIVED,
                    lifecycleEventArgumentCaptor.getAllValues().get(0).getJobStatus());
            Assertions.assertEquals(
                    JobStatus1.VALIDATING,
                    lifecycleEventArgumentCaptor.getAllValues().get(1).getJobStatus());
            Assertions.assertEquals(
                    "Alice",
                    lifecycleEventArgumentCaptor.getAllValues().get(1).getData().get(0).getName());

            Assertions.assertEquals(
                    JobStatus1.PROCESSING,
                    lifecycleEventArgumentCaptor.getAllValues().get(2).getJobStatus());

            Assertions.assertEquals(
                    "Alice",
                    lifecycleEventArgumentCaptor.getAllValues().get(2).getData().get(0).getName());

            Assertions.assertEquals(
                    JobStatus1.VALIDATING,
                    lifecycleEventArgumentCaptor.getAllValues().get(3).getJobStatus());
            Assertions.assertEquals(
                    "Bob",
                    lifecycleEventArgumentCaptor.getAllValues().get(3).getData().get(0).getName());

            Assertions.assertEquals(
                    JobStatus1.PROCESSING,
                    lifecycleEventArgumentCaptor.getAllValues().get(4).getJobStatus());

            Assertions.assertEquals(
                    "Bob",
                    lifecycleEventArgumentCaptor.getAllValues().get(4).getData().get(0).getName());
            Assertions.assertEquals(
                    JobStatus1.VALIDATING,
                    lifecycleEventArgumentCaptor.getAllValues().get(5).getJobStatus());

            Assertions.assertEquals(
                    JobStatus1.PROCESSING,
                    lifecycleEventArgumentCaptor.getAllValues().get(6).getJobStatus());

            Assertions.assertEquals(
                    JobStatus1.COMPLETED,
                    lifecycleEventArgumentCaptor.getAllValues().get(7).getJobStatus());
        } finally {
            MDC.clear();
        }

        Assertions.assertThrows(IOException.class, csvReader::getInputStream);
    }

    @Test
    void validationFirstJob_validatesEveryPageBeforeProcessing() throws Exception {
        String userId = "userId";
        UUID jobId = UUID.randomUUID();
        JobIdRequest jobIdRequest = JobIdRequest.builder().id(jobId).userName(userId).build();
        when(persistence.startJob(Mockito.notNull())).thenReturn(jobIdRequest);

        var resource = Thread.currentThread().getContextClassLoader().getResource("person.csv");
        var lifecycle = mockLifecycle();
        var jobRequest =
                JobTypeRequest.builder()
                        .jobType(JobType.BULK_UPLOAD_ENTRIES)
                        .userName(userId)
                        .build();

        try (var csvReader =
                new CsvReader<PersonCsvPojo>(new File(resource.getFile()), PersonCsvPojo.class)) {
            asyncJobServiceImpl
                    .startValidationFirstJob(jobRequest, csvReader, lifecycle, 1)
                    .getFuture()
                    .get();
        }

        var events = lifecycleEventCaptor();
        verify(lifecycle, times(8)).lifeCycleEventPerformed(events.capture());
        assertValidationFirstOrder(events.getAllValues());
        verify(persistence).setJobStatus(jobIdRequest, JobStatus1.RECEIVED);
        verify(persistence).setJobStatus(jobIdRequest, JobStatus1.VALIDATING);
        verify(persistence).setJobStatus(jobIdRequest, JobStatus1.PROCESSING);
        verify(persistence).setJobStatus(jobIdRequest, JobStatus1.COMPLETED);
    }

    @Test
    void validationFirstJob_doesNotProcessWhenValidationFails() throws Exception {
        String userId = "userId";
        UUID jobId = UUID.randomUUID();
        JobIdRequest jobIdRequest = JobIdRequest.builder().id(jobId).userName(userId).build();
        when(persistence.startJob(Mockito.notNull())).thenReturn(jobIdRequest);

        var resource = Thread.currentThread().getContextClassLoader().getResource("person.csv");
        var lifecycle = mockLifecycle();
        Mockito.doAnswer(
                        invocation -> {
                            AsyncJobLifecycleEvent<?> event = invocation.getArgument(0);
                            if (event.getJobStatus() == JobStatus1.VALIDATING) {
                                event.getContext().logFailure("invalid row");
                            }
                            return null;
                        })
                .when(lifecycle)
                .lifeCycleEventPerformed(Mockito.any());
        var jobRequest =
                JobTypeRequest.builder()
                        .jobType(JobType.BULK_UPLOAD_ENTRIES)
                        .userName(userId)
                        .build();

        try (var csvReader =
                new CsvReader<PersonCsvPojo>(new File(resource.getFile()), PersonCsvPojo.class)) {
            var outcome =
                    asyncJobServiceImpl.startValidationFirstJob(
                            jobRequest, csvReader, lifecycle, 1);
            Assertions.assertThrows(ExecutionException.class, () -> outcome.getFuture().get());
        }

        verify(persistence).setJobStatus(jobIdRequest, JobStatus1.VALIDATING);
        verify(persistence, never()).setJobStatus(jobIdRequest, JobStatus1.PROCESSING);
        verify(persistence, never()).setJobStatus(jobIdRequest, JobStatus1.COMPLETED);
        verify(persistence).setJobStatus(jobIdRequest, JobStatus1.FAILED);
    }

    private static void assertValidationFirstOrder(
            List<AsyncJobLifecycleEvent<PersonCsvPojo>> events) {
        Assertions.assertEquals(JobStatus1.RECEIVED, events.get(0).getJobStatus());
        Assertions.assertEquals(JobStatus1.VALIDATING, events.get(1).getJobStatus());
        Assertions.assertEquals(JobStatus1.VALIDATING, events.get(2).getJobStatus());
        Assertions.assertEquals(JobStatus1.VALIDATING, events.get(3).getJobStatus());
        Assertions.assertEquals(JobStatus1.PROCESSING, events.get(4).getJobStatus());
        Assertions.assertEquals(JobStatus1.PROCESSING, events.get(5).getJobStatus());
        Assertions.assertEquals(JobStatus1.PROCESSING, events.get(6).getJobStatus());
        Assertions.assertEquals(JobStatus1.COMPLETED, events.get(7).getJobStatus());
    }

    @Test
    void testAsyncCloseFailureDoesNotFailJob() throws Exception {
        asyncJobServiceImpl.setPageSize(1);

        String userId = "userId";
        UUID jobId = UUID.randomUUID();
        JobIdRequest jobIdRequest = JobIdRequest.builder().id(jobId).userName(userId).build();

        @SuppressWarnings("unchecked")
        DataReader<PersonCsvPojo> dataReader = Mockito.mock(DataReader.class);
        AsyncJobLifecycle<PersonCsvPojo> lifecycle = mockLifecycle();

        when(persistence.startJob(Mockito.notNull())).thenReturn(jobIdRequest);
        Mockito.doThrow(new IOException("close failed")).when(dataReader).close();

        JobTypeRequest jobRequest =
                JobTypeRequest.builder().jobType(JobType.DURATION_REPORT).userName(userId).build();

        TrackJobStatusResponse trackJobStatusResponse =
                asyncJobServiceImpl.startJob(jobRequest, dataReader, lifecycle);

        trackJobStatusResponse.getFuture().get();

        verify(dataReader).close();
        verify(persistence).setJobStatus(jobIdRequest, JobStatus1.RECEIVED);
        verify(persistence).setJobStatus(jobIdRequest, JobStatus1.COMPLETED);
        verify(persistence, never()).setJobStatus(jobIdRequest, JobStatus1.FAILED);
    }

    @Test
    void testAsyncAbruptProcessingFailure() throws Exception {
        BrokenLifecycleWithContext lifecycle = new BrokenLifecycleWithContext();

        String userId = "userId";
        UUID jobId = UUID.randomUUID();

        JobIdRequest jobIdRequest = JobIdRequest.builder().id(jobId).userName(userId).build();

        DataPageReader reader = executeWithLifecycleForFailure(jobIdRequest, lifecycle);

        // prove that we did not continue passing the data after the failure
        Assertions.assertEquals(1, reader.data.size());

        Assertions.assertTrue(lifecycle.failed);

        verify(persistence)
                .setJobStatus(
                        JobIdRequest.builder().id(jobId).userName(userId).build(),
                        JobStatus1.RECEIVED);

        verify(persistence)
                .setJobStatus(
                        JobIdRequest.builder().id(jobId).userName(userId).build(),
                        JobStatus1.VALIDATING);

        verify(persistence).setJobStatus(jobIdRequest, JobStatus1.FAILED);

        verify(persistence)
                .setFailure(
                        jobIdRequest,
                        BrokenLifecycleWithContext.ERROR
                                + ", "
                                + BrokenLifecycleWithContext.ERROR1);
    }

    @Test
    void testAsyncAbruptProcessingStopFailure() throws Exception {
        BrokenProcessingStoppableLifecycle lifecycle = new BrokenProcessingStoppableLifecycle();
        lifecycle.stop = true;

        String userId = "userId";
        UUID jobId = UUID.randomUUID();

        JobIdRequest jobIdRequest = JobIdRequest.builder().id(jobId).userName(userId).build();

        DataPageReader reader = executeWithLifecycleForFailure(jobIdRequest, lifecycle);
        Assertions.assertTrue(lifecycle.failed);

        // prove that we did not continue passing the data after the failure
        Assertions.assertEquals(1, reader.data.size());

        verify(persistence)
                .setJobStatus(
                        JobIdRequest.builder().id(jobId).userName(userId).build(),
                        JobStatus1.RECEIVED);

        verify(persistence)
                .setJobStatus(
                        JobIdRequest.builder().id(jobId).userName(userId).build(),
                        JobStatus1.VALIDATING);

        verify(persistence)
                .setFailure(
                        jobIdRequest,
                        BrokenLifecycleWithContext.ERROR
                                + ", Job failed during PROCESSING for job "
                                + jobIdRequest.getId()
                                + ". Forced termination");
    }

    @Test
    void testAsyncProcessingFailureNotStopping() throws Exception {
        BrokenProcessingStoppableLifecycle lifecycle = new BrokenProcessingStoppableLifecycle();
        lifecycle.stop = false;

        String userId = "userId";
        UUID jobId = UUID.randomUUID();

        JobIdRequest jobIdRequest = JobIdRequest.builder().id(jobId).userName(userId).build();

        DataPageReader reader = executeWithLifecycleForFailure(jobIdRequest, lifecycle);

        // prove that we continued passing the data even though we had an error in the processing
        Assertions.assertEquals(3, reader.data.size());

        Assertions.assertTrue(lifecycle.failed);

        verify(persistence)
                .setJobStatus(
                        JobIdRequest.builder().id(jobId).userName(userId).build(),
                        JobStatus1.RECEIVED);

        verify(persistence, times(3))
                .setJobStatus(
                        JobIdRequest.builder().id(jobId).userName(userId).build(),
                        JobStatus1.VALIDATING);

        verify(persistence).setJobStatus(jobIdRequest, JobStatus1.FAILED);

        verify(persistence).setJobStatus(jobIdRequest, JobStatus1.PROCESSING);

        verify(persistence)
                .setFailure(
                        jobIdRequest,
                        BrokenProcessingStoppableLifecycle.ERROR
                                + ", "
                                + BrokenProcessingStoppableLifecycle.VALIDATE_ERROR
                                + "1"
                                + ", "
                                + BrokenProcessingStoppableLifecycle.VALIDATE_ERROR
                                + "2"
                                + ", "
                                + "Failed to process job: "
                                + jobIdRequest.getId().toString());
    }

    @Test
    void testAsyncValidationFailureStop() throws Exception {

        BrokenValidationStoppableLifecycle lifecycle = new BrokenValidationStoppableLifecycle();

        String userId = "userId";
        UUID jobId = UUID.randomUUID();

        JobIdRequest jobIdRequest = JobIdRequest.builder().id(jobId).userName(userId).build();

        executeWithLifecycleForFailure(jobIdRequest, lifecycle);

        verify(persistence)
                .setJobStatus(
                        JobIdRequest.builder().id(jobId).userName(userId).build(),
                        JobStatus1.RECEIVED);

        // fail was executed and we logged only one error
        Assertions.assertTrue(lifecycle.failed);
        verify(persistence)
                .setFailure(
                        jobIdRequest,
                        BrokenLifecycleWithContext.ERROR
                                + "0"
                                + ", Job failed during VALIDATING for job "
                                + jobIdRequest.getId()
                                + ". Forced termination");
        verify(persistence).setJobStatus(jobIdRequest, JobStatus1.RECEIVED);
    }

    @Test
    void testAsyncValidationFailureDoNotStop() throws Exception {
        BrokenValidationStoppableLifecycle lifecycle = new BrokenValidationStoppableLifecycle();
        lifecycle.stop = false;

        String userId = "userId";
        UUID jobId = UUID.randomUUID();

        JobIdRequest jobIdRequest = JobIdRequest.builder().id(jobId).userName(userId).build();

        DataPageReader reader = executeWithLifecycleForFailure(jobIdRequest, lifecycle);

        // prove that we did continue passing the data after the failure
        Assertions.assertEquals(3, reader.data.size());

        Assertions.assertTrue(lifecycle.failed);
        verify(persistence)
                .setFailure(
                        jobIdRequest,
                        BrokenLifecycleWithContext.ERROR
                                + "0"
                                + ", "
                                + BrokenLifecycleWithContext.ERROR
                                + "1"
                                + ", "
                                + BrokenLifecycleWithContext.ERROR
                                + "2"
                                + ", Failed to process job: "
                                + jobIdRequest.getId().toString());

        verify(persistence)
                .setJobStatus(
                        JobIdRequest.builder().id(jobId).userName(userId).build(),
                        JobStatus1.RECEIVED);

        verify(persistence).setJobStatus(jobIdRequest, JobStatus1.FAILED);

        verify(persistence, times(3)).setJobStatus(jobIdRequest, JobStatus1.VALIDATING);

        verify(persistence, never()).setJobStatus(jobIdRequest, JobStatus1.PROCESSING);
    }

    @Test
    void testAsyncValidationFailureStopWithJobException() throws Exception {
        BrokenValidationJobExceptionLifecycle lifecycle =
                new BrokenValidationJobExceptionLifecycle();

        String userId = "userId";
        UUID jobId = UUID.randomUUID();

        JobIdRequest jobIdRequest = JobIdRequest.builder().id(jobId).userName(userId).build();

        executeWithLifecycleForFailure(jobIdRequest, lifecycle);

        // fail was executed and we logged only one error
        Assertions.assertTrue(lifecycle.failed);

        verify(persistence)
                .setJobStatus(
                        JobIdRequest.builder().id(jobId).userName(userId).build(),
                        JobStatus1.RECEIVED);

        verify(persistence).setJobStatus(jobIdRequest, JobStatus1.RECEIVED);

        verify(persistence).setFailure(jobIdRequest, BrokenLifecycleWithContext.ERROR);
    }

    @Test
    void testPartialFailOfCSVFailsWithANeatError() throws Exception {
        // set the page size
        asyncJobServiceImpl.setPageSize(1);

        // setup the user provider to get hold of the user name
        String userId = "userId";
        UUID jobId = UUID.randomUUID();

        // setup the callback
        AsyncJobLifecycle<PersonCsvPojo> lifecycle = mockLifecycle();

        // setup the reader for the csv file
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource("person_failformat.csv");
        File fileToLoad = new File(resource.getFile());

        List<PersonCsvPojo> output = new ArrayList<>();
        CsvReader<PersonCsvPojo> csvReader = new CsvReader<>(fileToLoad, PersonCsvPojo.class);
        try (csvReader) {
            JobIdRequest jobIdRequest = JobIdRequest.builder().id(jobId).userName(userId).build();

            // mock the persistence start job
            when(persistence.startJob(Mockito.notNull())).thenReturn(jobIdRequest);

            JobTypeRequest jobRequest =
                    JobTypeRequest.builder()
                            .jobType(JobType.DURATION_REPORT)
                            .userName(userId)
                            .build();

            try {
                // start the job and wait for the async response
                asyncJobServiceImpl
                        .startJob(
                                jobRequest,
                                csvReader,
                                (data, context) -> output.addAll(data),
                                lifecycle)
                        .getFuture()
                        .get();
            } catch (Exception e) {
                log.error("Error", e);
            }

            Assertions.assertEquals(2, output.size());
            verify(persistence)
                    .setFailure(
                            jobIdRequest,
                            "Number of data fields does not match number of headers.");
        }
    }

    private DataPageReader executeWithLifecycleForFailure(
            JobIdRequest jobIdRequest,
            AsyncJobLifecycle<PersonCsvPojo> personCsvPojoAsyncJobLifecycle)
            throws Exception {
        asyncJobServiceImpl.setPageSize(1);

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource("person.csv");
        File fileToLoad = new File(resource.getFile());

        String userId = "userId";

        JobTypeRequest jobRequest =
                JobTypeRequest.builder().jobType(JobType.DURATION_REPORT).userName(userId).build();
        DataPageReader reader = new DataPageReader();

        try (CsvReader<PersonCsvPojo> csvReader =
                new CsvReader<>(fileToLoad, PersonCsvPojo.class)) {
            when(persistence.startJob(Mockito.notNull())).thenReturn(jobIdRequest);

            try {
                asyncJobServiceImpl
                        .startJob(jobRequest, csvReader, reader, personCsvPojoAsyncJobLifecycle)
                        .getFuture()
                        .get();
            } catch (Exception e) {
                // we expect an error to be propagated so catch it
                log.error("Failed", e);
            }
        }
        return reader;
    }

    @SuppressWarnings("unchecked")
    private AsyncJobLifecycle<PersonCsvPojo> mockLifecycle() {
        return Mockito.mock(AsyncJobLifecycle.class);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<AsyncJobLifecycleEvent<PersonCsvPojo>> lifecycleEventCaptor() {
        return ArgumentCaptor.forClass(AsyncJobLifecycleEvent.class);
    }

    class DataPageReader implements PageReader<PersonCsvPojo> {
        private List<PersonCsvPojo> data = new ArrayList<>();

        @Override
        public void readData(List<PersonCsvPojo> relatedData, JobContext jobContext)
                throws IOException {
            data.addAll(relatedData);
        }
    }

    @Slf4j
    static class BrokenLifecycleWithContext implements AsyncJobLifecycle<PersonCsvPojo> {
        private static final String ERROR = "random error";
        private static final String ERROR1 = "random error1";
        private boolean failed = false;

        @Override
        public void processing(AsyncJobLifecycleEvent<PersonCsvPojo> event) throws IOException {
            log.debug("Failing processing");

            // set error
            event.getContext().logFailure(ERROR);
            event.getContext().logFailure(ERROR1);

            // throw exception so we do not continue with the processing
            throw new IOException("Broken");
        }

        @Override
        public void failed(AsyncJobLifecycleEvent<PersonCsvPojo> event) throws IOException {
            failed = true;
        }
    }

    @Slf4j
    static class BrokenValidationStoppableLifecycle implements AsyncJobLifecycle<PersonCsvPojo> {
        private static final String ERROR = "random error";
        private boolean failed = false;
        private boolean stop = true;

        int count = 0;

        @Override
        public void validating(AsyncJobLifecycleEvent<PersonCsvPojo> event) throws IOException {
            event.getContext().logFailure(ERROR + count);

            // allow us to stop the validation
            if (stop) {
                event.getContext().setStoppedValidating(true);
            } else {
                event.getContext().setStoppedValidating(false);
            }

            count = count + 1;
        }

        @Override
        public void failed(AsyncJobLifecycleEvent<PersonCsvPojo> event) throws IOException {
            failed = true;
        }
    }

    @Slf4j
    static class BrokenProcessingStoppableLifecycle implements AsyncJobLifecycle<PersonCsvPojo> {
        private static final String ERROR = "random error";
        private static final String VALIDATE_ERROR = "validate error";

        private boolean failed = false;
        private boolean stop = true;
        private boolean validateFailAfterFirst = true;
        private int validated = 0;

        @Override
        public void processing(AsyncJobLifecycleEvent<PersonCsvPojo> event) throws IOException {
            event.getContext().logFailure(ERROR);

            // allow us to stop the validation
            event.getContext().setStoppedValidating(stop);
        }

        @Override
        public void validating(AsyncJobLifecycleEvent<PersonCsvPojo> event) throws IOException {
            if (validated > 0 && validateFailAfterFirst) {
                event.getContext().logFailure(VALIDATE_ERROR + validated);
            }

            validated = validated + 1;
        }

        @Override
        public void failed(AsyncJobLifecycleEvent<PersonCsvPojo> event) throws IOException {
            failed = true;
        }
    }

    @Slf4j
    static class BrokenValidationJobExceptionLifecycle implements AsyncJobLifecycle<PersonCsvPojo> {
        private static final String ERROR = "random error";
        private boolean failed = false;

        @Override
        public void validating(AsyncJobLifecycleEvent<PersonCsvPojo> event) throws IOException {
            throw new JobException(ERROR);
        }

        @Override
        public void failed(AsyncJobLifecycleEvent<PersonCsvPojo> event) throws IOException {
            failed = true;
        }
    }
}
