package uk.gov.hmcts.appregister.applicationcode.validator;

import java.util.List;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationcode.exception.ApplicationCodeError;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.model.PayloadForGet;
import uk.gov.hmcts.appregister.common.validator.Validator;

/**
 * validates for a get application code operation.
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class GetApplicationCodeValidator
        implements Validator<PayloadForGet, GetApplicationCodeValidationSuccess> {
    private final ApplicationCodeRepository applicationCodeRepository;

    @Override
    public void validate(PayloadForGet validatable) {
        validate(validatable, (v, r) -> null);
    }

    @Override
    public <R> R validate(
            PayloadForGet validatable,
            BiFunction<PayloadForGet, GetApplicationCodeValidationSuccess, R> validateSuccess) {

        final List<ApplicationCode> applicationCodeResults =
                applicationCodeRepository.findByCodeAndDate(
                        validatable.getCode(), validatable.getDate());

        if (applicationCodeResults.isEmpty()) {
            throw new AppRegistryException(
                    ApplicationCodeError.CODE_NOT_FOUND,
                    "No code found for code %s and date %s"
                            .formatted(validatable.getCode(), validatable.getDate()));
        } else if (applicationCodeResults.size() > 1) {
            log.warn(
                    "Too many records found for code: {} and date: {}",
                    validatable.getCode(),
                    validatable.getDate());

            throw new AppRegistryException(
                    ApplicationCodeError.DUPLICATE_CODE_FOUND,
                    "Duplicate code found for code %s and date %s"
                            .formatted(validatable.getCode(), validatable.getDate()));
        }

        GetApplicationCodeValidationSuccess success =
                GetApplicationCodeValidationSuccess.builder()
                        .applicationCode(applicationCodeResults.getFirst())
                        .build();
        return validateSuccess.apply(validatable, success);
    }
}
