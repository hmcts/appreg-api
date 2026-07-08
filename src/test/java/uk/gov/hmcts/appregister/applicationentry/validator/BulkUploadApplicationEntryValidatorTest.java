package uk.gov.hmcts.appregister.applicationentry.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadError;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadRow;

class BulkUploadApplicationEntryValidatorTest {

    private final BulkUploadApplicationEntryValidator validator =
            new BulkUploadApplicationEntryValidator();

    @Test
    void givenValidOrganisationRow_whenValidateRow_thenReturnsNoErrors() {
        BulkUploadRow row = validOrganisationRow();

        List<BulkUploadError> errors = validator.validateRow(2, row);

        assertThat(errors).isEmpty();
    }

    @Test
    void givenBlankApplicantCode_whenValidateRow_thenReturnsApplicantCodeError() {
        BulkUploadRow row = validOrganisationRow();
        row.setApplicantCode(" ");

        List<BulkUploadError> errors = validator.validateRow(2, row);

        String name =
                row.getRespondentOrganisationName() != null
                        ? row.getRespondentOrganisationName()
                        : row.getRespondentForename1() + " " + row.getRespondentSurname();

        assertThat(errors)
                .containsExactly(
                        new BulkUploadError(
                                2,
                                "APPLICANT_CODE",
                                null,
                                "Applicant code is required",
                                row.getRespondentAddressLine1(),
                                name,
                                "DATA_ERROR"));
    }

    @Test
    void givenBlankApplicationCode_whenValidateRow_thenReturnsApplicationCodeError() {
        BulkUploadRow row = validOrganisationRow();
        row.setApplicationCode("");

        List<BulkUploadError> errors = validator.validateRow(2, row);

        String name =
                row.getRespondentOrganisationName() != null
                        ? row.getRespondentOrganisationName()
                        : row.getRespondentForename1() + " " + row.getRespondentSurname();

        assertThat(errors)
                .containsExactly(
                        new BulkUploadError(
                                2,
                                "APPLICATION_CODE",
                                null,
                                "Application code is required",
                                row.getRespondentAddressLine1(),
                                name,
                                "DATA_ERROR"));
    }

    @Test
    void givenOrganisationAndPersonRespondent_whenValidateRow_thenReturnsMutualExclusionError() {
        BulkUploadRow row = validOrganisationRow();
        row.setRespondentForename1("Jane");
        row.setRespondentSurname("Jones");

        List<BulkUploadError> errors = validator.validateRow(3, row);
        String name =
                row.getRespondentOrganisationName() != null
                        ? row.getRespondentOrganisationName()
                        : row.getRespondentForename1() + " " + row.getRespondentSurname();

        assertThat(errors)
                .containsExactly(
                        new BulkUploadError(
                                3,
                                "RESP_NAME_ORG/RESP_FORENAME1/RESP_SURNAME/RESP_FIRST_NAME/RESP_LAST_NAME",
                                null,
                                "Respondent cannot be both organisation and person",
                                row.getRespondentAddressLine1(),
                                name,
                                "DATA_ERROR"));
    }

    @Test
    void givenNoRespondentDetails_whenValidateRow_thenReturnsRespondentRequiredError() {
        BulkUploadRow row = validOrganisationRow();
        row.setRespondentOrganisationName(null);

        List<BulkUploadError> errors = validator.validateRow(4, row);

        String name = null;
        if (row.getRespondentOrganisationName() != null
                || (row.getRespondentForename1() != null && row.getRespondentSurname() != null)) {
            name =
                    row.getRespondentOrganisationName() != null
                            ? row.getRespondentOrganisationName()
                            : row.getRespondentForename1() + " " + row.getRespondentSurname();
        }

        assertThat(errors)
                .containsExactly(
                        new BulkUploadError(
                                4,
                                "RESP_NAME_ORG/RESP_FORENAME1/RESP_SURNAME/RESP_FIRST_NAME/RESP_LAST_NAME",
                                null,
                                "Respondent details must be provided",
                                row.getRespondentAddressLine1(),
                                name,
                                "DATA_ERROR"));
    }

    @Test
    void givenMultipleInvalidFields_whenValidateRow_thenReturnsAllErrors() {
        BulkUploadRow row = validOrganisationRow();
        row.setApplicantCode(null);
        row.setApplicationCode(null);
        row.setRespondentOrganisationName(null);

        List<BulkUploadError> errors = validator.validateRow(5, row);
        String name = null;
        if (row.getRespondentOrganisationName() != null
                || (row.getRespondentForename1() != null && row.getRespondentSurname() != null)) {
            name =
                    row.getRespondentOrganisationName() != null
                            ? row.getRespondentOrganisationName()
                            : row.getRespondentForename1() + " " + row.getRespondentSurname();
        }
        assertThat(errors)
                .containsExactly(
                        new BulkUploadError(
                                5,
                                "APPLICANT_CODE",
                                null,
                                "Applicant code is required",
                                row.getRespondentAddressLine1(),
                                name,
                                "DATA_ERROR"),
                        new BulkUploadError(
                                5,
                                "APPLICATION_CODE",
                                null,
                                "Application code is required",
                                row.getRespondentAddressLine1(),
                                name,
                                "DATA_ERROR"),
                        new BulkUploadError(
                                5,
                                "RESP_NAME_ORG/RESP_FORENAME1/RESP_SURNAME/RESP_FIRST_NAME/RESP_LAST_NAME",
                                null,
                                "Respondent details must be provided",
                                row.getRespondentAddressLine1(),
                                name,
                                "DATA_ERROR"));
    }

    private static BulkUploadRow validOrganisationRow() {
        BulkUploadRow row = new BulkUploadRow();
        row.setApplicantCode("APP001");
        row.setApplicationCode("AD99001");
        row.setRespondentOrganisationName("Test Organisation");
        return row;
    }
}
