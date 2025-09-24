package uk.gov.hmcts.appregister.common.validator;

import java.util.Arrays;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

/** A common validator to validate sort criteria. */
public abstract class AbstractSortValidator implements Validator<String> {

    /**
     * validates the sort value against the valid sort properties. An exception is expected to be
     * thrown on an error occurring
     *
     * @param sortValue The sort value for valuation
     */
    public void validate(String sortValue) {
        if (Arrays.stream(getValidSortProperties())
                .sorted()
                .filter(val -> val.equals(sortValue))
                .toList()
                .isEmpty()) {
            throw new AppRegistryException(
                    CommonAppError.SORT_NOT_SUITABLE,
                    "Sort value %s is not suitable".formatted(sortValue),
                    null);
        }
    }

    /**
     * gets the valid sort properties, based on the columns of the JPA table columns. We should use
     * the entity constants where possible. For an example use see {@link
     * uk.gov.hmcts.appregister.common.entity.ApplicationCode_}
     *
     * @return The applicable sort properties
     */
    protected abstract String[] getValidSortProperties();
}
