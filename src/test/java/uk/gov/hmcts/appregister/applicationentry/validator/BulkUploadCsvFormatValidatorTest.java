package uk.gov.hmcts.appregister.applicationentry.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

class BulkUploadCsvFormatValidatorTest {

    private final BulkUploadCsvFormatValidator validator = new BulkUploadCsvFormatValidator();

    @ParameterizedTest
    @ValueSource(
            strings = {
                "RESP_FORENAME1|RESP_FORENAME2|RESP_FORENAME3|RESP_SURNAME\n",
                "RESP_FIRST_NAME|RESP_MIDDLE_NAME|RESP_LAST_NAME\n"
            })
    void givenSupportedHeaders_whenValidate_thenPasses(String headers) {
        MultipartFile file = csvFile(headers);

        assertDoesNotThrow(() -> validator.validate(file));
    }

    @ParameterizedTest
    @MethodSource("invalidHeaderCases")
    void givenInvalidHeaders_whenValidate_thenThrows(String headers, String expectedMessage) {
        MultipartFile file = csvFile(headers);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(file));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_UPLOAD_INVALID_FILE_FORMAT);
        assertThat(exception).hasMessageContaining(expectedMessage);
    }

    @Test
    void givenBlankHeaderRow_whenValidate_thenThrows() {
        MultipartFile file = csvFile("\n");

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(file));

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

    private static Stream<Arguments> invalidHeaderCases() {
        return Stream.of(
                Arguments.of(
                        "RESP_FORENAME1|RESP_FORENAME2|RESP_FORENAME3|RESP_SURNAME|RESP_FIRST_NAME\n",
                        "either legacy respondent name columns or canonical"),
                Arguments.of(
                        "RESP_FORENAME1|RESP_FORENAME2|RESP_SURNAME\n",
                        "Legacy bulk upload files must include"),
                Arguments.of(
                        "RESP_FIRST_NAME|RESP_LAST_NAME\n",
                        "Canonical bulk upload files must include"),
                Arguments.of(
                        "APPLICANT_CODE|APPLICATION_CODE\n",
                        "must include either legacy respondent name columns or canonical"));
    }
}
