package uk.gov.hmcts.appregister.shared.validation;

@FunctionalInterface
public interface Validator<T> {
    void validate(T validatable);
}
