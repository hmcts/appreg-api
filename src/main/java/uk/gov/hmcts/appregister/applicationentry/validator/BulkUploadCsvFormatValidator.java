package uk.gov.hmcts.appregister.applicationentry.validator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

/**
 * Validates the respondent name column shape before OpenCSV binds rows.
 */
@Component
public class BulkUploadCsvFormatValidator {
    private static final String DELIMITER = "\\|";
    private static final Set<String> LEGACY_NAME_COLUMNS =
            Set.of("RESP_FORENAME1", "RESP_FORENAME2", "RESP_FORENAME3", "RESP_SURNAME");
    private static final Set<String> CANONICAL_NAME_COLUMNS =
            Set.of("RESP_FIRST_NAME", "RESP_MIDDLE_NAME", "RESP_LAST_NAME");

    public void validate(MultipartFile file) {
        Set<String> headers = readHeaders(file);

        boolean hasLegacyColumns = headers.stream().anyMatch(LEGACY_NAME_COLUMNS::contains);
        boolean hasCanonicalColumns = headers.stream().anyMatch(CANONICAL_NAME_COLUMNS::contains);

        if (hasLegacyColumns && hasCanonicalColumns) {
            throw invalidFormat(
                    "Bulk upload files must use either legacy respondent name columns or canonical"
                            + " respondent name columns, not both");
        }

        if (hasLegacyColumns && !headers.containsAll(LEGACY_NAME_COLUMNS)) {
            throw invalidFormat(
                    "Legacy bulk upload files must include RESP_FORENAME1, RESP_FORENAME2,"
                            + " RESP_FORENAME3 and RESP_SURNAME");
        }

        if (hasCanonicalColumns && !headers.containsAll(CANONICAL_NAME_COLUMNS)) {
            throw invalidFormat(
                    "Canonical bulk upload files must include RESP_FIRST_NAME, RESP_MIDDLE_NAME"
                            + " and RESP_LAST_NAME");
        }

        if (!hasLegacyColumns && !hasCanonicalColumns) {
            throw invalidFormat(
                    "Bulk upload files must include either legacy respondent name columns or"
                            + " canonical respondent name columns");
        }
    }

    private static Set<String> readHeaders(MultipartFile file) {
        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw invalidFormat("Uploaded file must contain a header row");
            }

            return Arrays.stream(headerLine.split(DELIMITER, -1))
                    .map(header -> header.trim().toUpperCase(Locale.ROOT))
                    .filter(header -> !header.isBlank())
                    .collect(Collectors.toSet());
        } catch (IOException exception) {
            throw invalidFormat("Uploaded file could not be read");
        }
    }

    private static AppRegistryException invalidFormat(String message) {
        return new AppRegistryException(AppListEntryError.BULK_UPLOAD_INVALID_FILE_FORMAT, message);
    }
}
