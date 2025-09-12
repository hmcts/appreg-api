package uk.gov.hmcts.appregister.common.exception;

import java.net.URI;
import java.util.Optional;
import org.springframework.http.HttpStatus;

/**
 * Describes an application error code. Typically we will use {@link
 * uk.gov.hmcts.appregister.common.exception.DefaultErrorCode}
 */
public interface ErrorCode {

    /**
     * The http code.
     *
     * @return The http code of the error
     */
    HttpStatus getHttpCode();

    /**
     * The error message.
     *
     * @return The message
     */
    String getMessage();

    /**
     * The application specific error code.
     *
     * @return The application specific error code
     */
    String getAppCode();

    /**
     * gets a URL representation of the application specific error code if it exists.
     *
     * @return The URI of the application specific error code
     */
    default Optional<URI> getType() {
        if (getAppCode() != null) {
            return Optional.of(URI.create(getAppCode()));
        }
        return Optional.empty();
    }
}
