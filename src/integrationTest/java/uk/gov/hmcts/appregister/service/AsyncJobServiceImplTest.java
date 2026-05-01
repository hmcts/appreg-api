package uk.gov.hmcts.appregister.service;

import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;
import uk.gov.hmcts.appregister.common.async.model.TrackJobStatusResponse;
import uk.gov.hmcts.appregister.common.async.reader.CsvReader;
import uk.gov.hmcts.appregister.common.async.reader.JpaDataReader;
import uk.gov.hmcts.appregister.common.async.reader.ReadPagePosition;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobService;
import uk.gov.hmcts.appregister.common.async.writer.CsvWriter;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.csv.ApplicationCodeCsvPojo;
import uk.gov.hmcts.appregister.testutils.csv.ApplicationCodeCsvReaderComparator;
import uk.gov.hmcts.appregister.testutils.csv.JobProcessCsvReadLifecycle;
import uk.gov.hmcts.appregister.testutils.csv.JobProcessCsvWriteLifecycle;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

public class AsyncJobServiceImplTest extends BaseIntegration {
    @Autowired private AsyncJobService asyncJobService;

    @Autowired private UserProvider userProvider;

    @Autowired private ApplicationCodeRepository applicationCodeRepository;

    @BeforeEach
    public void setUp() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal())
                .thenReturn(TokenGenerator.builder().build().getJwtFromToken());
        SecurityContextHolder.getContext().setAuthentication(authentication);
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
                                List<ApplicationCode> csvBaseAppCodeLst =
                                        applicationCodeRepository.findByCodeAndDate(
                                                data.get(i).getCode(), LocalDate.now());
                                Assertions.assertEquals(1, csvBaseAppCodeLst.size());
                                Assertions.assertEquals(
                                        data.get(i).getCode(), csvBaseAppCodeLst.get(0).getCode());
                                Assertions.assertEquals(
                                        data.get(i).getTitle(),
                                        csvBaseAppCodeLst.get(0).getTitle());
                                Assertions.assertEquals(
                                        data.get(i).getFeedue() != null && data.get(i).getFeedue(),
                                        csvBaseAppCodeLst.get(0).getFeeDue().isYes());
                                Assertions.assertEquals(
                                        data.get(i).getWording(),
                                        csvBaseAppCodeLst.get(0).getWording());
                            }
                        },
                        jobContext);
            }

            // ensure we succeeded
            Assertions.assertEquals(
                    JobStatus1.COMPLETED,
                    asyncJobService.getJobStatus(response.getJobId()).get().getStatus());
        }
    }

    /** Supplies generated application codes. */
    Function<Pageable, Page<ApplicationCode>> getApplicationCodesFunction =
            (pageable) -> {
                return applicationCodeRepository.search("CODE", null, LocalDate.now(), pageable);
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
                JobStatus1.COMPLETED,
                asyncJobService.getJobStatus(response.getJobId()).get().getStatus());

        // lets make sure the generated clob in the database is the same as the original
        try (CsvReader<ApplicationCodeCsvPojo> csvReaderFile =
                        new CsvReader<>(
                                getClass().getResourceAsStream("/appcodes.csv"),
                                ApplicationCodeCsvPojo.class);
                CsvReader<ApplicationCodeCsvPojo> csvReaderDbStream =
                        new CsvReader<>(response.read(), ApplicationCodeCsvPojo.class); ) {
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
                JobStatus1.FAILED,
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
        Assertions.assertTrue(codesAdded.isEmpty());
    }
}
