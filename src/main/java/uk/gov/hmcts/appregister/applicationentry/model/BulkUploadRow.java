package uk.gov.hmcts.appregister.applicationentry.model;

import com.opencsv.bean.CsvBindAndJoinByName;
import com.opencsv.bean.CsvBindByName;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.appregister.common.async.model.CsvPojo;

/**
 * CSV-backed row model used to bind application entry bulk upload records before mapping and
 * validation.
 */
@Getter
@Setter
public class BulkUploadRow implements CsvPojo {
    private static final Pattern APPLICATION_TEXT_COLUMN_PATTERN =
            Pattern.compile("APPLICATION_TEXT(\\d+)");

    // --- APPLICANT ---

    @CsvBindByName(column = "APPLICANT_CODE", required = true)
    private String applicantCode;

    // --- RESPONDENT DETAILS ---

    @CsvBindByName(column = "RESP_TITLE")
    private String respondentTitle;

    @CsvBindByName(column = "RESP_NAME_ORG")
    private String respondentOrganisationName;

    @CsvBindByName(column = "RESP_FORENAME1")
    private String respondentForename1;

    @CsvBindByName(column = "RESP_FORENAME2")
    private String respondentForename2;

    @CsvBindByName(column = "RESP_FORENAME3")
    private String respondentForename3;

    @CsvBindByName(column = "RESP_SURNAME")
    private String respondentSurname;

    @CsvBindByName(column = "RESP_ADDLINE1")
    private String respondentAddressLine1;

    @CsvBindByName(column = "RESP_ADDLINE2")
    private String respondentAddressLine2;

    @CsvBindByName(column = "RESP_ADDLINE3")
    private String respondentAddressLine3;

    @CsvBindByName(column = "RESP_ADDLINE4")
    private String respondentAddressLine4;

    @CsvBindByName(column = "RESP_ADDLINE5")
    private String respondentAddressLine5;

    @CsvBindByName(column = "RESP_POSTCODE")
    private String respondentPostcode;

    @CsvBindByName(column = "RESP_EMAIL")
    private String respondentEmail;

    @CsvBindByName(column = "RESP_TEL")
    private String respondentTelephone;

    @CsvBindByName(column = "RESP_MOBILE")
    private String respondentMobile;

    // --- ACCOUNT ---

    @CsvBindByName(column = "ACCOUNT_NUMBER")
    private String accountNumber;

    // --- APPLICATION ---

    @CsvBindByName(column = "APPLICATION_CODE", required = true)
    private String applicationCode;

    // --- WORDING FIELDS ---

    @CsvBindAndJoinByName(column = "APPLICATION_TEXT\\d+", elementType = String.class)
    private MultiValuedMap<String, String> applicationTexts;

    public static boolean hasRespondentOrganisation(BulkUploadRow row) {
        return row != null && StringUtils.isNotBlank(row.getRespondentOrganisationName());
    }

    public static boolean hasRespondentPerson(BulkUploadRow row) {
        return row != null
                && (StringUtils.isNotBlank(row.getRespondentForename1())
                        || StringUtils.isNotBlank(row.getRespondentSurname()));
    }

    public List<String> getApplicationTextValues() {
        if (applicationTexts == null || applicationTexts.isEmpty()) {
            return List.of();
        }

        return applicationTexts.asMap().entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> applicationTextIndex(entry.getKey())))
                .flatMap(entry -> entry.getValue().stream())
                .toList();
    }

    private static int applicationTextIndex(String columnName) {
        Matcher matcher = APPLICATION_TEXT_COLUMN_PATTERN.matcher(columnName);
        if (!matcher.matches()) {
            return Integer.MAX_VALUE;
        }

        return Integer.parseInt(matcher.group(1));
    }
}
