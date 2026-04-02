package uk.gov.hmcts.appregister.common.async.dao;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.async.model.JobIdRequest;
import uk.gov.hmcts.appregister.common.async.exception.JobError;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;

@Component
@RequiredArgsConstructor
public class JobBlobDao {
    private final JdbcTemplate jdbcTemplate;
    private final String JDBC_BLOB_QUERY = "content FROM files WHERE id = ?";

    public void getBlobToOutputStream(OutputStream outputStream, JobIdRequest jobId) {
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
            throw new AppRegistryException(JobError.JOB_DOES_NOT_EXIST_OR_NOT_FOR_USER, "No blob found for job id: "
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
