package uk.gov.hmcts.appregister.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class AppRegExceptionHandler {

    public record ApiError(String code, String message, String correlationId) {}

    @ExceptionHandler(AppRegistryException.class)
    ResponseEntity<ProblemDetail> handleAppRegisterApiException(AppRegistryException exception) {

        // gets the core exception code that we used to apply the application specific code
        ErrorCodeEnum error = exception.getCode();

        log.error("A app register exception occurred", exception);

        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(
                        error.getCode().getHttpCode(), error.getCode().getMessage());

        // map the type and title if we have a code
        if (error.getCode().getType().isPresent()) {
            problemDetail.setType(error.getCode().getType().get());
        }

        if (error.getCode().getMessage() != null) {
            problemDetail.setTitle(error.getCode().getMessage());
        }

        return new ResponseEntity<>(problemDetail, error.getCode().getHttpCode());
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> handle(NotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiError(ex.code, ex.getMessage(), (String) req.getAttribute("correlationId")));
    }
}
