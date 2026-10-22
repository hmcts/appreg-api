package uk.gov.hmcts.appregister.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.common.async.model.JobIdRequest;
import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;
import uk.gov.hmcts.appregister.common.async.model.TrackJobStatusResponse;
import uk.gov.hmcts.appregister.common.async.reader.CsvReader;
import uk.gov.hmcts.appregister.common.async.reader.JpaDataReader;
import uk.gov.hmcts.appregister.common.async.reader.ReadPagePosition;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobPersistenceService;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobService;
import uk.gov.hmcts.appregister.common.async.writer.CsvWriter;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.generated.model.JobStatus;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.csv.ApplicationCodeCsvPojo;
import uk.gov.hmcts.appregister.testutils.csv.ApplicationCodeCsvReaderComparator;
import uk.gov.hmcts.appregister.testutils.csv.JobProcessCsvReadLifecycle;
import uk.gov.hmcts.appregister.testutils.csv.JobProcessCsvWriteLifecycle;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

class AsyncJobServiceImplTest extends BaseIntegration {
    @Autowired private AsyncJobService asyncJobService;

    @Autowired private UserProvider userProvider;

    @Autowired private ApplicationCodeRepository applicationCodeRepository;

    @Autowired private AsyncJobPersistenceService jobPersistence;

    @Autowired private PlatformTransactionManager transactionManager;

    @Autowired private EntityManager entityManager;

