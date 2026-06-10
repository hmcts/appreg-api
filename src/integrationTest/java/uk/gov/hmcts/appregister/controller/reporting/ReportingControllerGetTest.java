package uk.gov.hmcts.appregister.controller.reporting;

import static org.mockito.Mockito.when;

import io.restassured.response.Response;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import lombok.val;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.appregister.common.async.DeleteableFileOutputStream;
import uk.gov.hmcts.appregister.common.async.exception.JobError;
import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;
import uk.gov.hmcts.appregister.common.async.model.TrackJobStatusResponse;
import uk.gov.hmcts.appregister.common.async.reader.CsvReader;
import uk.gov.hmcts.appregister.common.async.reader.JpaDataReader;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobService;
import uk.gov.hmcts.appregister.common.async.writer.CsvWriter;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.common.util.AppRegTempFileUtil;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.report.audit.ReportAuditOperation;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.csv.ApplicationCodeCsvPojo;
import uk.gov.hmcts.appregister.testutils.csv.ApplicationCodeCsvReaderComparator;
import uk.gov.hmcts.appregister.testutils.csv.JobProcessCsvReadLifecycle;
import uk.gov.hmcts.appregister.testutils.csv.JobProcessCsvWriteLifecycle;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

class ReportingControllerGetTest extends BaseIntegration {
    public static final String WEB_CONTEXT = "reports/jobs/%s/download";

    @Autowired private UserProvider userProvider;

    @Autowired private AsyncJobService asyncJobService;

    @Autowired private ApplicationCodeRepository applicationCodeRepository;

    @Autowired private DataAuditRepository dataAuditRepository;

    private Function<Pageable, Page<ApplicationCode>> getApplicationCodesFunction =
            (pageable) -> {
                return applicationCodeRepository.search("CODE", null, LocalDate.now(), pageable);
            };

    @BeforeEach
    void setUp() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal())
                .thenReturn(TokenGenerator.builder().build().getJwtFromToken());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void givenCompletedJob_whenWeRequestToDownloadTheCSV_thenASuccessIsReturned() throws Exception {
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

        // now read the records
        JpaDataReader<ApplicationCode> reader = new JpaDataReader<>(getApplicationCodesFunction);

        JobProcessCsvWriteLifecycle csvWriterLifecycle =
                new JobProcessCsvWriteLifecycle(new CsvWriter<>(ApplicationCodeCsvPojo.class));

        // run the job wsriting the csv as we go eventually writing it to the database
        TrackJobStatusResponse response =
                asyncJobService.startJob(request, reader, csvWriterLifecycle);

        // wait for end of async job
        response.getFuture().get();

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT.formatted(response.getJobId().getId().toString())),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);
        responseSpec.then().contentType("text/csv");

        // now lets parse the response to make sure it is as expected
        File fileToWriteResponseTo = AppRegTempFileUtil.generateTempFile("reporting-download");
        try (InputStream inputStream = responseSpec.getBody().asInputStream();
                OutputStream fileOutputStream =
                        new DeleteableFileOutputStream(fileToWriteResponseTo)) {

            // copy the input stream to a file
            IOUtils.copy(inputStream, fileOutputStream);

            // lets make sure the clob is csv and it is as is expected
            try (CsvReader<ApplicationCodeCsvPojo> csvReaderFile =
                            new CsvReader<>(
                                    getClass().getResourceAsStream("/appcodes.csv"),
                                    ApplicationCodeCsvPojo.class);
                    CsvReader<ApplicationCodeCsvPojo> csvReaderDbStream =
                            new CsvReader<>(
                                    response.read().getInputStream(),
                                    ApplicationCodeCsvPojo.class); ) {
                Assertions.assertEquals(
                        0,
                        new ApplicationCodeCsvReaderComparator()
                                .compare(csvReaderFile, csvReaderDbStream));
            }
        }
    }

    @Test
    void givenCompletedJob_whenWeRequestToDownloadTheCSVAndOneDoesNotExist_thenASuccessIsReturned()
            throws Exception {

        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

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

            Response responseSpec =
                    restAssuredClient.executeGetRequest(
                            getLocalUrl(
                                    WEB_CONTEXT.formatted(response.getJobId().getId().toString())),
                            tokenGenerator.fetchTokenForRole());

            responseSpec.then().statusCode(400);
            ProblemAssertUtil.assertEquals(
                    JobError.JOB_DOES_NOT_HAVE_DATA_TO_GET_A_DOWNLOAD_STREAM.getCode(),
                    responseSpec);
        }
    }

    @Test
    void givenCompletedJob_whenWeDownloadTheCSV_thenDataAuditRowIsPersisted() throws Exception {
        val request =
                JobTypeRequest.builder()
                        .jobType(JobType.FEES_REPORT)
                        .userName(userProvider.getUserId())
                        .build();

        // Seed the application codes that the report writer will later read back out to CSV.
        try (val csvReaderForAppCode =
                new CsvReader<>(
                        getClass().getResourceAsStream("/appcodes.csv"),
                        ApplicationCodeCsvPojo.class)) {
            val jobProcessCsvReadLifecycle =
                    new JobProcessCsvReadLifecycle(applicationCodeRepository);
            val response =
                    asyncJobService.startJob(
                            request, csvReaderForAppCode, jobProcessCsvReadLifecycle);
            response.getFuture().get();
        }

        val reader = new JpaDataReader<>(getApplicationCodesFunction);
        val csvWriterLifecycle =
                new JobProcessCsvWriteLifecycle(new CsvWriter<>(ApplicationCodeCsvPojo.class));

        // Create the downloadable report clob that the endpoint will stream back to the caller.
        val response = asyncJobService.startJob(request, reader, csvWriterLifecycle);
        response.getFuture().get();

        val tokenGenerator = getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        // Remove earlier audit rows so the assertions below only inspect this download request.
        dataAuditRepository.deleteAll();

        val responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT.formatted(response.getJobId().getId().toString())),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);
        responseSpec.then().contentType("text/csv");
        try (InputStream responseStream = responseSpec.getBody().asInputStream()) {
            Assertions.assertTrue(responseStream.readAllBytes().length > 0);
        }

        assertDownloadReportJobAuditRow("jobId", response.getJobId().getId().toString());
        assertDownloadReportJobAuditRow("reportType", JobType.FEES_REPORT.toString());
        assertDownloadReportJobAuditRow("fileReference", "report.csv");
    }

    private void assertDownloadReportJobAuditRow(String columnName, String value) {
        val persistedAuditRow =
                dataAuditRepository.findAll().stream()
                        .filter(row -> "report_jobs".equals(row.getTableName()))
                        .filter(row -> columnName.equals(row.getColumnName()))
                        .filter(row -> value.equals(row.getNewValue()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                ("Expected a report_jobs.%s audit row for GET"
                                                                + " /reports/jobs/{jobId}/download")
                                                        .formatted(columnName)));

        Assertions.assertEquals("", persistedAuditRow.getOldValue());
        Assertions.assertEquals(
                ReportAuditOperation.DOWNLOAD_REPORT_AUDIT_EVENT.getEventName(),
                persistedAuditRow.getEventName());
        Assertions.assertEquals(
                ReportAuditOperation.DOWNLOAD_REPORT_AUDIT_EVENT.getType(),
                persistedAuditRow.getUpdateType());
    }
}
