package uk.gov.hmcts.appregister.applicationentry.enumeration;

import java.util.List;
import lombok.Getter;

/**
 * Defines the supported CSV headers for application entry bulk uploads, including their expected
 * order.
 */
@Getter
public enum BulkUploadFieldType {
    APPLICANT_CODE("APPLICANT_CODE", 0, true),

    RESP_TITLE("RESP_TITLE", 1, false),
    RESP_NAME_ORG("RESP_NAME_ORG", 2, false),
    RESP_FORENAME1("RESP_FORENAME1", 3, false),
    RESP_FORENAME2("RESP_FORENAME2", 4, false),
    RESP_FORENAME3("RESP_FORENAME3", 5, false),
    RESP_SURNAME("RESP_SURNAME", 6, false),

    RESP_ADDLINE1("RESP_ADDLINE1", 7, false),
    RESP_ADDLINE2("RESP_ADDLINE2", 8, false),
    RESP_ADDLINE3("RESP_ADDLINE3", 9, false),
    RESP_ADDLINE4("RESP_ADDLINE4", 10, false),
    RESP_ADDLINE5("RESP_ADDLINE5", 11, false),

    RESP_POSTCODE("RESP_POSTCODE", 12, false),
    RESP_EMAIL("RESP_EMAIL", 13, false),
    RESP_TEL("RESP_TEL", 14, false),
    RESP_MOBILE("RESP_MOBILE", 15, false),

    ACCOUNT_NUMBER("ACCOUNT_NUMBER", 16, false),

    APPLICATION_CODE("APPLICATION_CODE", 17, true),

    APPLICATION_TEXT_1("APPLICATION_TEXT1", 18, false),
    APPLICATION_TEXT_2("APPLICATION_TEXT2", 19, false);

    /** Minimum number of columns required for a CSV row to include the mandatory upload fields. */
    public static final int MINIMUM_FIELDS = 18;

    private final String header;
    private final int index;
    private final boolean required;

    BulkUploadFieldType(String header, int index, boolean required) {
        this.header = header;
        this.index = index;
        this.required = required;
    }

    /**
     * Returns expected headers in strict positional order.
     *
     * @return the expected CSV header names in their required order
     */
    public static List<String> expectedHeaders() {
        return List.of(
                APPLICANT_CODE.header,
                RESP_TITLE.header,
                RESP_NAME_ORG.header,
                RESP_FORENAME1.header,
                RESP_FORENAME2.header,
                RESP_FORENAME3.header,
                RESP_SURNAME.header,
                RESP_ADDLINE1.header,
                RESP_ADDLINE2.header,
                RESP_ADDLINE3.header,
                RESP_ADDLINE4.header,
                RESP_ADDLINE5.header,
                RESP_POSTCODE.header,
                RESP_EMAIL.header,
                RESP_TEL.header,
                RESP_MOBILE.header,
                ACCOUNT_NUMBER.header,
                APPLICATION_CODE.header,
                APPLICATION_TEXT_1.header,
                APPLICATION_TEXT_2.header);
    }
}
