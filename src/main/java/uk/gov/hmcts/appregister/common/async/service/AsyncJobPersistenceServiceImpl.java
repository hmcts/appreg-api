package uk.gov.hmcts.appregister.common.async.service;

import jakarta.persistence.EntityManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.common.async.DeleteableFileInputStream;
import uk.gov.hmcts.appregister.common.async.mapper.JobStatusMapper;
import uk.gov.hmcts.appregister.common.async.model.JobIdRequest;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;
import uk.gov.hmcts.appregister.common.entity.AsyncJob;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.entity.repository.AsyncJobRepository;
import uk.gov.hmcts.appregister.common.enumeration.JobStatusType;
import uk.gov.hmcts.appregister.common.util.AppRegTempFileUtil;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;

/**
 * A persistence layer to control the asynchronous job persistence.
 */
@Component
@RequiredArgsConstructor
public class AsyncJobPersistenceServiceImpl implements AsyncJobPersistenceService {
    /** The schema from the Spring configuration. */
    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    /** Gets hold of the clob stream. */
    private static final String JDBC_CLOB_QUERY = "SELECT csv_output, id FROM %s WHERE id = ?";

    /** Update the clob with a stream. */
    // NOSONAR - SQL injection is not possible here as the id is a UUID.
    private static final String JDBC_INSERT_CLOB_QUERY =
            "UPDATE %s SET csv_output = ? WHERE id = ?";

    /** The jdbc template to use to interact with the database. */
    private final JdbcTemplate jdbcTemplate;

    private final AsyncJobRepository asyncJobRepository;

    private final EntityManager entityManager;

    private final JobStatusMapper jobStatusMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void setJobStatus(JobIdRequest jobType, JobStatus1 jobStatus) {
        // do nothing
        AsyncJob asyncJob = asyncJobRepository.findByJobId(jobType.getId(), jobType.getUserName());

        // map the status to the enum.
        asyncJob.setJobState(jobStatusMapper.getJobStatus(jobStatus));
        asyncJobRepository.save(asyncJob);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void setFailure(JobIdRequest jobType, String reasonFailed) {
        AsyncJob asyncJob = asyncJobRepository.findByJobId(jobType.getId(), jobType.getUserName());

        asyncJob.setJobState(JobStatusType.FAILED);

        // set the failure message. Truncate to 4000 characters.
        asyncJob.setFailureMessage(truncate(reasonFailed, 4000));
        asyncJobRepository.save(asyncJob);
    }

    @Override
    public Optional<JobStatusResponse> getJobStatus(JobIdRequest job) {
        AsyncJob asyncJob = asyncJobRepository.findByJobId(job.getId(), job.getUserName());
        if (asyncJob == null) {
            return Optional.empty();
        }

        JobStatusResponse jobStatusResponse =
                JobStatusResponse.builder()
                        .status(jobStatusMapper.getJobStatus(asyncJob.getJobState()))
                        .persistence(this)
                        .uuid(asyncJob.getUuid())
                        .userName(asyncJob.getUserName())
                        .type(JobType.fromValue(asyncJob.getJobType()))
                        .errorMessage(asyncJob.getFailureMessage())
                        .build();
        return Optional.ofNullable(jobStatusResponse);
    }

    @Override
    public boolean isJobTypeFinishedForUser(JobTypeRequest id) {
        AsyncJob asyncJob =
                asyncJobRepository.findByJobTypeAndUser(
                        id.getJobType().getValue(), id.getUserName());

        if (asyncJob != null) {
            return asyncJob.getJobState() == JobStatusType.COMPLETED
                    || asyncJob.getJobState() == JobStatusType.FAILED;
        }

        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobIdRequest startJob(JobTypeRequest request) {
        AsyncJob asyncJob = new AsyncJob();
        asyncJob.setJobType(request.getJobType().getValue());
        asyncJob.setJobState(JobStatusType.SUBMITTED);

        // save the start job
        asyncJobRepository.save(asyncJob);

        asyncJob = refreshEntity(asyncJob);

        return JobIdRequest.builder()
                .id(asyncJob.getUuid())
                .userName(request.getUserName())
                .build();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeClob(JobIdRequest jobIdRequest, InputStream inputStream) throws IOException {
        setClob(inputStream, jobIdRequest);
    }

    @Override
    public InputStreamResource readClob(JobIdRequest jobIdRequest) throws IOException {
        return getClobToOutputStream(jobIdRequest);
    }

    private InputStreamResource getClobToOutputStream(JobIdRequest jobId) throws IOException {
        File file = AppRegTempFileUtil.generateTempFile("async-job-clob");

        jdbcTemplate.query(
                JDBC_CLOB_QUERY.formatted(schema + "." + TableNames.ASYNC_JOBS),
                ps -> ps.setObject(1, jobId.getId()),
                rs -> {
                    try (Reader reader = rs.getCharacterStream(1);
                            Writer writer =
                                    Files.newBufferedWriter(
                                            file.toPath(), StandardCharsets.UTF_8)) {
                        if (reader != null) {
                            reader.transferTo(writer);
                        }
                    } catch (IOException e) {
                        throw new SQLException(e);
                    }
                });

        if (file.length() > 0) {
            // return the spring input stream resource
            return new InputStreamResource(new DeleteableFileInputStream(file));
        } else {
            Files.deleteIfExists(file.toPath());
        }
        return null;
    }

    /**
     * sets the clob in the database.
     *
     * @param inputStream The input stream to write to the database.
     * @param jobId The job id we are setting the clob on.
     */
    public void setClob(InputStream inputStream, JobIdRequest jobId) {
        jdbcTemplate.execute(
                // NOSONAR
                JDBC_INSERT_CLOB_QUERY.formatted(
                        schema + "." + TableNames.ASYNC_JOBS), // NOSONAR - SQL injection is not
                // possible here as we use a prepared statement. The only dynamic configurable
                // pieces are the schema
                // which comes from Spring config.

                (PreparedStatementCallback<Void>)
                        ps -> {
                            ps.setObject(2, jobId.getId());
                            try (Reader reader =
                                    new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                                ps.setCharacterStream(1, reader);
                                ps.executeUpdate();
                            } catch (IOException e) {
                                throw new SQLException(e);
                            }
                            return null;
                        });
    }

    /**
     * Reloads the entity so DB-generated fields (e.g. UUID via gen_random_uuid()) are available
     * immediately after save. Calls: - flush(): force the INSERT - refresh(): reselect the row with
     * DB defaults/triggers
     */
    private AsyncJob refreshEntity(AsyncJob entity) {
        entityManager.flush();
        entityManager.refresh(entity);
        return entity;
    }

    private static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }

        return str.length() <= maxLength ? str : str.substring(0, maxLength);
    }
}
