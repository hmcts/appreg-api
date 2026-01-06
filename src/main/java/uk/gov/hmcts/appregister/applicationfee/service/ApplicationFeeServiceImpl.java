package uk.gov.hmcts.appregister.applicationfee.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.applicationfee.exception.ApplicationFeeCodeError;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.base.FeePair;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;

/**
 * Service to handle application fee operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationFeeServiceImpl implements ApplicationFeeService {
    private static final Comparator<Fee> FEE_ID_COMPARATOR = Comparator.comparing(Fee::getId);
    private final FeeRepository feeRepository;
    private final Clock clock;

    @SuppressWarnings("java:S1135")
    @Transactional(readOnly = true)
    public FeePair resolveFeePair(String feeReference) {
        List<Fee> fee =
                feeRepository.findByReferenceBetweenDate(feeReference, LocalDate.now(clock));
        return resolveFeePair(fee);
    }

    @SuppressWarnings("java:S1135")
    private FeePair resolveFeePair(List<Fee> feesForRef) {
        List<Fee> main = feesForRef.stream().filter(r -> !r.isOffsite()).toList();
        List<Fee> offsite = feesForRef.stream().filter(Fee::isOffsite).toList();

        // if we do not have a main but have an offset then error
        if (main.isEmpty() && !offsite.isEmpty()) {
            log.warn(ApplicationFeeCodeError.NO_MAIN_FEE.getCode().getMessage());
        }

        // if we have more than one main or offset then we have an issue
        if (main.size() > 1 || offsite.size() > 1) {
            log.warn(ApplicationFeeCodeError.AMBIGUOUS_FEE.getCode().getMessage());
        }

        // Is this good enough when multiple mains and offsite exists?
        Optional<Fee> mainFee = main.stream().max(FEE_ID_COMPARATOR);
        Optional<Fee> offsiteFee = offsite.stream().max(FEE_ID_COMPARATOR);

        return new FeePair(mainFee.orElse(null), offsiteFee.orElse(null));
    }
}
