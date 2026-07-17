package uk.gov.hmcts.appregister.csds.ingress.audit;

import java.util.Arrays;

public enum CsdsAuditLevel {
    DEBUG,
    ERROR,
    NONE;

    public boolean auditsSuccesses() {
        return this == DEBUG;
    }

    public boolean auditsFailures() {
        return this != NONE;
    }

    public static CsdsAuditLevel fromDatabaseValue(String value) {
        return Arrays.stream(values())
                .filter(candidate -> candidate.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unsupported CSDS audit level: " + value));
    }
}
