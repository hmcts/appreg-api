package uk.gov.hmcts.appregister.common.enumeration;

import lombok.Getter;

@Getter
public enum Status {
    OPEN("OPEN"),
    CLOSED("CLOSED");

    private final String value;

    Status(String value) {
        this.value = value;
    }

    public static Status fromValue(String value) {
        for (Status status : Status.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}


