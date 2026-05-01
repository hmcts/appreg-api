package uk.gov.hmcts.appregister.applicationfee.helper;

import java.time.LocalDate;
import uk.gov.hmcts.appregister.common.entity.FeePair;

/**
 * Service for resolving application fees.
 */
public interface ApplicationFeeHelper {
    FeePair resolveFeePair(String feeReference);

    FeePair resolveFeePair(String feeReference, LocalDate date);
}
