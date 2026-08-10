package uk.gov.hmcts.appregister.applicationentry.service;

import java.util.List;

/**
 * Groups recognised row-level bulk-upload validation failures from one processing page.
 */
class BulkUploadValidationFailuresException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<BulkUploadValidationException> failures;

    BulkUploadValidationFailuresException(List<BulkUploadValidationException> failures) {
        super("Recognised bulk-upload validation failures", failures.getFirst());
        this.failures = List.copyOf(failures);
    }

    List<BulkUploadValidationException> failures() {
        return failures;
    }
}
