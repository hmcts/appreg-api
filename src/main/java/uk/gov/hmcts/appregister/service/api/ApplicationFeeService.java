package uk.gov.hmcts.appregister.service.api;

import java.util.Optional;
import uk.gov.hmcts.appregister.dto.internal.FeePair;
import uk.gov.hmcts.appregister.model.ApplicationFee;

public interface ApplicationFeeService {
    Optional<ApplicationFee> findMainFee(String feeReference);

    Optional<ApplicationFee> findOffsetFee(String feeReference);

    FeePair resolveFeePair(String feeReference);
}
