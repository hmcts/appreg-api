package uk.gov.hmcts.appregister.applicationfee.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.applicationfee.service.exception.ApplicationFeeCode;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.common.util.ReferenceDataSelectionUtil;

/**
 * Service to handle application fee operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationFeeServiceImpl implements ApplicationFeeService {
    private final FeeRepository feeRepository;
    private final BusinessDateProvider businessDateProvider;

    @SuppressWarnings("java:S1135")
    public FeePair resolveFeePair(String feeReference) {
        LocalDate todayUk = businessDateProvider.currentUkDate();
        return resolveFeePair(feeReference, todayUk);
    }

    @SuppressWarnings("java:S1135")
    public FeePair resolveFeePair(String feeReference, LocalDate asOfDate) {
        List<Fee> feesForRef = feeRepository.findByReferenceBetweenDate(feeReference, asOfDate);
        List<Fee> main = feesForRef.stream().filter(r -> !r.isOffsite()).toList();
        List<Fee> offsite = feesForRef.stream().filter(Fee::isOffsite).toList();

        // if we do not have a main but have an offset then error
        if (main.isEmpty() && !offsite.isEmpty()) {
            log.warn(ApplicationFeeCode.NO_MAIN_FEE.getCode().getMessage());
        }

        // if we have more than one main or offset then we have an issue
        if (main.size() > 1 || offsite.size() > 1) {
            log.warn(ApplicationFeeCode.AMBIGUOUS_FEE.getCode().getMessage());
        }

        Fee mainFee =
                main.isEmpty()
                        ? null
                        : ReferenceDataSelectionUtil.selectFirstOrderedActiveRecord(
                                main,
                                "fee",
                                feeReference + " (offsite=false)",
                                asOfDate,
                                Fee::getEndDate);

        Fee offsiteFee =
                offsite.isEmpty()
                        ? null
                        : ReferenceDataSelectionUtil.selectFirstOrderedActiveRecord(
                                offsite,
                                "fee",
                                feeReference + " (offsite=true)",
                                asOfDate,
                                Fee::getEndDate);

        return new FeePair(mainFee, offsiteFee);
    }
}
