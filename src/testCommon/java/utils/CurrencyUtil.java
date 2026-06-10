package utils;

import java.math.BigDecimal;

public class CurrencyUtil {
    private CurrencyUtil() {
        /* This utility class should not be instantiated */
    }

    /**
     * Converts a BigDecimal to pounds and pence by moving the decimal point to the right by 2
     * places. Ignores any fractional part.
     *
     * @param bigDecimal the BigDecimal to convert
     * @return the converted value
     */
    public static final Long getPoundsToPennies(BigDecimal bigDecimal) {
        BigDecimal pence = bigDecimal.movePointRight(2);
        return pence.longValue();
    }
}
