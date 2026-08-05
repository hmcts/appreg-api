package uk.gov.hmcts.appregister.common.async.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.appregister.common.async.mapper.JobStatusMapper;
import uk.gov.hmcts.appregister.common.async.model.JobIdRequest;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;
import uk.gov.hmcts.appregister.common.entity.AsyncJob;
import uk.gov.hmcts.appregister.common.entity.repository.AsyncJobRepository;
import uk.gov.hmcts.appregister.common.enumeration.JobStatusType;
import uk.gov.hmcts.appregister.generated.model.JobStatus;
import uk.gov.hmcts.appregister.generated.model.JobType;

class AsyncJobPersistenceServiceImplTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final AsyncJobRepository asyncJobRepository = mock(AsyncJobRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final JobStatusMapper jobStatusMapper = mock(JobStatusMapper.class);

    private AsyncJobPersistenceServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new AsyncJobPersistenceServiceImpl(
                        jdbcTemplate, asyncJobRepository, entityManager, jobStatusMapper);
        ReflectionTestUtils.setField(service, "schema", "public");
    }

    @Test
    void setJobStatus_updatesAndSavesEntity() {
        var jobId = JobIdRequest.builder().id(UUID.randomUUID()).userName("tester").build();
        var asyncJob = AsyncJob.builder().uuid(jobId.getId()).userName("tester").build();
        when(asyncJobRepository.findByJobId(jobId.getId(), "tester")).thenReturn(asyncJob);
        when(jobStatusMapper.getJobStatus(JobStatus.PROCESSING)).thenReturn(JobStatusType.RUNNING);

        service.setJobStatus(jobId, JobStatus.PROCESSING);

        assertEquals(JobStatusType.RUNNING, asyncJob.getJobState());
        verify(asyncJobRepository).save(asyncJob);
    }

    @Test
    void getJobStatus_returnsEmptyWhenJobMissing() {
        var jobId = JobIdRequest.builder().id(UUID.randomUUID()).userName("tester").build();
        when(asyncJobRepository.findByJobId(jobId.getId(), "tester")).thenReturn(null);

        assertTrue(service.getJobStatus(jobId).isEmpty());
    }

    @Test
    void getJobStatus_mapsAsyncJobToResponse() {
        var uuid = UUID.randomUUID();
        var jobId = JobIdRequest.builder().id(uuid).userName("tester").build();
        var asyncJob =
                AsyncJob.builder()
                        .uuid(uuid)
                        .userName("tester")
                        .jobState(JobStatusType.RUNNING)
                        .jobType(JobType.FEES_REPORT.getValue())
                        .failureMessage("bad")
                        .build();
        when(asyncJobRepository.findByJobId(uuid, "tester")).thenReturn(asyncJob);
        when(jobStatusMapper.getJobStatus(JobStatusType.RUNNING)).thenReturn(JobStatus.PROCESSING);

        Optional<JobStatusResponse> response = service.getJobStatus(jobId);

        assertTrue(response.isPresent());
        assertEquals(JobStatus.PROCESSING, response.orElseThrow().getStatus());
        assertEquals(JobType.FEES_REPORT, response.orElseThrow().getType());
        assertEquals("bad", response.orElseThrow().getErrorMessage());
    }

    @Test
    void isJobTypeFinishedForUser_returnsExpectedState() {
        var request =
                JobTypeRequest.builder().jobType(JobType.FEES_REPORT).userName("tester").build();
        var runningJob = AsyncJob.builder().jobState(JobStatusType.RUNNING).build();
        when(asyncJobRepository.findByJobTypeAndUser(JobType.FEES_REPORT.getValue(), "tester"))
                .thenReturn(runningJob);

        assertFalse(service.isJobTypeFinishedForUser(request));

        runningJob.setJobState(JobStatusType.COMPLETED);
        assertTrue(service.isJobTypeFinishedForUser(request));

        when(asyncJobRepository.findByJobTypeAndUser(JobType.FEES_REPORT.getValue(), "tester"))
                .thenReturn(null);
        assertTrue(service.isJobTypeFinishedForUser(request));
    }

    @Test
    void startJob_savesRefreshesAndReturnsIdentifier() {
        var uuid = UUID.randomUUID();
        doAnswer(
                        invocation -> {
                            var asyncJob = invocation.getArgument(0, AsyncJob.class);
                            asyncJob.setUuid(uuid);
                            return asyncJob;
                        })
                .when(asyncJobRepository)
                .save(any(AsyncJob.class));
        doAnswer(
                        invocation -> {
                            var asyncJob = invocation.getArgument(0, AsyncJob.class);
                            asyncJob.setUuid(uuid);
                            return null;
                        })
                .when(entityManager)
                .refresh(any(AsyncJob.class));
        var request =
                JobTypeRequest.builder()
                        .jobType(JobType.DURATION_REPORT)
                        .userName("tester")
                        .build();

        var response = service.startJob(request);

        assertEquals(uuid, response.getId());
        assertEquals("tester", response.getUserName());
        verify(entityManager).flush();
    }

    @Test
    void setFailure_allowsNullReason() {
        var jobId = JobIdRequest.builder().id(UUID.randomUUID()).userName("tester").build();
        var asyncJob = AsyncJob.builder().uuid(jobId.getId()).userName("tester").build();
        when(asyncJobRepository.findByJobId(jobId.getId(), "tester")).thenReturn(asyncJob);

        service.setFailure(jobId, null);

        assertEquals(JobStatusType.FAILED, asyncJob.getJobState());
        assertNull(asyncJob.getFailureMessage());
    }

    @Test
    void writeClob_writesCharacterStreamToStatement() throws Exception {
        var jobId = JobIdRequest.builder().id(UUID.randomUUID()).userName("tester").build();
        var preparedStatement = mock(PreparedStatement.class);
        var input = new ByteArrayInputStream("csv".getBytes(StandardCharsets.UTF_8));

        doAnswer(
                        invocation -> {
                            @SuppressWarnings("unchecked")
                            PreparedStatementCallback<Void> callback = invocation.getArgument(1);
                            return callback.doInPreparedStatement(preparedStatement);
                        })
                .when(jdbcTemplate)
                .execute(anyString(), Mockito.<PreparedStatementCallback<Void>>any());

        service.writeClob(jobId, input);

        verify(preparedStatement).setObject(2, jobId.getId());
        verify(preparedStatement).setCharacterStream(anyInt(), any(Reader.class));
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void readClob_returnsResourceWhenQueryWritesContent() throws Exception {
        var jobId = JobIdRequest.builder().id(UUID.randomUUID()).userName("tester").build();
        var preparedStatement = mock(PreparedStatement.class);
        var resultSet = mock(ResultSet.class);
        when(resultSet.getCharacterStream(1)).thenReturn(new StringReader("csv"));

        doAnswer(
                        invocation -> {
                            var setter = invocation.getArgument(1, PreparedStatementSetter.class);
                            var callbackHandler =
                                    invocation.getArgument(2, RowCallbackHandler.class);
                            setter.setValues(preparedStatement);
                            callbackHandler.processRow(resultSet);
                            return null;
                        })
                .when(jdbcTemplate)
                .query(
                        anyString(),
                        any(PreparedStatementSetter.class),
                        any(RowCallbackHandler.class));

        var resource = service.readClob(jobId);

        assertNotNull(resource);
        try (var inputStream = resource.getInputStream()) {
            assertEquals("csv", new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        }
        verify(preparedStatement).setObject(1, jobId.getId());
    }

    @Test
    void readClob_returnsNullWhenQueryProducesNoContent() throws Exception {
        var jobId = JobIdRequest.builder().id(UUID.randomUUID()).userName("tester").build();
        var preparedStatement = mock(PreparedStatement.class);
        var resultSet = mock(ResultSet.class);
        when(resultSet.getCharacterStream(1)).thenReturn(null);

        doAnswer(
                        invocation -> {
                            var setter = invocation.getArgument(1, PreparedStatementSetter.class);
                            var callbackHandler =
                                    invocation.getArgument(2, RowCallbackHandler.class);
                            setter.setValues(preparedStatement);
                            callbackHandler.processRow(resultSet);
                            return null;
                        })
                .when(jdbcTemplate)
                .query(
                        anyString(),
                        any(PreparedStatementSetter.class),
                        any(RowCallbackHandler.class));

        assertNull(service.readClob(jobId));
    }

    @Test
    void setFailure_noTruncationWhenMessageIsLongerThanMaxLength() {
        var jobId = JobIdRequest.builder().id(UUID.randomUUID()).userName("tester").build();
        var asyncJob = AsyncJob.builder().uuid(jobId.getId()).userName("tester").build();
        when(asyncJobRepository.findByJobId(jobId.getId(), "tester")).thenReturn(asyncJob);
        // Original maxLength was 4000, so we create a failure message that is longer than that to
        // test truncation
        var failureMessage = "x".repeat(5000);

        service.setFailure(jobId, failureMessage);

        assertEquals(JobStatusType.FAILED, asyncJob.getJobState());
        assertEquals(5000, asyncJob.getFailureMessage().length());
        verify(asyncJobRepository).save(asyncJob);
    }
}
