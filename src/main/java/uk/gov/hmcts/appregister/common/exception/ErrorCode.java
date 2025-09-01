package uk.gov.hmcts.appregister.common.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.Optional;

public interface ErrorCode {
    HttpStatus getHttpCode();
    String getMessage();
    String getAppCode();

    default Optional<URI> getType() {
        if (getAppCode() != null) {
            return Optional.of(URI.create(
                getAppCode())
            );
        }
        return Optional.empty();
    }
}

