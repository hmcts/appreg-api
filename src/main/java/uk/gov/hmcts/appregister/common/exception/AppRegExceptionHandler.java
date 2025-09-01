package uk.gov.hmcts.appregister.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;

@ControllerAdvice
@EnableAutoConfiguration(exclude = ErrorMvcAutoConfiguration.class)
@Slf4j
public class AppRegExceptionHandler {

    @ExceptionHandler
    @SuppressWarnings({ "java:S2259" })
    ResponseEntity<ProblemDetail> handleDartsApiException(AppRegistryException exception) {
        ErrorCodeEnum error = exception.getCode();

        log.error("A darts exception occurred", exception);

        ProblemDetail problemDetail = ProblemDetail
            .forStatusAndDetail(error.getCode().getHttpCode(), error.getCode().getMessage());

        if (error.getCode().getType().isPresent()) {
            problemDetail.setType(error.getCode().getType().get());
        }

        if (error.getCode().getMessage() != null) {
            problemDetail.setTitle(error.getCode().getMessage());
        }

        return new ResponseEntity<>(problemDetail, error.getCode().getHttpCode());
    }


    @ExceptionHandler
    ResponseEntity<ProblemDetail> exception(Exception exception) {
        log.error("A  exception occurred", exception);

        ProblemDetail problemDetail = ProblemDetail
            .forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "A runtime exception occurred");

        return new ResponseEntity<>(problemDetail, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
