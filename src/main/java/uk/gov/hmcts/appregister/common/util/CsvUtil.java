package uk.gov.hmcts.appregister.common.util;

public class CsvUtil {
    private CsvUtil() {
        // Private constructor to prevent instantiation
    }

    // Prevent spreadsheet applications from interpreting CSV values as formulas.
    public static String escapeCharacters(String value) {
        if (value == null) {
            return null;
        }

        var trimmedValue = value.trim();

        if (trimmedValue.startsWith("=")
                || trimmedValue.startsWith("+")
                || trimmedValue.startsWith("-")
                || trimmedValue.startsWith("@")) {
            return "'" + value;
        }

        return value;
    }
}
