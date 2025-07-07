package uk.gov.hmcts.appregister.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.dto.internal.FeePair;
import uk.gov.hmcts.appregister.model.ApplicationFee;
import uk.gov.hmcts.appregister.repository.ApplicationFeeRepository;
import uk.gov.hmcts.appregister.service.api.ApplicationFeeService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApplicationFeeServiceImpl implements ApplicationFeeService {
    private final ApplicationFeeRepository feeRepository;

    public Optional<ApplicationFee> findMainFee(String feeReference) {
        return feeRepository.findByReferenceAndIsOffset(feeReference, false)
            .stream()
            .findFirst();
    }

    public Optional<ApplicationFee> findOffsetFee(String feeReference) {
        return feeRepository.findByReferenceAndIsOffset(feeReference, true)
            .stream()
            .findFirst();
    }

    public FeePair resolveFeePair(String feeReference) {
        List<ApplicationFee> fees = feeRepository.findByReference(feeReference);

        ApplicationFee mainFee = fees.stream()
            .filter(f -> !Boolean.TRUE.equals(f.isOffset()))
            .findFirst()
            .orElse(null);

        ApplicationFee offsetFee = fees.stream()
            .filter(f -> Boolean.TRUE.equals(f.isOffset()))
            .findFirst()
            .orElse(null);

        return new FeePair(mainFee, offsetFee);
    }
}
