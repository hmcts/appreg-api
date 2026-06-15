package uk.gov.hmcts.appregister.applicationfee.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.FeePair;

/**
 * Service for resolving application fees.
 */
public interface ApplicationFeeService {
    FeePair resolveFeePair(String feeReference);

    FeePair resolveFeePair(String feeReference, LocalDate date);

    FeePair resolveFeePair(String feeReference, LocalDate date, Optional<Fee> offsiteFee);

    Optional<Fee> resolveOffsiteFee(LocalDate date);

    Map<String, FeePair> resolveFeePairs(Collection<String> feeReferences, LocalDate date);
}
