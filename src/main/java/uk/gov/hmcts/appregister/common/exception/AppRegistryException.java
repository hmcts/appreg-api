package uk.gov.hmcts.appregister.common.exception;

import lombok.Getter;

/** The exception that is thrown when an error occurs in the App Registry service. */
@Getter
public class AppRegistryException extends RuntimeException {

    private final transient ErrorCodeEnum code;

    /**
     * Construct exception.
     *
     * @param code The core code entry that we will use to respond to the user.
     * @param detail The detailed message. Used for logging only
     * @param cause The originating error message for logging purposes
     */
    public AppRegistryException(ErrorCodeEnum code, String detail, Throwable cause) {
        super(detail, cause);
        this.code = code;
    }

    /**
     * Construct exception.
     *
     * @param code The core code entry that we will use to respond to the user.
     * @param detail The detailed message. Used for logging only
     */
    public AppRegistryException(ErrorCodeEnum code, String detail) {
        super(detail, null);
        this.code = code;
    }
}
