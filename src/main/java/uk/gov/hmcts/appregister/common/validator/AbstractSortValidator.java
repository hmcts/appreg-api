package uk.gov.hmcts.appregister.common.validator;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

/**
 * A common validator to validate sort criteria.
 */
public abstract class AbstractSortValidator implements Validator<String, Void> {

    /** Set of allowed property names for sorting. */
    /** Set of allowed property names for sorting. */
    private final Set<String> allowed;

    /**
     * Constructs a new validator with the given allowed property names.
     *
     * @param props property names that can be used in sort clauses
     * @throws AppRegistryException if no properties are provided
     */
    protected AbstractSortValidator(String... props) {
        this.allowed =
                Arrays.stream(props)
                        .map(s -> s == null ? "" : s.trim())
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toUnmodifiableSet());
        if (allowed.isEmpty()) {
            throw new AppRegistryException(
                    CommonAppError.SORT_NOT_SUITABLE, "No allowed sort properties configured");
        }
    }

    /**
     * validates the sort value against the valid sort properties. An exception is expected to be
     * thrown on an error occurring
     *
     * @param sortValue The sort value for valuation
     * @throws AppRegistryException if the property is not allowed
     */
    @Override
    public void validate(String sortValue) {
        String p = sortValue == null ? "" : sortValue.trim();
        if (!allowed.contains(p)) {
            throw new AppRegistryException(
                    CommonAppError.SORT_NOT_SUITABLE,
                    "Sort property '%s' is not allowed. Allowed: %s"
                            .formatted(p, String.join(", ", allowed)));
        }
    }
}
