package uk.gov.hmcts.appregister.applicationfee.service;

import java.util.Optional;
import uk.gov.hmcts.appregister.applicationfee.model.ApplicationFee;
import uk.gov.hmcts.appregister.applicationfee.model.FeePair;

public interface ApplicationFeeService {
    Optional<ApplicationFee> findMainFee(String feeReference);

    Optional<ApplicationFee> findOffsetFee(String feeReference);

    FeePair resolveFeePair(String feeReference);
}
