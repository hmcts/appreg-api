package uk.gov.hmcts.appregister.common.validator.regex;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.validator.Validator;

@Slf4j
public class AbstractRegexValidator implements Validator<Map<String, String>, String> {

    private final String pattern;

    public AbstractRegexValidator(String pattern) {
        this.pattern = pattern;
    }

    /**
     * Validates the value against the regex pattern. If the value does not match the pattern, an
     * AppRegistryException is thrown.
     *
     * @param validatable The map containing the value to be validated. The value is expected to be
     *     under the key "value" and the field name under the key "field".
     * @throws AppRegistryException if the value does not match the regex pattern.
     */
    @Override
    public void validate(Map<String, String> validatable) {
        String value = validatable.get("value");
        if (value != null && !value.matches(pattern)) {
            String field = validatable.get("field");
            log.warn(
                    "Regex validation mismatch. Pattern: {}, Value: {}, Field {}",
                    pattern,
                    value,
                    field);
            Map<String, String> details = Map.of("field", field, "value", value);
            throw new AppRegistryException(
                    CommonAppError.REGEX_VALIDATION_MISMATCH_ERROR,
                    "Regex Validation Mismatch",
                    details);
        }
    }
}
