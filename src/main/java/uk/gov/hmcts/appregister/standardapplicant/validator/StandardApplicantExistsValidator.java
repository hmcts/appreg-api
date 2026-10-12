package uk.gov.hmcts.appregister.standardapplicant.validator;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.util.ReferenceDataSelectionUtil;
import uk.gov.hmcts.appregister.common.validator.Validator;
import uk.gov.hmcts.appregister.standardapplicant.exception.StandardApplicantCodeError;

/**
 * A standard applicant code exists validator that validates if a standard applicant exists for a
 * given code. The validator throws an exception if no standard applicant is found and otherwise
 * selects the first deterministically ordered row.
 */
@Component
@RequiredArgsConstructor
public class StandardApplicantExistsValidator implements Validator<String, StandardApplicant> {
    private final StandardApplicantRepository repository;
    private final Clock clock;
    private final ZoneId ukZone;

    @Override
    public void validate(String code) {
        validateId(code);
    }

    @Override
    public <R> R validate(
            String code, BiFunction<String, StandardApplicant, R> createApplicationSupplier) {
        StandardApplicant standardApplicant = validateId(code);
        if (createApplicationSupplier != null) {
            return createApplicationSupplier.apply(code, standardApplicant);
        }
        return null;
    }

    /**
     * validate the id.
     *
     * @param code The standard applicant id
     * @return The standard applicant
     */
    private StandardApplicant validateId(String code) {
        LocalDate todayUk = LocalDate.now(clock.withZone(ukZone));
        List<StandardApplicant> results = repository.findStandardApplicantByCode(code, todayUk);

        if (results.isEmpty()) {
            throw new AppRegistryException(
                    StandardApplicantCodeError.STANDARD_APPLICANT_NOT_FOUND,
                    "No standard applicant found for code '%s'".formatted(code));
        }

        return ReferenceDataSelectionUtil.selectFirstOrderedRecord(
                results, "standard applicant", code);
    }
}
