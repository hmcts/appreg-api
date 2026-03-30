package uk.gov.hmcts.appregister.data.filter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Random;

public class PrimitiveDataGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final LocalTime TIME = LocalTime.of(14, 0, 0);

    private static final LocalDate DATE = LocalDate.now();

    public static String generate() {
        return generate(100);
    }

    public static String generate(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }

        return sb.toString();
    }

    public static Boolean getBoolean(int count) {
        return count % 2 == 0;
    }

    public static Long getLong(int count) {
        return Integer.toUnsignedLong(count);
    }

    public static LocalDate getDate(int count) {
        return DATE.plusDays(count);
    }

    public static LocalTime getTime(int count) {
        return TIME.plusHours(count);
    }
}
