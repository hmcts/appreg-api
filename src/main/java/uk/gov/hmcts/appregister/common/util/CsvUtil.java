package uk.gov.hmcts.appregister.common.util;

public class CsvUtil {

    // String split uses a regular expression, so need to escape the pipe character
    private static final String DEFAULT_DELIMITER = "|";

    private CsvUtil() {
        // Private constructor to prevent instantiation
    }

    // Safely escapes characters that Excel/Sheets might interpret as a formula
    public static String escapeCharacters(String value) {

        // We trim the value to ensure there isn't a leading space that could affect the startsWith
        // check

        if(value == null) {
            return null;
        }

        var trimmedValue = value.trim();

        if(trimmedValue.startsWith("\"")) {
            // remove beginning quote and then check for formula symbols
            var unquotedValue = trimmedValue.substring(1);
            if(unquotedValue.startsWith("=") ||
                unquotedValue.startsWith("+") ||
                unquotedValue.startsWith("-") ||
                unquotedValue.startsWith("@")) {

                return value.charAt(0) + "'" + unquotedValue;
            }
        }

        // Check if the cell starts with a formula trigger
        if (trimmedValue.startsWith("=")
                || trimmedValue.startsWith("+")
                || trimmedValue.startsWith("-")
                || trimmedValue.startsWith("@")) {

            // Prepend a single quote to force the spreadsheet to treat it as raw text
            return "'" + value;
        }
        return value;
    }
}
