package uk.gov.hmcts.appregister.shared.validation;

/**
 * Functional contract for validating a value. Implementations may throw an exception if invalid.
 *
 * @param <T> value type
 */
@FunctionalInterface
public interface Validator<T> {
    /**
     * Validate the provided value.
     *
     * @param validatable value to check
     * @throws RuntimeException if validation fails
     */
    void validate(T validatable); // single operation
}
