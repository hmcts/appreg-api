package uk.gov.hmcts.appregister.applicationcode.validator;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode_;
import uk.gov.hmcts.appregister.common.validator.AbstractSortValidator;

/**
 * An explicit sort to ensure that the sort parameter being passed through is expected. If we do not
 * explicitly check the sort value then Spring returns a 500 as it tries to feed the sort value
 * blindly onto the backend JPA query
 */
@Component
public class ApplicationCodeSortValidator extends AbstractSortValidator {
    @Override
    protected String[] getValidSortProperties() {
        return new String[] {ApplicationCode_.CODE, ApplicationCode_.TITLE};
    }
}
