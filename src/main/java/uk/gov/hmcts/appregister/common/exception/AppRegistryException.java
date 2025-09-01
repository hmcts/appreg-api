package uk.gov.hmcts.appregister.common.exception;

import lombok.Getter;

@Getter
public class AppRegistryException extends RuntimeException{

    private ErrorCodeEnum code;

    public AppRegistryException(ErrorCodeEnum code, String detail, Throwable cause) {
        super(detail, cause);
        this.code = code;
    }
}
