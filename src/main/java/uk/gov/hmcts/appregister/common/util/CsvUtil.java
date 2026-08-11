package uk.gov.hmcts.appregister.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CsvUtil {
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
            return "'" + trimmedValue;
        }
        return trimmedValue;
    }

    public static String escapeCSV(String csv) {
        List<String> rows = Arrays.asList(csv.split("\n"));
        List<String> escapedCsv = new ArrayList<>();
        escapedCsv.add(rows.getFirst()); // Adding header

        for (int i = 1; i < rows.size(); i++) { // Start from 1 to skip the header row
            String[] columns = rows.get(i).split(",", -1); // -1 to include trailing empty strings
            for (int j = 0; j < columns.length; j++) {
                columns[j] = escapeCharacters(columns[j]);
            }
            escapedCsv.add(String.join(",", columns));
        }
        return String.join("\n", escapedCsv);
    }
}
