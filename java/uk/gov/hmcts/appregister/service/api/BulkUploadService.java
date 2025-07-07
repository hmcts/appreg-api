package uk.gov.hmcts.appregister.service.api;

import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.appregister.dto.read.bulkupload.BulkUploadResponseDto;

public interface BulkUploadService {

    /**
     * Uploads a CSV file and returns the result of the upload.
     *
     * @param file the CSV file to upload
     * @return a BulkUploadResponseDto containing the result of the upload
     */
    BulkUploadResponseDto uploadCsv(Long listId, MultipartFile file, String userId);
}
