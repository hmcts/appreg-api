package uk.gov.hmcts.appregister.standardapplicant.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.model.PayloadForGet;
import uk.gov.hmcts.appregister.common.validator.Validator;
import uk.gov.hmcts.appregister.standardapplicant.exception.StandardApplicantCodeError;

import java.util.List;
import java.util.function.BiFunction;

/**
 * A standard applicant id exists validator that validates if a standard applicant exists for a given code and date.
 * The validator throws an exception if no standard applicant is found or if multiple standard applicants are found.
 */
@Component
@RequiredArgsConstructor
public class StandardApplicantExistsValidator implements Validator<PayloadForGet, StandardApplicant> {
    private final StandardApplicantRepository repository;

    @Override
    public void validate(PayloadForGet code) {
        validateId(code);
    }

    /**
     * validate the id
     * @param code The standard applicant id
     * @return The standard applicant
     */
    private StandardApplicant validateId(PayloadForGet code) {
        List<StandardApplicant> results = repository.findStandardApplicantByCodeAndDate(code.getCode(), code.getDate());

        if (results.isEmpty()) {
            throw new AppRegistryException(
                    StandardApplicantCodeError.STANDARD_APPLICANT_NOT_FOUND,
                    "No standard applicant found for code '%s' on date %s"
                            .formatted(code, code.getDate()));
        } else if (results.size() > SINGLE_RECORD) {
            throw new AppRegistryException(
                    StandardApplicantCodeError.DUPLICATE_RESULT_CODE_FOUND,
                    "Multiple standard applicant found for code '%s' on date %s"
                            .formatted(code, code.getDate()));
        }

        return results.getFirst();
    }

    @Override
    public <R> R validate(PayloadForGet saId, BiFunction<PayloadForGet, StandardApplicant, R> createApplicationSupplier) {
        StandardApplicant standardApplicant = validateId(saId);
        if (createApplicationSupplier != null) {
            return createApplicationSupplier.apply(saId, standardApplicant);
        }
        return null;
    }
}
