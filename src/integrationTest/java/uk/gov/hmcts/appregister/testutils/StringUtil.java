package uk.gov.hmcts.appregister.testutils;

import lombok.experimental.UtilityClass;

/** Utility class for string operations. */
@UtilityClass
public class StringUtil {
    /**
     * Strips a string down to a maximum length.
     *
     * @param input The original string.
     * @param maxLength The maximum allowed length.
     * @return The truncated string if longer than maxLength, otherwise the original string.
     */
    public static String stripToMax(String input, int maxLength) {
        if (input == null || maxLength < 0) {
            return input; // invalid cases
        }
        if (input.length() <= maxLength) {
            return input; // already within the limit
        }
        return input.substring(0, maxLength);
    }
}
