package uk.gov.hmcts.appregister.data.filter.generator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Random;

/**
 * A primitive data generator. This is useful for filter or sorting tests.
 */
public class PrimitiveDataGenerator {

    /** The characters that we can use to generate a string. */
    private static final String CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /** The baseline time. */
    private static final LocalTime TIME = LocalTime.of(14, 0, 0);

    /** The baseline date. */
    private static final LocalDate DATE = LocalDate.now();

    /** generate the string with a maximum of 100. */
    public static String generate() {
        return generate(-1, 100);
    }

    /**
     * generate the string with a maximum.
     *
     * @param length The maximum length of the string.
     */
    public static String generate(int count, int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);

        int numberOfSpacesNeededExcludingCount = length;

        if (count != -1) {
            numberOfSpacesNeededExcludingCount = (length - Integer.toString(count).length());
        }

        for (int i = 0; i < numberOfSpacesNeededExcludingCount; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }

        if (count != -1) {
            sb = sb.append(count);
        }

        return sb.toString();
    }

    /**
     * gets the boolean.
     *
     * @param count The count to use.
     * @return The modulated boolean
     */
    public static Boolean getBoolean(int count) {
        return count % 2 == 0;
    }

    /**
     * gets the long.
     *
     * @param count The count to use.
     * @return The long of the count.
     */
    public static Long getLong(int count) {
        return Integer.toUnsignedLong(count);
    }

    /**
     * gets a day with a day increment.
     *
     * @param count The count to use.
     */
    public static LocalDate getDate(int count) {
        return DATE.plusDays(count);
    }

    /**
     * gets a time.
     *
     * @param count The count to use
     * @return The local time with an hour increment
     */
    public static LocalTime getTime(int count) {
        return TIME.plusHours(count);
    }
}
