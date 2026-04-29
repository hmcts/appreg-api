package uk.gov.hmcts.appregister.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import uk.gov.hmcts.appregister.common.entity.Fee;

public class CurrencyUtil {
    /**
     * get pennies for the fee.
     *
     * @param fee The fee to get the pennies for.
     * @return The pennies.
     */
    public static Long getPennies(Fee fee) {
        BigDecimal pounds = fee.getAmount();

        BigDecimal scaled = pounds.setScale(2, RoundingMode.UNNECESSARY);

        return scaled.movePointRight(2).longValueExact();
    }
}
