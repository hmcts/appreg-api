package uk.gov.hmcts.appregister.service.api;

import uk.gov.hmcts.appregister.dto.internal.FeePair;
import uk.gov.hmcts.appregister.model.ApplicationFee;

import java.util.Optional;

public interface ApplicationFeeService {
    Optional<ApplicationFee> findMainFee(String feeReference);
    Optional<ApplicationFee> findOffsetFee(String feeReference);
    FeePair resolveFeePair(String feeReference);
}
