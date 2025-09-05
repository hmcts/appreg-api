package uk.gov.hmcts.appregister.applicationentry.service;

import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.appregister.applicationentry.dto.BulkUploadResponseDto;

/** Service interface for handling bulk uploads of CSV files. */
public interface BulkUploadService {

  /**
   * Uploads a CSV file and returns the result of the upload.
   *
   * @param file the CSV file to upload
   * @return a BulkUploadResponseDto containing the result of the upload
   */
  BulkUploadResponseDto uploadCsv(Long listId, MultipartFile file, String userId);
}
