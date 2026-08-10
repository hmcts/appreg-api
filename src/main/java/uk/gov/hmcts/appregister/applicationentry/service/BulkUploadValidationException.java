package uk.gov.hmcts.appregister.applicationentry.service;

/**
 * Carries a row-aware, explicitly approved bulk-upload validation message through the processing
 * phase without exposing an arbitrary exception message.
 */
final class BulkUploadValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int rowNumber;
    private final String location;
    private final String clientMessage;

    BulkUploadValidationException(
            int rowNumber, String location, String clientMessage, Throwable cause) {
        super("Recognised bulk-upload validation failure", cause);
        this.rowNumber = rowNumber;
        this.location = location;
        this.clientMessage = clientMessage;
    }

    int rowNumber() {
        return rowNumber;
    }

    String location() {
        return location;
    }

    String clientMessage() {
        return clientMessage;
    }
}
