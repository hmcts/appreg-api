package uk.gov.hmcts.appregister.common.enumeration;

public enum YesOrNo {
    YES("1"),
    NO("0");

    private final String value;

    YesOrNo(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
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
