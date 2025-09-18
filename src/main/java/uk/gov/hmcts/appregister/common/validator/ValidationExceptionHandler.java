package uk.gov.hmcts.appregister.common.validator;

import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Utility class to handle validation exceptions and convert them to appropriate HTTP responses. */
public class ValidationExceptionHandler {
    /**
     * Runs the given action and wraps any IllegalArgumentException into a ResponseStatusException
     * (400 Bad Request).
     *
     * @param action a lambda that may throw IllegalArgumentException
     * @param <T> return type
     * @return the result of the action
     */
    public static <T> T wrap(Supplier<T> action) {
        try {
            return action.get();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
