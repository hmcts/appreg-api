package uk.gov.hmcts.appregister.common.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.appregister.common.log.SecurityEndpointFailureLogger;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class AppRegExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Set<String> WHOLE_NUMBER_FIELDS =
            Set.of("sequenceNumber", "page", "pageNumber", "pageSize", "size");
    private static final String UNKNOWN_FIELD = "unknown field";

    private static final Set<String> BOOLEAN_FIELDS = Set.of("feeRequired");

    private final SecurityEndpointFailureLogger securityEndpointFailureLogger;

    @ExceptionHandler(AppRegistryException.class)
    ResponseEntity<ProblemDetail> handleAppRegisterApiException(AppRegistryException exception) {

        // getss the core exception code that we used to apply the application specific code
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
        ProblemDetail problemDetail = getDetailFromEnum(CommonAppError.CONSTRAINT_ERROR, ex);

        problemDetail.setDetail("Constraints failed for fields:" + System.lineSeparator());

        // add the failure specifics to the problem detail properties
        for (ConstraintViolation<?> fieldError : ex.getConstraintViolations()) {
            problemDetail.setDetail(
                    problemDetail.getDetail()
                            + fieldError.getPropertyPath()
                            + "="
                            + fieldError.getMessage());
        }

        problemDetail.setDetail((ex.getMessage() != null ? ex.getMessage() : ""));
        logExpectedClientError(problemDetail.getStatus(), problemDetail.getDetail());

        return new ResponseEntity<>(problemDetail, HttpStatus.valueOf(problemDetail.getStatus()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> mismatchType(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problemDetail = getDetailFromEnum(CommonAppError.TYPE_MISMATCH_ERROR, ex);

        // Add a custom detail message by extracting the relevant information from the exception
        problemDetail.setDetail(
                "Problem with value " + ex.getValue() + " for parameter " + ex.getName());
        logExpectedClientError(problemDetail.getStatus(), problemDetail.getDetail());

        return new ResponseEntity<>(problemDetail, HttpStatus.valueOf(problemDetail.getStatus()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problemDetail =
                getDetailFromEnum(CommonAppError.METHOD_ARGUMENT_INVALID_ERROR, ex);

        problemDetail.setDetail("Validation failed for fields:");
        problemDetail.setProperties(new java.util.HashMap<>());

        Map<String, Object> errors = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .forEach(
                        fieldError -> {
                            if (fieldError.getCode() == null
                                    || !fieldError.getCode().contains("typeMismatch")) {
                                errors.put(fieldError.getField(), fieldError.getDefaultMessage());
                            } else if (WHOLE_NUMBER_FIELDS.contains(fieldError.getField())) {
                                errors.put(
                                        fieldError.getField(),
                                        "Please ensure %s is a whole number"
                                                .formatted(fieldError.getField()));
                            } else if (BOOLEAN_FIELDS.contains(fieldError.getField())) {
                                errors.put(
                                        fieldError.getField(),
                                        "Please ensure %s is a valid boolean value"
                                                .formatted(fieldError.getField()));
                            } else {
                                errors.put(
                                        fieldError.getField(),
                                        "Please ensure that any times are in the format HH:mm and dates are in the"
                                                + " format yyyy-MM-dd");
                            }
                        });

        problemDetail.setProperty("errors", errors);
        logExpectedClientError(
                resolveStatusCode(status, problemDetail), summariseBindingErrors(errors));

        return new ResponseEntity<>(problemDetail, HttpStatus.valueOf(problemDetail.getStatus()));
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        logExpectedClientError(status.value(), summariseMethodValidation(ex));
        return super.handleHandlerMethodValidationException(ex, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodValidationException(
            MethodValidationException ex,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request) {
        logExpectedClientError(status.value(), summariseMethodValidation(ex));
        return super.handleMethodValidationException(ex, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        DateTimeParseException dateException = findCause(ex, DateTimeParseException.class);
        InvalidFormatException invalidFormatException = findCause(ex, InvalidFormatException.class);
        ValueInstantiationException valueInstantiationException =
                findCause(ex, ValueInstantiationException.class);

        ProblemDetail problemDetail = getDetailFromEnum(CommonAppError.NOT_READABLE_ERROR, ex);

        // if we have a date exception use that as it gives us a more specific error message
        if (dateException != null) {
            problemDetail.setDetail(dateException.getMessage());
        } else if (isEnumInstantiationProblem(valueInstantiationException)) {
            problemDetail.setDetail(getEnumInstantiationProblemDetail(valueInstantiationException));
        } else if (invalidFormatException != null) {
            problemDetail.setDetail(
                    "Problem setting value for %s please check the correct type is used"
                            .formatted(
                                    !invalidFormatException.getPath().isEmpty()
                                            ? invalidFormatException
                                                    .getPath()
                                                    .getFirst()
                                                    .getFieldName()
                                            : UNKNOWN_FIELD));
        } else {
            problemDetail.setDetail(
                    "Type conversion problem. Something in the payload is not correct");
        }
        logExpectedClientError(resolveStatusCode(status, problemDetail), problemDetail.getDetail());

        return new ResponseEntity<>(problemDetail, HttpStatus.valueOf(problemDetail.getStatus()));
    }

    private boolean isEnumInstantiationProblem(ValueInstantiationException exception) {
        return exception != null
                && exception.getType() != null
                && exception.getType().getRawClass().isEnum();
    }

    private String getEnumInstantiationProblemDetail(ValueInstantiationException exception) {
        Class<?> enumType = exception.getType().getRawClass();
        String acceptedValues =
                String.join(
                        ", ",
                        Arrays.stream(enumType.getEnumConstants()).map(String::valueOf).toList());

        return "Problem setting value for %s. Accepted values are: %s"
                .formatted(getJsonPath(exception), acceptedValues);
    }

    private String getJsonPath(ValueInstantiationException exception) {
        if (exception.getPath().isEmpty()) {
            return UNKNOWN_FIELD;
        }

        StringBuilder path = new StringBuilder();
        for (var reference : exception.getPath()) {
            if (reference.getFieldName() != null) {
                if (!path.isEmpty()) {
                    path.append(".");
                }
                path.append(reference.getFieldName());
            } else if (reference.getIndex() >= 0) {
                path.append("[").append(reference.getIndex()).append("]");
            }
        }

        return path.isEmpty() ? UNKNOWN_FIELD : path.toString();
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problemDetail = getDetailFromEnum(CommonAppError.PARAMETER_REQUIRED, ex);
        problemDetail.setDetail(
                "Required request parameter '" + ex.getParameterName() + "' is missing");
        logExpectedClientError(resolveStatusCode(status, problemDetail), problemDetail.getDetail());

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

    private String summariseBindingErrors(Map<String, Object> errors) {
        return "Validation failed for fields: "
                + errors.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.joining("; "));
    }

    private String summariseMethodValidation(MethodValidationResult validationResult) {
        String parameterErrors =
                validationResult.getParameterValidationResults().stream()
                        .flatMap(
                                result ->
                                        result.getResolvableErrors().stream()
                                                .map(
                                                        error ->
                                                                formatValidationMessage(
                                                                        result.getMethodParameter()
                                                                                .getParameterName(),
                                                                        error)))
                        .collect(Collectors.joining("; "));

        String crossParameterErrors =
                validationResult.getCrossParameterValidationResults().stream()
                        .map(error -> formatValidationMessage("method", error))
                        .collect(Collectors.joining("; "));

        String combined =
                Arrays.stream(new String[] {parameterErrors, crossParameterErrors})
                        .filter(value -> value != null && !value.isBlank())
                        .collect(Collectors.joining("; "));

        if (combined.isBlank()) {
            return "Validation failed for handler method arguments";
        }

        return "Validation failed for handler method arguments: " + combined;
    }

    private String formatValidationMessage(String parameterName, MessageSourceResolvable error) {
        String safeParameterName =
                parameterName != null && !parameterName.isBlank() ? parameterName : UNKNOWN_FIELD;
        String message = error.getDefaultMessage();
        if ((message == null || message.isBlank())
                && error.getCodes() != null
                && error.getCodes().length > 0) {
            message = error.getCodes()[0];
        }
        if (message == null || message.isBlank()) {
            message = error.toString();
        }
        return safeParameterName + "=" + message;
    }

    private void logExpectedClientError(int responseCode, String detail) {
        log.warn("[{}]: {}", responseCode, detail);
    }

    private int resolveStatusCode(HttpStatusCode status, ProblemDetail problemDetail) {
        if (status != null) {
            return status.value();
        }
        return problemDetail.getStatus();
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        securityEndpointFailureLogger.logFailure(
                request, HttpStatus.FORBIDDEN.value(), SecurityEndpointFailureLogger.ACCESS_DENIED);

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ProblemDetail.forStatusAndDetail(
                                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"));
    }
}
