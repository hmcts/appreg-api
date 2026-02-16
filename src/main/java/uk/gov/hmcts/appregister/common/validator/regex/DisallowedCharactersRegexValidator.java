package uk.gov.hmcts.appregister.common.validator.regex;

import org.springframework.stereotype.Component;

@Component
public class DisallowedCharactersRegexValidator extends AbstractRegexValidator {
    public static final String DISALLOWED_CHARACTERS = "^[^\\\"$^*\\[\\]/={};#~=<>]*$";

    public DisallowedCharactersRegexValidator() {
        super(DISALLOWED_CHARACTERS);
    }
}
