package uk.gov.hmcts.appregister.applicationentry.model;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.async.model.CsvPojo;

/**
 * CSV-backed row model used to bind application entry bulk upload records before mapping and
 * validation.
 */
@Getter
@Setter
public class BulkUploadRow implements CsvPojo {

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

    @CsvBindByName(column = "APPLICATION_TEXT1")
    private String applicationText1;

    @CsvBindByName(column = "APPLICATION_TEXT2")
    private String applicationText2;
}
