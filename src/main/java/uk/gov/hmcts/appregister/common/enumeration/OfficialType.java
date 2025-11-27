package uk.gov.hmcts.appregister.common.enumeration;

import lombok.Getter;

@Getter
public enum OfficialType {
    MAGISTRATE("M"),

    CLERK("C");

    private final String value;

    OfficialType(String value) {
        this.value = value;
    }

    public static OfficialType fromDisplayName(String displayName) {
        for (OfficialType type : values()) {
            if (type.getValue().equalsIgnoreCase(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown type : " + displayName);
    }

    public static OfficialType fromValue(String value) {
        for (OfficialType status : OfficialType.values()) {
            if (status.getValue().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown type: " + value);
    }
}
