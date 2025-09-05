package uk.gov.hmcts.appregister.common.enumeration;

import lombok.Getter;

/** Enumeration representing the status of a fee. */
@Getter
public enum FeeStatusType {
    DUE("Due"),
    PAID("Paid"),
    REMITTED("Remitted"),
    UNDERTAKING("Undertaking");

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
}