    @BeforeEach
    void setUp() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal())
                .thenReturn(TokenGenerator.builder().build().getJwtFromToken());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void given_noTransaction_when_completingJob_then_existingTransactionIsRequired() {
        var jobId =
                JobIdRequest.builder()
                        .id(UUID.randomUUID())
                        .userName(userProvider.getUserId())
                        .build();

        Assertions.assertThrows(
                IllegalTransactionStateException.class, () -> jobPersistence.setCompleted(jobId));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void given_pendingCompletion_when_transactionFinishes_then_dataAndStatusCommitTogether(
            boolean failCommit) throws Exception {
        var observedStatus = new AtomicReference<JobStatus>();
        var observedEntryCount = new AtomicReference<Long>();
        var failureCallbacks = new AtomicInteger();
        var observer = new TransactionTemplate(transactionManager);
        observer.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        var request =
                JobTypeRequest.builder()
                        .jobType(JobType.FEES_REPORT)
                        .userName(userProvider.getUserId())
                        .build();
        var lifecycle =
                new JobProcessCsvReadLifecycle(applicationCodeRepository) {
                    @Override
                    public void completed(AsyncJobLifecycleEvent<ApplicationCodeCsvPojo> event) {
                        Assertions.assertTrue(
                                TransactionSynchronizationManager.isActualTransactionActive());
                        TransactionSynchronizationManager.registerSynchronization(
                                new TransactionSynchronization() {
                                    @Override
                                    public void beforeCommit(boolean readOnly) {
                                        // Flush completion and business writes before observing
                                        // them
                                        // through an independent database transaction.
                                        entityManager.flush();
                                        observer.executeWithoutResult(
                                                status -> {
                                                    observedStatus.set(
                                                            jobPersistence
                                                                    .getJobStatus(
                                                                            event.getResponse()
                                                                                    .getJobId())
                                                                    .orElseThrow()
                                                                    .getStatus());
                                                    observedEntryCount.set(
                                                            getApplicationCodesFunction
                                                                    .apply(Pageable.unpaged())
                                                                    .getTotalElements());
                                                });
                                        if (failCommit) {
                                            throw new IllegalStateException(
                                                    "Forced commit failure");
                                        }
                                    }
                                });
                    }

                    @Override
                    public void failed(AsyncJobLifecycleEvent<ApplicationCodeCsvPojo> event)
                            throws IOException {
                        failureCallbacks.incrementAndGet();
                        super.failed(event);
                    }
                };

        try (var reader =
                new CsvReader<>(
                        getClass().getResourceAsStream("/appcodes.csv"),
                        ApplicationCodeCsvPojo.class)) {
            var response = asyncJobService.startJob(request, reader, lifecycle);
            if (failCommit) {
                var exception =
                        Assertions.assertThrows(
                                ExecutionException.class,
                                () -> response.getFuture().get(30, TimeUnit.SECONDS));
                assertThat(exception).hasRootCauseMessage("Forced commit failure");
            } else {
                response.getFuture().get(30, TimeUnit.SECONDS);
            }

            assertThat(observedStatus.get()).isEqualTo(JobStatus.PROCESSING);
            assertThat(observedEntryCount.get()).isZero();
            assertThat(getApplicationCodesFunction.apply(Pageable.unpaged()).getTotalElements())
                    .isEqualTo(failCommit ? 0 : 10);
            assertThat(jobPersistence.getJobStatus(response.getJobId()).orElseThrow().getStatus())
                    .isEqualTo(failCommit ? JobStatus.FAILED : JobStatus.COMPLETED);
            assertThat(failureCallbacks.get()).isEqualTo(failCommit ? 1 : 0);
        }
    }

    @Test
    void testRunAsyncJobReadingPagedCsv() throws Exception {

        // read the csv and write to the database
        JobTypeRequest request =
                JobTypeRequest.builder()
                        .jobType(JobType.FEES_REPORT)
                        .userName(userProvider.getUserId())
                        .build();
        try (CsvReader<ApplicationCodeCsvPojo> csvReaderForAppCode =
                new CsvReader<>(
                        getClass().getResourceAsStream("/appcodes.csv"),
                        ApplicationCodeCsvPojo.class)) {
            JobProcessCsvReadLifecycle jobProcessCsvReadLifecycle =
                    new JobProcessCsvReadLifecycle(applicationCodeRepository);

            TrackJobStatusResponse response =
                    asyncJobService.startJob(
                            request, csvReaderForAppCode, jobProcessCsvReadLifecycle);

            Assertions.assertNotNull(response.getJobId());

            // wait for end of async job
            response.getFuture().get();

            // now lets assert that each code was added to the database
            ReadPagePosition position = new ReadPagePosition(2, 0);
            JobContext jobContext = new JobContext();

            // ensure we have processed all of the csv records
            Assertions.assertEquals(10, jobProcessCsvReadLifecycle.getCountProcessed());

            // make sure that each record in the csv is in the database
            try (CsvReader<ApplicationCodeCsvPojo> csvReader =
                    new CsvReader<>(
                            getClass().getResourceAsStream("/appcodes.csv"),
                            ApplicationCodeCsvPojo.class)) {
                // verify that all application codes were created in the database
                csvReader.readData(
                        position,
                        (data, ctxt) -> {
                            for (int i = 0; i < data.size(); i++) {
                                var csvBaseAppCode =
                                        applicationCodeRepository
                                                .findByCodeAndDate(
                                                        data.get(i).getCode(),
                                                        LocalDate.now(java.time.ZoneOffset.UTC))
                                                .orElseThrow();
                                Assertions.assertEquals(
                                        data.get(i).getCode(), csvBaseAppCode.getCode());
                                Assertions.assertEquals(
                                        data.get(i).getTitle(), csvBaseAppCode.getTitle());
                                Assertions.assertEquals(
                                        data.get(i).getFeedue() != null && data.get(i).getFeedue(),
                                        csvBaseAppCode.getFeeDue().isYes());
                                Assertions.assertEquals(
                                        data.get(i).getWording(), csvBaseAppCode.getWording());
                            }
                        },
                        jobContext);
            }

            // ensure we succeeded
            Assertions.assertEquals(
                    JobStatus.COMPLETED,
                    asyncJobService.getJobStatus(response.getJobId()).get().getStatus());
        }
    }

    /** Supplies generated application codes. */
    Function<Pageable, Page<ApplicationCode>> getApplicationCodesFunction =
            (pageable) -> {
                return applicationCodeRepository.search(
                        "CODE", null, LocalDate.now(java.time.ZoneOffset.UTC), pageable);
            };

    @Test
    void testRunAsyncJobWritingPagedCsv() throws Exception {

        // read the csv and write to the database
        JobTypeRequest request =
                JobTypeRequest.builder()
                        .jobType(JobType.FEES_REPORT)
                        .userName(userProvider.getUserId())
                        .build();

        // run the csv into the database
        try (CsvReader<ApplicationCodeCsvPojo> csvReaderForAppCode =
                new CsvReader<>(
                        getClass().getResourceAsStream("/appcodes.csv"),
                        ApplicationCodeCsvPojo.class)) {
            JobProcessCsvReadLifecycle jobProcessCsvReadLifecycle =
                    new JobProcessCsvReadLifecycle(applicationCodeRepository);

            TrackJobStatusResponse response =
                    asyncJobService.startJob(
                            request, csvReaderForAppCode, jobProcessCsvReadLifecycle);

            response.getFuture().get();
        }

        // now read the database values and write to csv
        JpaDataReader<ApplicationCode> reader = new JpaDataReader<>(getApplicationCodesFunction);

        JobProcessCsvWriteLifecycle csvWriterLifecycle =
                new JobProcessCsvWriteLifecycle(new CsvWriter<>(ApplicationCodeCsvPojo.class));

        request =
                JobTypeRequest.builder()
                        .jobType(JobType.FEES_REPORT)
                        .userName(userProvider.getUserId())
                        .build();

        // run the job writing the csv as we go eventually writing it to the database
        TrackJobStatusResponse response =
                asyncJobService.startJob(request, reader, csvWriterLifecycle);

        // wait for end of async job
        response.getFuture().get();

        // ensure we succeeded
        Assertions.assertEquals(
                JobStatus.COMPLETED,
                asyncJobService.getJobStatus(response.getJobId()).get().getStatus());

        // lets make sure the generated clob in the database is the same as the original
        try (CsvReader<ApplicationCodeCsvPojo> csvReaderFile =
                        new CsvReader<>(
                                getClass().getResourceAsStream("/appcodes.csv"),
                                ApplicationCodeCsvPojo.class);
                CsvReader<ApplicationCodeCsvPojo> csvReaderDbStream =
                        new CsvReader<>(
                                response.read().getInputStream(), ApplicationCodeCsvPojo.class); ) {
            Assertions.assertEquals(
                    0,
                    new ApplicationCodeCsvReaderComparator()
                            .compare(csvReaderFile, csvReaderDbStream));
        }
    }

    @Test
    void testRunAsyncJobFailValidationPagedCsv() throws Exception {
        // read the csv
        JobTypeRequest request =
                JobTypeRequest.builder()
                        .jobType(JobType.FEES_REPORT)
                        .userName(userProvider.getUserId())
                        .build();
        CsvReader<ApplicationCodeCsvPojo> csvReaderForAppCode =
                new CsvReader<>(
                        getClass().getResourceAsStream("/appcodes.csv"),
                        ApplicationCodeCsvPojo.class);
        JobProcessCsvReadLifecycle jobProcessCsvReadLifecycle =
                new JobProcessCsvReadLifecycle(applicationCodeRepository);

        // force a failure of the csv read database write on page 2
        jobProcessCsvReadLifecycle.setFailValidationOnPage(2);

        TrackJobStatusResponse response =
                asyncJobService.startJob(request, csvReaderForAppCode, jobProcessCsvReadLifecycle);

        try {
            response.getFuture().get();
        } catch (Exception e) {
            // ignore the error that this thread
        }

        // ensure we succeeded
        Assertions.assertEquals(
                JobStatus.FAILED,
                asyncJobService.getJobStatus(response.getJobId()).get().getStatus());

        Assertions.assertTrue(
                asyncJobService
                        .getJobStatus(response.getJobId())
                        .get()
                        .getErrorMessage()
                        .startsWith(JobProcessCsvReadLifecycle.FAILURE_MESSAGE));

        // now lets read and write the csv to the database
        JpaDataReader<ApplicationCode> reader = new JpaDataReader<>(getApplicationCodesFunction);

        List<ApplicationCode> codesAdded = new ArrayList<>();
        JobContext context = new JobContext();
        ReadPagePosition position = new ReadPagePosition(2, 0);
        reader.readData(
                position,
                (data, ctxt) -> {
                    codesAdded.addAll(data);
                },
                context);

        // there should be no codes added as the validation failed on the second page
        assertThat(codesAdded).isEmpty();
    }
}
