package uk.gov.hmcts.appregister.csds.ingress.database;

import java.util.List;

public class CsdsBatchUpsertException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final transient List<FailedUpsertRecord<?>> failures;

    public CsdsBatchUpsertException(
            String message, Throwable cause, List<FailedUpsertRecord<?>> failures) {
        super(message, cause);
        this.failures = List.copyOf(failures);
    }

    public List<FailedUpsertRecord<?>> failures() {
        return failures;
    }

    public int failureCount() {
        return failures.size();
    }

    public String clientMessage() {
        return "CSDS ingest failed for %d row(s). See csds_audit for row-level errors."
                .formatted(failureCount());
    }

    public String logSummary() {
        return "%s. Failed rows=%d.".formatted(getMessage(), failureCount());
    }
}
