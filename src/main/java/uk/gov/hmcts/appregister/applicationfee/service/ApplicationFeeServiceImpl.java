package uk.gov.hmcts.appregister.applicationfee.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;

/** Service to handle application fee operations. */
@Service
@RequiredArgsConstructor
public class ApplicationFeeServiceImpl implements ApplicationFeeService {
    private final FeeRepository feeRepository;

    public FeePair resolveFeePair(String feeReference) {
        List<Fee> fees = feeRepository.findByReference(feeReference);

        Fee mainFee = fees.stream().findFirst().orElse(null);

        // TODO: No offset fee in the DB yet so second fee will always be offset
        Fee offsetFee = fees.size() > 1 ? fees.get(1) : null;
        return new FeePair(mainFee, offsetFee);
    }
}
