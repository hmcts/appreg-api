package uk.gov.hmcts.appregister.common.async.dao;

import uk.gov.hmcts.appregister.common.async.model.JobIdRequest;

import java.io.OutputStream;

@FunctionalInterface
public interface GetJobBlobCommand {
    /**
     * writes the blob to an output stream
     * @param outputStream The output stream to write to.
     * @param jobId The job id of the job that is running.
     */
    void setBlobToOutputStream(OutputStream outputStream, JobIdRequest jobId);
}
