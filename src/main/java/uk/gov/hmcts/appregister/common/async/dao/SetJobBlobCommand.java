package uk.gov.hmcts.appregister.common.async.dao;

import uk.gov.hmcts.appregister.common.async.model.JobIdRequest;

import java.io.InputStream;

@FunctionalInterface
public interface SetJobBlobCommand {
    /**
     * writes the blob to an output stream
     * @param outputStream The output stream to write to.
     * @param jobId The job id of the job that is running.
     */
    void writeBlobToOutputStream(InputStream outputStream, JobIdRequest jobId);
}
