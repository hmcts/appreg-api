package uk.gov.hmcts.appregister.applicationfee.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.applicationfee.audit.FeeOperation;
import uk.gov.hmcts.appregister.applicationfee.service.exception.ApplicationFeeCode;
import uk.gov.hmcts.appregister.applicationfee.service.mapper.FeeMapper;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.common.util.BeanUtil;

/**
 * Service to handle application fee operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ApplicationFeeServiceImpl implements ApplicationFeeService {
    private final FeeRepository feeRepository;
    private final BusinessDateProvider businessDateProvider;
    private final AuditOperationService auditService;
    private final FeeMapper mapper;

    @Override
    public FeePair resolveFeePair(String feeReference) {
        return resolveFeePair(feeReference, businessDateProvider.currentUkDate());
    }

    @Override
    public FeePair resolveFeePair(String feeReference, LocalDate date) {
        var targetDate = date != null ? date : businessDateProvider.currentUkDate();
        return resolveFeePair(feeReference, targetDate, findOffsiteFee(targetDate));
    }

    private FeePair resolveFeePair(String feeReference, LocalDate date, Optional<Fee> offsiteFee) {
        List<Fee> fee = feeRepository.findByReferenceBetweenDate(feeReference, date);
        return resolveFeePair(fee.stream().findFirst(), offsiteFee);
    }

    private FeePair resolveFeePair(Optional<Fee> feesForRef, Optional<Fee> offsiteFee) {
        // if we do not have a main but have an offset then error
        if (feesForRef.isEmpty() && offsiteFee.isPresent()) {
            log.warn(ApplicationFeeCode.NO_MAIN_FEE.getCode().getMessage());
        }

        return new FeePair(feesForRef.orElse(null), offsiteFee.orElse(null));
    }

    @Override
    public Map<String, FeePair> resolveFeePairs(Collection<String> feeReferences, LocalDate date) {
        var targetDate = date != null ? date : businessDateProvider.currentUkDate();
        var offsiteFee = findOffsiteFee(targetDate);
        var feesByReference = new LinkedHashMap<String, FeePair>();
        var firstFeeByNormalisedReference = new LinkedHashMap<String, Fee>();

        if (feeReferences == null || feeReferences.isEmpty()) {
            return feesByReference;
        }

        var distinctReferences =
                feeReferences.stream()
                        .filter(reference -> reference != null && !reference.isBlank())
                        .map(reference -> reference.toLowerCase(Locale.ROOT))
                        .distinct()
                        .toList();

        for (var fee : feeRepository.findByReferenceInBetweenDate(distinctReferences, targetDate)) {
            firstFeeByNormalisedReference.putIfAbsent(
                    fee.getReference().toLowerCase(Locale.ROOT), fee);
        }

        for (var feeReference : feeReferences) {
            var matchedFee =
                    feeReference == null || feeReference.isBlank()
                            ? null
                            : firstFeeByNormalisedReference.get(
                                    feeReference.toLowerCase(Locale.ROOT));
            feesByReference.putIfAbsent(
                    feeReference, new FeePair(matchedFee, offsiteFee.orElse(null)));
        }

        return feesByReference;
    }

    private Optional<Fee> findOffsiteFee(LocalDate date) {
        return feeRepository.findOffsite(date).stream().findFirst();
    }

    @Override
    @Transactional
    public void upsertFee(Fee fee) {
        var feeDB =
                feeRepository
                        .findById(fee.getId())
                        .stream()
                        .findFirst();

        if (feeDB.isEmpty()) {
            auditService.processAudit(
                    FeeOperation.CREATE_FEE,
                    req -> {
                        fee.setChangedBy(1L);
                        fee.setChangedDate(businessDateProvider.currentUkDateTime());
                        feeRepository.saveAndFlush(fee);
                        AuditableResult<Void, Fee> auditableResult =
                                new AuditableResult<>(null, fee);
                        return Optional.of(auditableResult);
                    });

        } else {
            var currentFee = feeDB.get();
            var updatedFee = BeanUtil.copyBean(currentFee);
            mapper.updateFee(updatedFee, fee);

            // this code is to allow the end date to be updated if it has changed, as the mapper
            // will ignore null values
            if (!Objects.equals(updatedFee.getEndDate(), fee.getEndDate())) {
                updatedFee.setEndDate(fee.getEndDate());
            }

            auditService.processAudit(
                    currentFee,
                    FeeOperation.UPDATE_FEE,
                    req -> {
                        updatedFee.setChangedBy(1L);
                        updatedFee.setChangedDate(businessDateProvider.currentUkDateTime());

                        AuditableResult<Void, Fee> auditableResult =
                                new AuditableResult<>(null, updatedFee);
                        return Optional.of(auditableResult);
                    });
            feeRepository.saveAndFlush(updatedFee);
        }
    }
}
