package uk.gov.hmcts.appregister.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import lombok.experimental.UtilityClass;

/**
 * Utility class for date operations.
 */
@UtilityClass
public class DateUtil {
    public static boolean equalsIgnoreMillis(LocalDate i1, LocalDate i2) {
        if (i1 == null || i2 == null) {
            return i1 == i2;
        }
        return i1.equals(i2);
    }

    public static boolean equalsIgnoreMillis(LocalTime i1, LocalTime i2) {
        if (i1 == null || i2 == null) {
            return i1 == i2;
        }
        return i1.truncatedTo(ChronoUnit.SECONDS).equals(i2.truncatedTo(ChronoUnit.SECONDS));
    }

    public static boolean equalsIgnoreMillis(OffsetDateTime i1, OffsetDateTime i2) {
        if (i1 == null || i2 == null) {
            return i1 == i2;
        }
        return i1.truncatedTo(ChronoUnit.SECONDS).equals(i2.truncatedTo(ChronoUnit.SECONDS));
    }
}
