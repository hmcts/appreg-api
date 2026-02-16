package uk.gov.hmcts.appregister.common.validator.regex;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.validator.Validator;

@Slf4j
public class AbstractRegexValidator implements Validator<String, String> {

    private final String pattern;

    public AbstractRegexValidator(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public void validate(String value) {
        if (value != null && !value.matches(pattern)) {
            log.warn("Regex validation mismatch. Pattern: {}, Value: {}", pattern, value);
            throw new AppRegistryException(
                    CommonAppError.REGEX_VALIDATION_MISMATCH_ERROR,
                    "Regex Validation Mismatch: Pattern: %s, Value: %s".formatted(pattern, value));
        }
    }
}
