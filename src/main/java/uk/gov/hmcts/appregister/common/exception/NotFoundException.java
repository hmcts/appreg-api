package uk.gov.hmcts.appregister.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {

    public final String code;

    public NotFoundException(String code, String message) { super(message); this.code = code; }

}
