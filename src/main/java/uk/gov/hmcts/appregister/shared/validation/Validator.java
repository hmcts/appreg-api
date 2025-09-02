package uk.gov.hmcts.appregister.shared.validation;

@FunctionalInterface
public interface Validator<T> {
    void validate(T validatable);

    default Validator<T> andThen(Validator<? super T> next) {
        return v -> {
            this.validate(v);
            next.validate(v);
        };
    }
}
