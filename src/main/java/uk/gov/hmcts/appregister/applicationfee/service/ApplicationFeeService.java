package uk.gov.hmcts.appregister.applicationfee.service;

import uk.gov.hmcts.appregister.common.entity.base.FeePair;

/**
 * Service for resolving application fees.
 */
public interface ApplicationFeeService {
    FeePair resolveFeePair(String feeReference);
}
