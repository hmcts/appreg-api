package uk.gov.hmcts.appregister.applicationentry.validator;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadError;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadRow;

/**
 * Performs structural and business-rule validation for application entry bulk upload CSV files.
 */
@Component
public class BulkUploadApplicationEntryValidator {

    private static final String APPLICANT_CODE = "APPLICANT_CODE";
    private static final String APPLICATION_CODE = "APPLICATION_CODE";
    private static final String RESP_EMAIL = "RESP_EMAIL";
    private static final String RESP_TEL = "RESP_TEL";
    private static final String RESP_MOBILE = "RESP_MOBILE";

    /**
     * Validates a single mapped upload row and returns all discovered row-level validation errors.
     *
     * @param rowNumber the 1-based CSV row number including the header row
     * @param row the parsed bulk upload row to validate
     * @return the list of validation errors for the supplied row
     */
    public List<BulkUploadError> validateRow(int rowNumber, BulkUploadRow row) {
        List<BulkUploadError> errors = new ArrayList<>();

        // --- REQUIRED FIELDS ---

        if (StringUtils.isBlank(row.getApplicantCode())) {
            errors.add(
                    new BulkUploadError(
                            rowNumber, APPLICANT_CODE, null, "Applicant code is required"));
        }

        if (StringUtils.isBlank(row.getApplicationCode())) {
            errors.add(
                    new BulkUploadError(
                            rowNumber, APPLICATION_CODE, null, "Application code is required"));
        }

        // --- RESPONDENT RULES ---

        boolean hasOrganisation = StringUtils.isNotBlank(row.getRespondentOrganisationName());

        boolean hasPerson =
                StringUtils.isNotBlank(row.getRespondentForename1())
                        || StringUtils.isNotBlank(row.getRespondentSurname());

        // Must not have both
        if (hasOrganisation && hasPerson) {
            errors.add(
                    new BulkUploadError(
                            rowNumber,
                            "RESP_NAME_ORG/RESP_FORENAME/RESP_SURNAME",
                            null,
                            "Respondent cannot be both organisation and person"));
        }

        // Must have at least one
        if (!hasOrganisation && !hasPerson) {
            errors.add(
                    new BulkUploadError(
                            rowNumber,
                            "RESP_NAME_ORG/RESP_FORENAME/RESP_SURNAME",
                            null,
                            "Respondent details must be provided"));
        }

        // --- EMAIL VALIDATION ---

        if (StringUtils.isNotBlank(row.getRespondentEmail())
                && !row.getRespondentEmail()
                        .matches("([0-9A-Za-z'.\\-+_%]{1,126}@[0-9A-Za-z.\\-]{1,126})?")) {

            errors.add(
                    new BulkUploadError(
                            rowNumber,
                            RESP_EMAIL,
                            row.getRespondentEmail(),
                            "Invalid email format"));
        }

        // --- PHONE VALIDATION ---

        if (StringUtils.isNotBlank(row.getRespondentTelephone())
                && !row.getRespondentTelephone().matches("[0-9 \\-]*")) {

            errors.add(
                    new BulkUploadError(
                            rowNumber,
                            RESP_TEL,
                            row.getRespondentTelephone(),
                            "Invalid phone format"));
        }

        if (StringUtils.isNotBlank(row.getRespondentMobile())
                && !row.getRespondentMobile().matches("[0-9 \\-]*")) {

            errors.add(
                    new BulkUploadError(
                            rowNumber,
                            RESP_MOBILE,
                            row.getRespondentMobile(),
                            "Invalid mobile format"));
        }

        return errors;
    }
}
