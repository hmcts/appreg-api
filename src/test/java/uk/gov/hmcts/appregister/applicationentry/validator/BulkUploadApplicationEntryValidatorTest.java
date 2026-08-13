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

        assertThat(errors)
                .containsExactly(
                        new BulkUploadError(
                                2,
                                "standardApplicantCode",
                                null,
                                "Applicant code is required",
                                row.getRespondentAddressLine1(),
                                row.getApplicantCode(),
                                "DATA_ERROR"));
    }

    @Test
    void givenBlankApplicationCode_whenValidateRow_thenReturnsApplicationCodeError() {
        BulkUploadRow row = validOrganisationRow();
        row.setApplicationCode("");

        List<BulkUploadError> errors = validator.validateRow(2, row);

        assertThat(errors)
                .containsExactly(
                        new BulkUploadError(
                                2,
                                "applicationCode",
                                null,
                                "Application code is required",
                                row.getRespondentAddressLine1(),
                                row.getApplicantCode(),
                                "DATA_ERROR"));
    }

    @Test
    void givenOrganisationAndPersonRespondent_whenValidateRow_thenReturnsMutualExclusionError() {
        BulkUploadRow row = validOrganisationRow();
        row.setRespondentForename1("Jane");
        row.setRespondentSurname("Jones");

        List<BulkUploadError> errors = validator.validateRow(3, row);

        assertThat(errors)
                .containsExactly(
                        new BulkUploadError(
                                3,
                                "RESPONDENT",
                                null,
                                "Respondent cannot be both organisation and person",
                                row.getRespondentAddressLine1(),
                                row.getApplicantCode(),
                                "DATA_ERROR"));
    }

    @Test
    void givenNoRespondentDetails_whenValidateRow_thenReturnsNoErrors() {
        BulkUploadRow row = validOrganisationRow();
        row.setRespondentOrganisationName(null);

        List<BulkUploadError> errors = validator.validateRow(4, row);

        assertThat(errors).isEmpty();
    }

    @Test
    void
            givenCanonicalOrganisationAndPersonNames_whenValidateRow_thenReturnsMutualExclusionError() {
        BulkUploadRow row = validOrganisationRow();
        row.setRespondentFirstName("Jane");
        row.setRespondentLastName("Jones");

        List<BulkUploadError> errors = validator.validateRow(3, row);

        assertThat(errors)
                .containsExactly(
                        new BulkUploadError(
                                3,
                                "RESPONDENT",
                                null,
                                "Respondent cannot be both organisation and person",
                                row.getRespondentAddressLine1(),
                                row.getApplicantCode(),
                                "DATA_ERROR"));
    }

    @Test
    void givenWhitespaceRespondentDetails_whenValidateRow_thenReturnsNoErrors() {
        BulkUploadRow row = validOrganisationRow();
        row.setRespondentOrganisationName(" ");
        row.setRespondentFirstName(" ");
        row.setRespondentLastName("\t");

        List<BulkUploadError> errors = validator.validateRow(4, row);

        assertThat(errors).isEmpty();
    }

    @Test
    void givenContactDetailsWithoutRespondentName_whenValidateRow_thenReturnsRespondentError() {
        BulkUploadRow row = validOrganisationRow();
        row.setRespondentOrganisationName(null);
        row.setRespondentAddressLine1("1 Example Street");

        List<BulkUploadError> errors = validator.validateRow(4, row);

        assertThat(errors)
                .containsExactly(
                        new BulkUploadError(
                                4,
                                "RESPONDENT",
                                null,
                                "Respondent details are missing. Enter either Organisation Name, or"
                                        + " Respondent First Name and Last Name.",
                                row.getRespondentAddressLine1(),
                                row.getApplicantCode(),
                                "DATA_ERROR"));
    }

    @Test
    void givenMultipleInvalidFields_whenValidateRow_thenReturnsAllErrors() {
        BulkUploadRow row = validOrganisationRow();
        row.setApplicantCode(null);
        row.setApplicationCode(null);
        row.setRespondentOrganisationName(null);

        List<BulkUploadError> errors = validator.validateRow(5, row);
        assertThat(errors)
                .containsExactly(
                        new BulkUploadError(
                                5,
                                "standardApplicantCode",
                                null,
                                "Applicant code is required",
                                row.getRespondentAddressLine1(),
                                row.getApplicantCode(),
                                "DATA_ERROR"),
                        new BulkUploadError(
                                5,
                                "applicationCode",
                                null,
                                "Application code is required",
                                row.getRespondentAddressLine1(),
                                row.getApplicantCode(),
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
