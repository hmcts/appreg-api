package uk.gov.hmcts.appregister.applicationentry.model;

import com.opencsv.bean.CsvBindAndJoinByName;
import com.opencsv.bean.CsvBindByName;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
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

    @CsvBindByName(column = "APPLICANT_CODE")
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

    @CsvBindByName(column = "RESP_FIRST_NAME")
    private String respondentFirstName;

    @CsvBindByName(column = "RESP_MIDDLE_NAME")
    private String respondentMiddleName;

    @CsvBindByName(column = "RESP_LAST_NAME")
    private String respondentLastName;

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

    @CsvBindByName(column = "APPLICATION_CODE")
    private String applicationCode;

    // --- WORDING FIELDS ---

    @CsvBindAndJoinByName(column = "APPLICATION_TEXT\\d+", elementType = String.class)
    private MultiValuedMap<String, String> applicationTexts;

    public static boolean hasRespondentOrganisation(BulkUploadRow row) {
        return row != null && StringUtils.isNotBlank(row.respondentOrganisationName);
    }

    public static boolean hasRespondentPerson(BulkUploadRow row) {
        return row != null
                && (StringUtils.isNotBlank(row.getRespondentFirstNameValue())
                        || StringUtils.isNotBlank(row.getRespondentLastNameValue()));
    }

    public static boolean hasAnyRespondentDetails(BulkUploadRow row) {
        return row != null
                && Stream.of(
                                row.respondentTitle,
                                row.respondentOrganisationName,
                                row.respondentForename1,
                                row.respondentForename2,
                                row.respondentForename3,
                                row.respondentSurname,
                                row.respondentFirstName,
                                row.respondentMiddleName,
                                row.respondentLastName,
                                row.respondentAddressLine1,
                                row.respondentAddressLine2,
                                row.respondentAddressLine3,
                                row.respondentAddressLine4,
                                row.respondentAddressLine5,
                                row.respondentPostcode,
                                row.respondentEmail,
                                row.respondentTelephone,
                                row.respondentMobile)
                        .anyMatch(StringUtils::isNotBlank);
    }

    public static RespondentNameState respondentNameState(BulkUploadRow row) {
        boolean hasOrganisation = hasRespondentOrganisation(row);
        boolean hasPerson = hasRespondentPerson(row);

        if (hasOrganisation && hasPerson) {
            return RespondentNameState.CONFLICTING;
        }
        if (hasOrganisation) {
            return RespondentNameState.ORGANISATION;
        }
        if (hasPerson) {
            return RespondentNameState.PERSON;
        }
        return RespondentNameState.MISSING;
    }

    public enum RespondentNameState {
        MISSING,
        ORGANISATION,
        PERSON,
        CONFLICTING
    }

    public String getRespondentFirstNameValue() {
        return firstNonBlank(respondentFirstName, respondentForename1);
    }

    public String getRespondentMiddleNameValue() {
        return firstNonBlank(
                respondentMiddleName,
                uk.gov.hmcts.appregister.common.mapper.ApplicantMapper.combineMiddleName(
                        respondentForename2, respondentForename3));
    }

    public String getRespondentLastNameValue() {
        return firstNonBlank(respondentLastName, respondentSurname);
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

    private static String firstNonBlank(String primary, String fallback) {
        String value = StringUtils.trimToNull(primary);
        return value != null ? value : StringUtils.trimToNull(fallback);
    }
}
