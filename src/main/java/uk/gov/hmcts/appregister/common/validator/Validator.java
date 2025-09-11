package uk.gov.hmcts.appregister.common.validator;

/** A generic validation interface. */
@FunctionalInterface
public interface Validator<T> {

    /**
     * Validate the provided {@code validatable}.
     *
     * <p>The general convention is to throw a {@code DartsApiException} if validation fails, but
     * this is not enforced by this interface.
     *
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException if validation fails
     */
    void validate(T validatable);
}
