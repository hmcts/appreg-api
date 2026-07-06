package uk.gov.hmcts.appregister.common.enumeration;

import lombok.Getter;

@Getter
public enum YesOrNo {
    YES("Y"),
    NO("N");

    private final String value;

    YesOrNo(String value) {
        this.value = value;
    }

    public static YesOrNo fromValue(String value) {
        for (YesOrNo yesOrNo : YesOrNo.values()) {
            if (yesOrNo.value.equalsIgnoreCase(value)) {
                return yesOrNo;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }

    public boolean isYes() {
        return this == YES;
    }
}
