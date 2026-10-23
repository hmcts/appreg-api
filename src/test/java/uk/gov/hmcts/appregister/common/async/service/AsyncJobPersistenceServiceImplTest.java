package uk.gov.hmcts.appregister.common.async.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.UncategorizedSQLException;
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
import uk.gov.hmcts.appregister.common.util.AppRegTempFileUtil;
import uk.gov.hmcts.appregister.generated.model.JobStatus;
import uk.gov.hmcts.appregister.generated.model.JobType;

class AsyncJobPersistenceServiceImplTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final AsyncJobRepository asyncJobRepository = mock(AsyncJobRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final JobStatusMapper jobStatusMapper = mock(JobStatusMapper.class);

    private AsyncJobPersistenceServiceImpl service;

    @TempDir private Path tempDirectory;

    private Path reportFile;
    private MockedStatic<AppRegTempFileUtil> tempFileFactory;

    @BeforeEach
    void setUp() throws IOException {
        service =
                new AsyncJobPersistenceServiceImpl(
                        jdbcTemplate, asyncJobRepository, entityManager, jobStatusMapper);
        ReflectionTestUtils.setField(service, "schema", "public");
        reportFile = Files.createTempFile(tempDirectory, "async-job-clob-", ".appregtmp");
        tempFileFactory = mockStatic(AppRegTempFileUtil.class);
        tempFileFactory
                .when(() -> AppRegTempFileUtil.generateTempFile("async-job-clob"))
                .thenReturn(reportFile.toFile());
    }

    @AfterEach
    void closeTempFileFactory() {
        tempFileFactory.close();
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
        var resultSet = mock(ResultSet.class);
        when(resultSet.getCharacterStream(1)).thenReturn(new StringReader("csv"));
        final var preparedStatement = stubClobQuery(resultSet);

        var resource = service.readClob(jobId);

        assertNotNull(resource);
        assertTrue(Files.exists(reportFile));
        try (var inputStream = resource.getInputStream()) {
            assertEquals("csv", new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            assertTrue(Files.exists(reportFile));
        }
        assertFalse(Files.exists(reportFile));
        verify(preparedStatement).setObject(1, jobId.getId());
    }

    @Test
    void readClob_returnsNullWhenQueryProducesNoContent() throws Exception {
        var jobId = JobIdRequest.builder().id(UUID.randomUUID()).userName("tester").build();
        var resultSet = mock(ResultSet.class);
        when(resultSet.getCharacterStream(1)).thenReturn(null);
        stubClobQuery(resultSet);

        assertNull(service.readClob(jobId));
        assertFalse(Files.exists(reportFile));
    }

    @Test
    void given_queryFails_when_readingClob_then_tempFileIsDeletedAndOriginalFailureIsPreserved() {
        var jobId = JobIdRequest.builder().id(UUID.randomUUID()).userName("tester").build();
        var failure = new DataAccessResourceFailureException("Report query failed");
        Mockito.doThrow(failure)
                .when(jdbcTemplate)
                .query(
                        anyString(),
                        any(PreparedStatementSetter.class),
                        any(RowCallbackHandler.class));

        var thrown =
                assertThrows(
                        DataAccessResourceFailureException.class, () -> service.readClob(jobId));

        assertSame(failure, thrown);
        assertFalse(Files.exists(reportFile));
    }

    @Test
    void given_clobAccessFails_when_readingClob_then_tempFileIsDeleted() throws Exception {
        var jobId = JobIdRequest.builder().id(UUID.randomUUID()).userName("tester").build();
        var resultSet = mock(ResultSet.class);
        var failure = new SQLException("CLOB unavailable");
        when(resultSet.getCharacterStream(1)).thenThrow(failure);
        stubClobQuery(resultSet);

        var thrown = assertThrows(UncategorizedSQLException.class, () -> service.readClob(jobId));

        assertSame(failure, thrown.getCause());
        assertFalse(Files.exists(reportFile));
    }

    @Test
    void given_copyFailsAfterWriting_when_readingClob_then_partialFileIsDeletedAndReaderIsClosed()
            throws Exception {
        var resultSet = mock(ResultSet.class);
        var reader = mock(Reader.class);
        var failure = new IOException("Report copy interrupted");
        when(resultSet.getCharacterStream(1)).thenReturn(reader);
        doAnswer(
                        invocation -> {
                            invocation.getArgument(0, Writer.class).write("partial report");
                            throw failure;
                        })
                .when(reader)
                .transferTo(any(Writer.class));
        stubClobQuery(resultSet);

        var jobId = JobIdRequest.builder().id(UUID.randomUUID()).userName("tester").build();
        var thrown = assertThrows(UncategorizedSQLException.class, () -> service.readClob(jobId));

        assertSame(failure, thrown.getCause().getCause());
        assertFalse(Files.exists(reportFile));
        verify(reader).close();
    }

    @Test
    void given_noRows_when_readingClob_then_emptyTempFileIsDeleted() throws IOException {
        var jobId = JobIdRequest.builder().id(UUID.randomUUID()).userName("tester").build();

        assertNull(service.readClob(jobId));
        assertFalse(Files.exists(reportFile));
    }

    @Test
    void given_cleanupAlsoFails_when_queryFails_then_cleanupFailureIsSuppressed() {
        var jobId = JobIdRequest.builder().id(UUID.randomUUID()).userName("tester").build();
        var failure = new DataAccessResourceFailureException("Report query failed");
        doAnswer(
                        invocation -> {
                            // A nonempty directory reliably makes deletion fail without depending
                            // on platform-specific file permissions.
                            Files.delete(reportFile);
                            Files.createDirectory(reportFile);
                            Files.writeString(reportFile.resolve("partial.csv"), "partial report");
                            throw failure;
                        })
                .when(jdbcTemplate)
                .query(
                        anyString(),
                        any(PreparedStatementSetter.class),
                        any(RowCallbackHandler.class));

        var thrown =
                assertThrows(
                        DataAccessResourceFailureException.class, () -> service.readClob(jobId));

        assertSame(failure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertTrue(thrown.getSuppressed()[0] instanceof DirectoryNotEmptyException);
    }

    private PreparedStatement stubClobQuery(ResultSet resultSet) {
        var preparedStatement = mock(PreparedStatement.class);

        doAnswer(
                        invocation -> {
                            var setter = invocation.getArgument(1, PreparedStatementSetter.class);
                            var callbackHandler =
                                    invocation.getArgument(2, RowCallbackHandler.class);
                            try {
                                setter.setValues(preparedStatement);
                                callbackHandler.processRow(resultSet);
                            } catch (SQLException exception) {
                                throw new UncategorizedSQLException(
                                        "Read report CLOB", "SELECT csv_output", exception);
                            }
                            return null;
                        })
                .when(jdbcTemplate)
                .query(
                        anyString(),
                        any(PreparedStatementSetter.class),
                        any(RowCallbackHandler.class));
        return preparedStatement;
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
