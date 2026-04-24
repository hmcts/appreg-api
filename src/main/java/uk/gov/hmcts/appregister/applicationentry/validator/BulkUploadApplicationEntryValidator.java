package uk.gov.hmcts.appregister.applicationentry.validator;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import uk.gov.hmcts.appregister.applicationentry.enumeration.BulkUploadFieldType;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadApplicationCommand;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadError;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

/**
 * Performs structural and business-rule validation for application entry bulk upload CSV files.
 */
@Component
public class BulkUploadApplicationEntryValidator {

    /**
     * Validates header row strictly (order + exact match).
     *
     * @param header the CSV header values supplied in the uploaded file
     */
    public void validateHeader(List<String> header) {
        List<String> expected = BulkUploadFieldType.expectedHeaders();

        if (header == null || header.size() < expected.size()) {
            throw new AppRegistryException(
                AppListEntryError.BULK_UPLOAD_INVALID_HEADERS,
                "CSV header row is missing, invalid, or does not match the expected structure"
            );
        }

        for (int i = 0; i < expected.size(); i++) {
            String actual = StringUtils.trimToEmpty(header.get(i));

            if (!expected.get(i).equals(actual)) {
                throw new AppRegistryException(
                    AppListEntryError.BULK_UPLOAD_INVALID_HEADERS,
                    "Header mismatch at column " + i
                        + ": expected '" + expected.get(i)
                        + "' but found '" + actual + "'"
                );
            }
        }
    }

    /**
     * Validates a single mapped upload row and returns all discovered row-level validation errors.
     *
     * @param rowNumber the 1-based CSV row number including the header row
     * @param command the mapped bulk upload command to validate
     * @return the list of validation errors for the supplied row
     */
    public List<BulkUploadError> validateRow(int rowNumber, BulkUploadApplicationCommand command) {
        List<BulkUploadError> errors = new ArrayList<>();

        // --- REQUIRED FIELDS ---

        if (StringUtils.isBlank(command.getApplicantCode())) {
            errors.add(new BulkUploadError(
                rowNumber,
                BulkUploadFieldType.APPLICANT_CODE.getHeader(),
                null,
                "Applicant code is required"));
        }

        if (StringUtils.isBlank(command.getApplicationCode())) {
            errors.add(new BulkUploadError(
                rowNumber,
                BulkUploadFieldType.APPLICATION_CODE.getHeader(),
                null,
                "Application code is required"));
        }

        // --- RESPONDENT RULES ---

        boolean hasOrganisation = StringUtils.isNotBlank(command.getRespondentOrganisationName());

        boolean hasPerson =
            StringUtils.isNotBlank(command.getRespondentForename1())
                || StringUtils.isNotBlank(command.getRespondentSurname());

        // Must not have both
        if (hasOrganisation && hasPerson) {
            errors.add(new BulkUploadError(
                rowNumber,
                "RESP_NAME_ORG/RESP_FORENAME/RESP_SURNAME",
                null,
                "Respondent cannot be both organisation and person"));
        }

        // Must have at least one
        if (!hasOrganisation && !hasPerson) {
            errors.add(new BulkUploadError(
                rowNumber,
                "RESP_NAME_ORG/RESP_FORENAME/RESP_SURNAME",
                null,
                "Respondent details must be provided"));
        }

        // --- EMAIL VALIDATION ---

        if (StringUtils.isNotBlank(command.getRespondentEmail())
            && !command.getRespondentEmail().matches(
                "([0-9A-Za-z'.\\-+_%]{1,126}@[0-9A-Za-z.\\-]{1,126})?")) {

            errors.add(new BulkUploadError(
                rowNumber,
                BulkUploadFieldType.RESP_EMAIL.getHeader(),
                command.getRespondentEmail(),
                "Invalid email format"));
        }

        // --- PHONE VALIDATION ---

        if (StringUtils.isNotBlank(command.getRespondentTelephone())
            && !command.getRespondentTelephone().matches("[0-9 \\-]*")) {

            errors.add(new BulkUploadError(
                rowNumber,
                BulkUploadFieldType.RESP_TEL.getHeader(),
                command.getRespondentTelephone(),
                "Invalid phone format"));
        }

        if (StringUtils.isNotBlank(command.getRespondentMobile())
            && !command.getRespondentMobile().matches("[0-9 \\-]*")) {

            errors.add(new BulkUploadError(
                rowNumber,
                BulkUploadFieldType.RESP_MOBILE.getHeader(),
                command.getRespondentMobile(),
                "Invalid mobile format"));
        }

        return errors;
    }
}
