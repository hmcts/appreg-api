package uk.gov.hmcts.appregister.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

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
        var trimmedValue = value.trim();

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

    public static String escapeCSV(String csv) {
        List<String> rows = Arrays.asList(csv.split("\n"));
        List<String> escapedCsv = new ArrayList<>();
        escapedCsv.add(rows.getFirst()); // Adding header

        // Start from 1 to skip the header row
        for (int i = 1; i < rows.size(); i++) {
            // -1 to include trailing empty strings, using pattern.quote to escape the delimiter for
            // regex
            String[] columns = rows.get(i).split(Pattern.quote(DEFAULT_DELIMITER), -1);
            for (int j = 0; j < columns.length; j++) {
                columns[j] = escapeCharacters(columns[j]);
            }
            escapedCsv.add(String.join(DEFAULT_DELIMITER, columns));
        }
        return String.join("\n", escapedCsv);
    }
}
