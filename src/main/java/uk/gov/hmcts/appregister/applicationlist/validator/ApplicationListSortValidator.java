package uk.gov.hmcts.appregister.applicationlist.validator;

import org.springframework.stereotype.Component;

import uk.gov.hmcts.appregister.common.entity.ApplicationList_;
import uk.gov.hmcts.appregister.common.validator.AbstractSortValidator;

@Component
public class ApplicationListSortValidator extends AbstractSortValidator {

    public ApplicationListSortValidator() {
        super(
            ApplicationList_.DATE,
            ApplicationList_.TIME,
            ApplicationList_.STATUS,
            ApplicationList_.COURT_CODE,
            ApplicationList_.CJA,
            ApplicationList_.DESCRIPTION,
            ApplicationList_.OTHER_LOCATION
        );
    }
}
