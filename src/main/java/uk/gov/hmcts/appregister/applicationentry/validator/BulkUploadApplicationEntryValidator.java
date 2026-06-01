package uk.gov.hmcts.appregister.applicationentry.validator;

import com.opencsv.bean.CsvBindByName;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadError;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadRow;

/**
 * Performs structural and business-rule validation for application entry bulk upload CSV files.
 */
@Component
public class BulkUploadApplicationEntryValidator {

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
                            rowNumber,
                            columnName("applicantCode"),
                            null,
                            "Applicant code is required"));
        }

        if (StringUtils.isBlank(row.getApplicationCode())) {
            errors.add(
                    new BulkUploadError(
                            rowNumber,
                            columnName("applicationCode"),
                            null,
                            "Application code is required"));
        }

        // --- RESPONDENT RULES ---

        boolean hasOrganisation = BulkUploadRow.hasRespondentOrganisation(row);
        boolean hasPerson = BulkUploadRow.hasRespondentPerson(row);

        // Must not have both
        if (hasOrganisation && hasPerson) {
            errors.add(
                    new BulkUploadError(
                            rowNumber,
                            columnNames(),
                            null,
                            "Respondent cannot be both organisation and person"));
        }

        // Must have at least one
        if (!hasOrganisation && !hasPerson) {
            errors.add(
                    new BulkUploadError(
                            rowNumber, columnNames(), null, "Respondent details must be provided"));
        }

        return errors;
    }

    private static String columnNames() {
        return Arrays.stream(
                        new String[] {
                            "respondentOrganisationName",
                            "respondentForename1",
                            "respondentSurname",
                            "respondentFirstName",
                            "respondentLastName"
                        })
                .map(BulkUploadApplicationEntryValidator::columnName)
                .collect(Collectors.joining("/"));
    }

    private static String columnName(String fieldName) {
        try {
            Field field = BulkUploadRow.class.getDeclaredField(fieldName);
            CsvBindByName binding = field.getAnnotation(CsvBindByName.class);

            if (binding == null) {
                throw new IllegalStateException(
                        "Bulk upload row field %s is missing @CsvBindByName".formatted(fieldName));
            }

            return binding.column();
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(
                    "Bulk upload row field %s does not exist".formatted(fieldName), e);
        }
    }
}
