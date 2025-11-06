package uk.gov.hmcts.appregister.common.validator;

import java.util.function.BiFunction;

/**
 * A generic validation interface that is used across the app registration service.
 */
public interface Validator<T, O> {
    static final int SINGLE_RECORD = 1;


    /**
     * Validate the provided {@code validatable}.
     *
     * <p>The general convention is to throw a {@link
     * uk.gov.hmcts.appregister.common.exception.AppRegistryException} if validation fails, but this
     * is not enforced by this interface.
     *
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException if validation fails
     */
    void validate(T validatable);

    /**
     * Validate the provided {@code validatable}.
     *
     * @param validatable The object to validate
     * @param validateSuccess Function to be executed if validation is successful returning an
     *     associated response. The function is passed the original DTO as well as the success data
     *     from the implementing validator. The function should return a response of type R.
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException if validation fails
     */
    default <R> R validate(T validatable, BiFunction<T, O, R> validateSuccess) {
        validate(validatable);
        return validateSuccess.apply(validatable, null);
    }
}
