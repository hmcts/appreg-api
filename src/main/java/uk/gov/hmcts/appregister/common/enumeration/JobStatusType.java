package uk.gov.hmcts.appregister.common.enumeration;

import lombok.Getter;

@Getter
public enum JobStatusType {
    SUBMITTED("SUBMITTED"),
    PENDING("PENDING"),
    RUNNING("RUNNING"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    private final String state;

    JobStatusType(String state) {
        this.state = state;
    }

    public static JobStatusType fromStateString(String displayName) {
        for (JobStatusType status : values()) {
            if (status.state.equalsIgnoreCase(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown fee status: " + displayName);
    }
}
