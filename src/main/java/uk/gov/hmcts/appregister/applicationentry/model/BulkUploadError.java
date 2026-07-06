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

    @Override
    public String toString() {
        StringBuilder error =
                new StringBuilder(30)
                        .append("Row ")
                        .append(rowNumber)
                        .append(" [")
                        .append(location)
                        .append(']');

        if (rejectedValue != null) {
            error.append(" rejected value [").append(rejectedValue).append("]");
        }

        return error.append(": ").append(message).toString();
    }
}
