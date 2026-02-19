package uk.gov.hmcts.appregister.common.enumeration;

/**
 * Enumeration representing the code type for name address types.
 */
public enum NameAddressCodeType {
    APPLICANT("NA"),
    RESPONDENT("RE");

    private final String code;

    NameAddressCodeType(String code) {
        this.code = code;
    }

    public static NameAddressCodeType fromCode(String code) {
        for (NameAddressCodeType status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown fee status: " + code);
    }

    public String getCode() {
        return code;
    }
}
