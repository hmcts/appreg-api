package uk.gov.hmcts.appregister.applicationentry.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents a validation or processing failure for a specific row and location in a bulk upload
 * file.
 */
@Data
@AllArgsConstructor
public class BulkUploadError {
    private int rowNumber;
    private String location;
    private String rejectedValue;
    private String message;
    private String addressLine1;
    private String name;
    private String errorType;
}
