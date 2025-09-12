package uk.gov.hmcts.appregister.testutils;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/** Utility class for date operations. */
public class DateUtil {
    public static boolean equalsIgnoreMillis(OffsetDateTime i1, OffsetDateTime i2) {
        if (i1 == null || i2 == null) {
            return i1 == i2;
        }
        return i1.truncatedTo(ChronoUnit.SECONDS).equals(i2.truncatedTo(ChronoUnit.SECONDS));
    }
}
