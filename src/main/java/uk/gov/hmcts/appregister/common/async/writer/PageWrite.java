package uk.gov.hmcts.appregister.common.async.writer;

import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;
import uk.gov.hmcts.appregister.common.async.reader.ReadPagePosition;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface PageWrite<T> extends Closeable {
    public boolean write(ReadPagePosition position, JobTypeRequest jobTypeRequest, List<T> csv);

    InputStream getInputStream() throws IOException;
}
