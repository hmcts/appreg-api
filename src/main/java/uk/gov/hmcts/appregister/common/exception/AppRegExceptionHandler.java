package uk.gov.hmcts.appregister.common.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class AppRegExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AppRegistryException.class)
    ResponseEntity<ProblemDetail> handleAppRegisterApiException(AppRegistryException exception) {

        // gets the core exception code that we used to apply the application specific code
        ErrorCodeEnum error = exception.getCode();

        log.error("A app register exception occurred", exception);

        ProblemDetail problemDetail = getDetailFromEnum(error);

        return new ResponseEntity<>(problemDetail, error.getCode().getHttpCode());
    }

    /**
     * creates a problem details for a given error enum and exception.
     *
     * @param error The error
     * @return The problem detail
     */
    private ProblemDetail getDetailFromEnum(ErrorCodeEnum error) {
        return getDetailFromEnum(error, null);
    }

    /**
     * creates a problem details for a given error enum and exception.
     *
     * @param error The error
     * @param e The exception. This can be null
     * @return The problem detail
     */
    private ProblemDetail getDetailFromEnum(ErrorCodeEnum error, Exception e) {
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(
                        error.getCode().getHttpCode(), error.getCode().getMessage());

        // take the message from the exception. We need to be careful about exposing internal
        // details here
        // so it needs to be performed selectively based on exception type
        if (e != null) {
            problemDetail.setDetail(e.getMessage());
        }

        // map the type and title if we have a code
        if (error.getCode().getType().isPresent()) {
            problemDetail.setType(error.getCode().getType().get());
        }

        if (error.getCode().getMessage() != null) {
            problemDetail.setTitle(error.getCode().getMessage());
        }
        return problemDetail;
    }

    @ExceptionHandler
    @SuppressWarnings({"java:S2259", "java:S1185"})
    protected ResponseEntity<Object> handleConstraintViolationException(
            ConstraintViolationException ex) {
        log.error("An exception occurred", ex);
        ProblemDetail problemDetail = getDetailFromEnum(CommonAppError.CONSTRAINT_ERROR, ex);
        return new ResponseEntity<>(problemDetail, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> mismatchType(MethodArgumentTypeMismatchException ex) {
        log.error("An exception occurred", ex);
        ProblemDetail problemDetail = getDetailFromEnum(CommonAppError.TYPE_MISMATCH_ERROR, ex);
        return new ResponseEntity<>(problemDetail, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        log.error("An exception occurred", ex);
        ProblemDetail problemDetail =
                getDetailFromEnum(CommonAppError.METHOD_ARGUMENT_INVALID_ERROR, ex);

        // add the failure specifics to the problem detail properties
        for (FieldError fieldError : ex.getFieldErrors()) {
            if (problemDetail.getProperties() == null) {
                problemDetail.setProperties(new HashMap<>());
            }
            problemDetail
                    .getProperties()
                    .put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        MultiValueMap<String, String> errors = new LinkedMultiValueMap<>();

        return new ResponseEntity<>(problemDetail, HttpStatus.BAD_REQUEST);
    }

    @Override
    @SuppressWarnings("java:S2259")
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        log.error("An exception occurred", ex);
        ProblemDetail problemDetail =
                getDetailFromEnum(CommonAppError.METHOD_VALIDATION_INVALID_ERROR, ex);
        return new ResponseEntity<>(problemDetail, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        log.error("An exception occurred", ex);
        ProblemDetail problemDetail = getDetailFromEnum(CommonAppError.NOT_READABLE_ERROR, ex);
        return new ResponseEntity<>(problemDetail, HttpStatus.BAD_REQUEST);
    }
}
