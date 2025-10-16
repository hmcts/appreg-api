package uk.gov.hmcts.appregister.resultcode.validator;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.validator.AbstractSortValidator;
import uk.gov.hmcts.appregister.resultcode.api.ResultCodeApiFields;

/**
 * Sort validator for Result Code queries.
 *
 * <p>Restricts sorting to a predefined set of allowed properties to prevent invalid or unsafe
 * database access through arbitrary sort fields.
 */
@Component
public class ResultCodeSortValidator extends AbstractSortValidator {

    /**
     * Creates a validator with allowed sort properties for Result Codes.
     *
     * <p>Currently limited to "name" and "code".
     */
    public ResultCodeSortValidator() {
        super(ResultCodeApiFields.CODE, ResultCodeApiFields.TITLE);
    }
}
