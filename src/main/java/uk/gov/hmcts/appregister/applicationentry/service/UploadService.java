package uk.gov.hmcts.appregister.applicationentry.service;

import java.io.IOException;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;

public interface UploadService {
    JobAcknowledgement uploadAppEntryCsv(UUID listId, MultipartFile file) throws IOException;
}
