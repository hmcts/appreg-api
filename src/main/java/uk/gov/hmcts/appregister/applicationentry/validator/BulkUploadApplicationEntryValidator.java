package uk.gov.hmcts.appregister.applicationentry.validator;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadError;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadRow;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadRow.RespondentNameState;

/**
 * Performs structural and business-rule validation for application entry bulk upload CSV files.
 */
@Component
public class BulkUploadApplicationEntryValidator {
    public static final String RESPONDENT_LOCATION = "RESPONDENT";
    public static final String RESPONDENT_MISSING_MESSAGE =
            "Respondent details are missing. Enter either Organisation Name, or Respondent First"
                    + " Name and Last Name.";

    /**
     * Validates a single mapped upload row and returns all discovered row-level validation errors.
     *
     * @param rowNumber the 1-based CSV row number including the header row
     * @param row the parsed bulk upload row to validate
     * @return the list of validation errors for the supplied row
     */
    public List<BulkUploadError> validateRow(int rowNumber, BulkUploadRow row) {
        List<BulkUploadError> errors = new ArrayList<>();
        RespondentNameState respondentNameState = BulkUploadRow.respondentNameState(row);
        final String errorType = "DATA_ERROR";

        // --- REQUIRED FIELDS ---

        if (StringUtils.isBlank(row.getApplicantCode())) {
            errors.add(
                    new BulkUploadError(
                            rowNumber,
                            "standardApplicantCode",
                            null,
                            "Applicant code is required",
                            row.getRespondentAddressLine1(),
                            row.getApplicantCode(),
                            errorType));
        }

        if (StringUtils.isBlank(row.getApplicationCode())) {
            errors.add(
                    new BulkUploadError(
                            rowNumber,
                            "applicationCode",
                            null,
                            "Application code is required",
                            row.getRespondentAddressLine1(),
                            row.getApplicantCode(),
                            errorType));
        }

        // --- RESPONDENT RULES ---

        // Must not have both
        if (respondentNameState == RespondentNameState.CONFLICTING) {
            errors.add(
                    new BulkUploadError(
                            rowNumber,
                            RESPONDENT_LOCATION,
                            null,
                            "Respondent cannot be both organisation and person",
                            row.getRespondentAddressLine1(),
                            row.getApplicantCode(),
                            errorType));
        }

        // Partial respondent details must not be silently discarded when the code does not require
        // a respondent.
        if (respondentNameState == RespondentNameState.MISSING
                && BulkUploadRow.hasAnyRespondentDetails(row)) {
            errors.add(
                    new BulkUploadError(
                            rowNumber,
                            RESPONDENT_LOCATION,
                            null,
                            RESPONDENT_MISSING_MESSAGE,
                            row.getRespondentAddressLine1(),
                            row.getApplicantCode(),
                            errorType));
        }

        return errors;
    }
}
