package uk.gov.hmcts.appregister.applicationcode.validator;

import java.util.Arrays;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationcode.exception.AppCodeError;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode_;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.validator.Validator;

/**
 * An explicit sort to ensure that the sort parameter being passed through is expected. If we do not
 * explicitly check the sort value then Spring returns a 500 as it tries to feed the sort value
 * blindly onto the backend JPA query
 */
@Component
public class ApplicationCodeSortValidator implements Validator<String> {
    public static String[] VALID_SORT_VALUES = {
        ApplicationCode_.APPLICATION_CODE, ApplicationCode_.TITLE
    };

    @Override
    public void validate(String sortValue) {
        if (Arrays.stream(VALID_SORT_VALUES)
                .sorted()
                .filter(val -> val.equals(sortValue))
                .toList()
                .isEmpty()) {
            throw new AppRegistryException(
                    AppCodeError.SORT_NOT_SUITABLE,
                    "Sort value %s is not suitable".formatted(sortValue),
                    null);
        }
    }
}
