package uk.gov.hmcts.appregister.applicationentry.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

class BulkUploadCsvFormatValidatorTest {

    private final BulkUploadCsvFormatValidator validator = new BulkUploadCsvFormatValidator();

    @Test
    void givenLegacyHeaders_whenValidate_thenPasses() {
        assertDoesNotThrow(
                () ->
                        validator.validate(
                                csvFile(
                                        "RESP_FORENAME1|RESP_FORENAME2|RESP_FORENAME3|RESP_SURNAME\n")));
    }

    @Test
    void givenCanonicalHeaders_whenValidate_thenPasses() {
        assertDoesNotThrow(
                () ->
                        validator.validate(
                                csvFile("RESP_FIRST_NAME|RESP_MIDDLE_NAME|RESP_LAST_NAME\n")));
    }

    @Test
    void givenMixedLegacyAndCanonicalHeaders_whenValidate_thenThrows() {
        AppRegistryException exception =
                assertThrows(
                        AppRegistryException.class,
                        () ->
                                validator.validate(
                                        csvFile(
                                                "RESP_FORENAME1|RESP_FORENAME2|RESP_FORENAME3|RESP_SURNAME|"
                                                        + "RESP_FIRST_NAME\n")));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_INVALID_FILE_FORMAT);
        assertThat(exception)
                .hasMessageContaining("either legacy respondent name columns or canonical");
    }

    @Test
    void givenPartialLegacyHeaders_whenValidate_thenThrows() {
        AppRegistryException exception =
                assertThrows(
                        AppRegistryException.class,
                        () ->
                                validator.validate(
                                        csvFile("RESP_FORENAME1|RESP_FORENAME2|RESP_SURNAME\n")));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_INVALID_FILE_FORMAT);
        assertThat(exception).hasMessageContaining("Legacy bulk upload files must include");
    }

    @Test
    void givenPartialCanonicalHeaders_whenValidate_thenThrows() {
        AppRegistryException exception =
                assertThrows(
                        AppRegistryException.class,
                        () -> validator.validate(csvFile("RESP_FIRST_NAME|RESP_LAST_NAME\n")));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_INVALID_FILE_FORMAT);
        assertThat(exception).hasMessageContaining("Canonical bulk upload files must include");
    }

    @Test
    void givenNoNameHeaders_whenValidate_thenThrows() {
        AppRegistryException exception =
                assertThrows(
                        AppRegistryException.class,
                        () -> validator.validate(csvFile("APPLICANT_CODE|APPLICATION_CODE\n")));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_INVALID_FILE_FORMAT);
        assertThat(exception)
                .hasMessageContaining(
                        "must include either legacy respondent name columns or canonical");
    }

    @Test
    void givenBlankHeaderRow_whenValidate_thenThrows() {
        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(csvFile("\n")));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_INVALID_FILE_FORMAT);
        assertThat(exception).hasMessageContaining("must contain a header row");
    }

    @Test
    void givenUnreadableFile_whenValidate_thenThrows() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("boom"));

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(file));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_INVALID_FILE_FORMAT);
        assertThat(exception).hasMessageContaining("could not be read");
    }

    private static MultipartFile csvFile(String content) {
        MultipartFile file = mock(MultipartFile.class);
        try {
            when(file.getInputStream())
                    .thenReturn(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
        return file;
    }
}
