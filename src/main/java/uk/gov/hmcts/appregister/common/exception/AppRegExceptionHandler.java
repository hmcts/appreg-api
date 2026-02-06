package uk.gov.hmcts.appregister.common.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
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

        ProblemDetail problemDetail = getDetailFromEnum(exception.getCode(), exception);

        return new ResponseEntity<>(problemDetail, error.getCode().getHttpCode());
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

        // if the exception has properties, add them to the problem detail as they should be exposed
        if (e instanceof AppRegistryException appRegistryException
                && appRegistryException.getDetails() != null
                && !appRegistryException.getDetails().isEmpty()) {

            problemDetail.setDetail("");

            for (String key : appRegistryException.getDetails().keySet()) {
                // add to the map
                problemDetail.setDetail(
                        problemDetail.getDetail()
                                + key
                                + "="
                                + appRegistryException.getDetails().get(key)
                                + System.lineSeparator());
            }
        } else {
            problemDetail.setDetail(error.getCode().getMessage());
        }

        Optional<URI> uri = error.getCode().getType();

        // map the type and title if we have a code
        uri.ifPresent(problemDetail::setType);

        // set the title and detail according to the code
        if (error.getCode().getMessage() != null) {
            problemDetail.setTitle(error.getCode().getMessage());
        }

        return problemDetail;
    }

    @ExceptionHandler
    @SuppressWarnings({"java:S2259", "java:S1185"})
    protected ResponseEntity<ProblemDetail> handleConstraintViolationException(
            ConstraintViolationException ex) {
        log.error("An exception occurred", ex);
        ProblemDetail problemDetail = getDetailFromEnum(CommonAppError.CONSTRAINT_ERROR, ex);

        problemDetail.setDetail("Constraints failed for fields:" + System.lineSeparator());

        // add the failure specifics to the problem detail properties
        for (ConstraintViolation fieldError : ex.getConstraintViolations()) {
            problemDetail.setDetail(
                    problemDetail.getDetail()
                            + fieldError.getPropertyPath()
                            + "="
                            + fieldError.getMessage());
        }

        problemDetail.setDetail((ex.getMessage() != null ? ex.getMessage() : ""));

        return new ResponseEntity<>(problemDetail, HttpStatus.valueOf(problemDetail.getStatus()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> mismatchType(MethodArgumentTypeMismatchException ex) {
        log.error("An exception occurred", ex);
        ProblemDetail problemDetail = getDetailFromEnum(CommonAppError.TYPE_MISMATCH_ERROR, ex);

        // Add a custom detail message by extracting the relevant information from the exception
        problemDetail.setDetail(
                "Problem with value " + ex.getValue() + " for parameter " + ex.getName());

        return new ResponseEntity<>(problemDetail, HttpStatus.valueOf(problemDetail.getStatus()));
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

        problemDetail.setDetail("Validation failed for fields:");
        problemDetail.setProperties(new java.util.HashMap<>());

        Map<String, Object> errors = new HashMap();

        // add the failure specifics to the problem detail properties
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {

            // we cant check on the sub class of field error. Instead lets check on the name of the
            // class to determine if this is a violation error
            // or a type mismatch error as these are the most common errors we want to give specific
            // messages for
            if (fieldError.getCode() == null || !fieldError.getCode().contains("typeMismatch")) {
                errors.put(fieldError.getField(), fieldError.getDefaultMessage());
            } else {
                // if this is a type mismatch error, we want to give a more specific message about
                // the expected format as this is a common error for our date and time fields
                errors.put(
                        fieldError.getField(),
                        "Please ensure that any times are in the format HH:mm and dates are in the format yyyy-MM-dd");
            }
        }

        problemDetail.setProperty("errors", errors);

        return new ResponseEntity<>(problemDetail, HttpStatus.valueOf(problemDetail.getStatus()));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        log.error("An exception occurred", ex);

        DateTimeParseException dateException = findCause(ex, DateTimeParseException.class);
        InvalidFormatException invalidFormatException =
                findCause(ex, InvalidFormatException.class);

        ProblemDetail problemDetail = getDetailFromEnum(CommonAppError.NOT_READABLE_ERROR, ex);

        // if we have a date exception use that as it gives us a more specific error message
        if (dateException != null) {
            problemDetail.setDetail("Type mismatch error somewhere in payload");
        } else if (invalidFormatException != null) {
            problemDetail.setDetail("Problem setting value for %s please check the correct type is used".formatted(
                invalidFormatException.getPath().size() > 0
                    ? invalidFormatException.getPath().get(0).getFieldName() : "unknown field"));
        }

        return new ResponseEntity<>(problemDetail, HttpStatus.valueOf(problemDetail.getStatus()));
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        log.error("An exception occurred", ex);

        ProblemDetail problemDetail = getDetailFromEnum(CommonAppError.PARAMETER_REQUIRED, ex);
        problemDetail.setDetail(
                "Required request parameter '" + ex.getParameterName() + "' is missing");

        return new ResponseEntity<>(problemDetail, HttpStatus.valueOf(problemDetail.getStatus()));
    }

    /**
     * find the cause of a type.
     *
     * @param ex the exception
     * @param type the type to find
     * @return the identified exception
     */
    public static <T extends Throwable> T findCause(Throwable ex, Class<T> type) {
        Throwable current = ex;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
