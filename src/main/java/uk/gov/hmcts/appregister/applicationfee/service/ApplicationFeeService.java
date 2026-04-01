package uk.gov.hmcts.appregister.applicationfee.service;

import java.time.LocalDate;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.FeePair;

/**
 * Service for resolving application fees.
 */
public interface ApplicationFeeService {
    FeePair resolveFeePair(String feeReference);

    Fee getOffsiteFee(LocalDate date);
}
