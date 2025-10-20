package uk.gov.hmcts.appregister.applicationlist.validator;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.ApplicationList_;
import uk.gov.hmcts.appregister.common.validator.AbstractSortValidator;

/**
 * Validator for allowed ApplicationList entity properties used in sorting.
 *
 * <p>Ensures that only approved fields can appear in ORDER BY clauses to prevent invalid or unsafe
 * sorting requests.
 */
@Component
public class ApplicationListSortValidator extends AbstractSortValidator {

    public ApplicationListSortValidator() {
        super(
                ApplicationList_.DATE,
                ApplicationList_.TIME,
                ApplicationList_.STATUS,
                ApplicationList_.DESCRIPTION);
    }
}
