package uk.gov.hmcts.appregister.applicationentry.model;

import lombok.Getter;

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
