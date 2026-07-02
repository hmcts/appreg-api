package uk.gov.hmcts.appregister.common.log;

/**
 * Which payload direction(s) to log for an annotated endpoint.
 */
public enum PayloadLogDirection {
    REQUEST,
    RESPONSE,
    BOTH;

    public boolean includesRequest() {
        return this == REQUEST || this == BOTH;
    }

    public boolean includesResponse() {
        return this == RESPONSE || this == BOTH;
    }
}
