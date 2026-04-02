package uk.gov.hmcts.appregister.common.async.dao;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Component;

import uk.gov.hmcts.appregister.common.async.exception.JobError;
import uk.gov.hmcts.appregister.common.async.model.JobIdRequest;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class SetJobBlobCommandImpl implements GetJobBlobCommand {
    private final JdbcTemplate jdbcTemplate;
    private final String JDBC_BLOB_QUERY = "INSERT INTO files (name, content) VALUES (?, ?)";

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
