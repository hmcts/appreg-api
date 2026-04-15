package uk.gov.hmcts.appregister.applicationfee.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.applicationfee.service.exception.ApplicationFeeCode;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;

/**
 * Service to handle application fee operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationFeeServiceImpl implements ApplicationFeeService {
    private final FeeRepository feeRepository;
    private final BusinessDateProvider businessDateProvider;

    @Override
    @SuppressWarnings("java:S1135")
    public FeePair resolveFeePair(String feeReference) {
        return resolveFeePair(feeReference, businessDateProvider.currentUkDate());
    }

    @Override
    @SuppressWarnings("java:S1135")
    public FeePair resolveFeePair(String feeReference, LocalDate date) {
        List<Fee> fee = feeRepository.findByReferenceBetweenDate(feeReference, date);
        return resolveFeePair(fee.stream().findFirst(), getOffsiteFee(date));
    }

    @SuppressWarnings("java:S1135")
    private FeePair resolveFeePair(Optional<Fee> feesForRef, Optional<Fee> offsiteFee) {
        // if we do not have a main but have an offset then error
        if (feesForRef.isEmpty() && !offsiteFee.isEmpty()) {
            log.warn(ApplicationFeeCode.NO_MAIN_FEE.getCode().getMessage());
        }

        return new FeePair(feesForRef.orElse(null), offsiteFee.orElse(null));
    }

    private Optional<Fee> getOffsiteFee(LocalDate date) {
        return feeRepository.findOffsite(date).stream().findFirst();
    }
}
