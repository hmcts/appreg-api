package uk.gov.hmcts.appregister.controller.job;

import static org.mockito.Mockito.when;

import io.restassured.response.Response;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.appregister.common.async.exception.JobError;
import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;
import uk.gov.hmcts.appregister.common.async.model.TrackJobStatusResponse;
import uk.gov.hmcts.appregister.common.async.reader.CsvReader;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobService;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.job.audit.JobAuditOperation;
import uk.gov.hmcts.appregister.testutils.AwaitilityUtil;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.csv.ApplicationCodeCsvPojo;
import uk.gov.hmcts.appregister.testutils.csv.JobProcessCsvReadLifecycle;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

public class JobControllerSearchTest extends BaseIntegration {
    public static final String WEB_CONTEXT = "jobs";

    @Autowired private UserProvider userProvider;

    @Autowired private AsyncJobService asyncJobService;

    @Autowired private ApplicationCodeRepository applicationCodeRepository;

    @Autowired private DataAuditRepository dataAuditRepository;

    @BeforeEach
    public void setUp() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal())
                .thenReturn(TokenGenerator.builder().build().getJwtFromToken());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    public void givenJob_whenJobStatusRequested_thenASuccessIsReturned() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

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

        // fire off the job, let not wait
        TrackJobStatusResponse response =
                asyncJobService.startJob(request, csvReaderForAppCode, jobProcessCsvReadLifecycle);

        // wait for 30 seconds polling each second for the status to be true
        AwaitilityUtil.waitForMaxWithOneSecondPoll(
                () -> {
                    Response responseSpec =
                            restAssuredClient.executeGetRequest(
                                    getLocalUrl(
                                            WEB_CONTEXT
                                                    + "/"
                                                    + response.getJobId().getId().toString()),
                                    tokenGenerator.fetchTokenForRole());

                    // if not a 200 then fail
                    if (responseSpec.statusCode() != 200) {
                        return false;
                    } else {
                        JobAcknowledgement jobStatusResponse =
                                responseSpec.as(JobAcknowledgement.class);

                        // assert the response
                        responseSpec.then().statusCode(200);

                        // if not a completed or failed then fail
                        if (jobStatusResponse.getStatus() != JobStatus1.COMPLETED
                                && jobStatusResponse.getStatus() != JobStatus1.FAILED) {
                            return false;
                        }

                        Assertions.assertEquals(
                                JobStatus1.COMPLETED, jobStatusResponse.getStatus());
                        Assertions.assertEquals(JobType.FEES_REPORT, jobStatusResponse.getType());
                        Assertions.assertEquals(
                                response.getJobId().getId(), jobStatusResponse.getId());

                        return true;
                    }
                },
                Duration.ofSeconds(30));

        response.getFuture().get();
    }

    @Test
    public void givenJob_whenJobThatDoesNotExistIsRequested_thenAFailureIsReturned()
            throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + UUID.randomUUID()),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(404);
        ProblemAssertUtil.assertEquals(
                JobError.JOB_DOES_NOT_EXIST_OR_NOT_FOR_USER.getCode(), responseSpec);
    }

    @Test
    public void givenJob_whenJobExistsButNotForUser_thenAFailureIsReturned() throws Exception {
        // create the token
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

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

        // fire off a lookup for the job
        TrackJobStatusResponse response =
                asyncJobService.startJob(request, csvReaderForAppCode, jobProcessCsvReadLifecycle);

        // wait for the job to complete
        response.getFuture().get();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + response.getJobId().getId().toString()),
                        tokenGenerator.fetchTokenForRole());
        Assertions.assertEquals(200, responseSpec.statusCode());

        // now we know the job exists lets get a token for a different user
        // so that a job match will not exist

        // token for a different user
        tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).oid("34").build();

        responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + response.getJobId().getId().toString()),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(404);
        ProblemAssertUtil.assertEquals(
                JobError.JOB_DOES_NOT_EXIST_OR_NOT_FOR_USER.getCode(), responseSpec);
    }

    @Test
    public void givenJob_whenJobStatusRequested_thenDataAuditRowIsPersisted() throws Exception {
        val tokenGenerator = getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        val request =
                JobTypeRequest.builder()
                        .jobType(JobType.FEES_REPORT)
                        .userName(userProvider.getUserId())
                        .build();

        val csvReaderForAppCode =
                new CsvReader<>(
                        getClass().getResourceAsStream("/appcodes.csv"),
                        ApplicationCodeCsvPojo.class);
        val jobProcessCsvReadLifecycle = new JobProcessCsvReadLifecycle(applicationCodeRepository);

        // Start a real async job so the GET endpoint can read back a persisted asynch_jobs row.
        val response =
                asyncJobService.startJob(request, csvReaderForAppCode, jobProcessCsvReadLifecycle);

        // Wait for the job to finish before querying it through the API to keep the test stable.
        response.getFuture().get();

        // Clear out any earlier audit rows so the assertions below only inspect this GET request.
        dataAuditRepository.deleteAll();

        val responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + response.getJobId().getId()),
                        tokenGenerator.fetchTokenForRole());

        responseSpec.then().statusCode(200);
        val jobStatusResponse = responseSpec.as(JobAcknowledgement.class);
        Assertions.assertEquals(response.getJobId().getId(), jobStatusResponse.getId());

        // Read the persisted data_audit rows directly and verify the GET audit contains the
        // requested job UUID under the async jobs table.
        val persistedAuditRow =
                dataAuditRepository.findAll().stream()
                        .filter(row -> TableNames.ASYNC_JOBS.equals(row.getTableName()))
                        .filter(row -> "id".equals(row.getColumnName()))
                        .filter(
                                row ->
                                        response.getJobId()
                                                .getId()
                                                .toString()
                                                .equals(row.getNewValue()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected an asynch_jobs.id audit row for GET /jobs/{jobId}"));

        Assertions.assertEquals("", persistedAuditRow.getOldValue());
        Assertions.assertEquals(
                JobAuditOperation.GET_JOB_STATUS_AUDIT_EVENT.getEventName(),
                persistedAuditRow.getEventName());
        Assertions.assertEquals(
                JobAuditOperation.GET_JOB_STATUS_AUDIT_EVENT.getType(),
                persistedAuditRow.getUpdateType());
    }
}
