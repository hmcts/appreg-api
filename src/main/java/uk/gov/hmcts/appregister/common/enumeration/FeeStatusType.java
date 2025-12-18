package uk.gov.hmcts.appregister.common.enumeration;

import lombok.Getter;

/**
 * Enumeration representing the status of a fee.
 */
@Getter
public enum FeeStatusType {
    DUE("D"),
    PAID("P"),
    REMITTED("R"),
    UNDERTAKING("U");

    private final String displayName;

    FeeStatusType(String displayName) {
        this.displayName = displayName;
    }

    public static FeeStatusType fromDisplayName(String displayName) {
        for (FeeStatusType status : values()) {
            if (status.displayName.equalsIgnoreCase(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown fee status: " + displayName);
    }

    public static FeeStatusType fromValue(String value) {
        for (FeeStatusType status : FeeStatusType.values()) {
            if (status.getDisplayName().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}
