package uk.gov.hmcts.appregister.common.async;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Component;

import uk.gov.hmcts.appregister.common.async.dao.JobBlobDao;
import uk.gov.hmcts.appregister.common.async.exception.JobError;
import uk.gov.hmcts.appregister.common.async.model.JobIdRequest;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.JobStatus;
import uk.gov.hmcts.appregister.generated.model.JobType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JobStatusPersistenceImpl implements JobStatusPersistence {
    private final String JDBC_BLOB_QUERY = "content FROM files WHERE id = ?";

    private final JobBlobDao jobBlobDao;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void setJobStatus(JobIdRequest jobType, JobStatus jobStatus) {

    }

    @Override
    public Optional<JobStatusResponse> getJobStatus(JobIdRequest id) {
        return Optional.empty();
    }

    @Override
    public JobIdRequest startJob(JobTypeRequest request) {
        return null;
    }

    @Override
    public void writeBlob(JobIdRequest jobIdRequest, InputStream inputStream) {
        setBlobToOutputStream(inputStream, jobIdRequest);
    }

    @Override
    public void readBlob(JobIdRequest jobIdRequest, OutputStream outputStream) {
        getBlobToOutputStream(outputStream, jobIdRequest);
    }

    private void getBlobToOutputStream(OutputStream outputStream, JobIdRequest jobId) {
        try {
            jdbcTemplate.query(
                JDBC_BLOB_QUERY,
                ps -> ps.setString(1, jobId.getId()),
                rs -> {
                    if (rs.next()) {
                        try (InputStream in = rs.getBinaryStream("content")) {
                            if (in != null) {
                                in.transferTo(outputStream);
                                outputStream.flush();
                            }
                        } catch (IOException e) {
                            throw new SQLException(e);
                        }
                    }
                }
            );
        } catch (DataAccessException e) {
            throw new AppRegistryException(
                JobError.JOB_DOES_NOT_EXIST_OR_NOT_FOR_USER, "No blob found for job id: "
                + jobId.getId() + " and user: " + jobId.getUserName(), e);
        }
    }

    public void setBlobToOutputStream(InputStream outputStream, JobIdRequest jobId) {
        try {
            jdbcTemplate.execute(
                "INSERT INTO files (name, content) VALUES (?, ?)",
                (PreparedStatementCallback<Void>) ps -> {
                    ps.setString(1, "file.pdf");
                    ps.executeUpdate();
                    return null;
                }
            );
        } catch (DataAccessException e) {
            throw new AppRegistryException(JobError.JOB_DOES_NOT_EXIST_OR_NOT_FOR_USER, "No blob found for job id: "
                + jobId.getId() + " and user: " + jobId.getUserName(), e);
        }
    }
}
