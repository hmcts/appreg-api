package uk.gov.hmcts.appregister.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.context.request.NativeWebRequest;

import org.springframework.web.util.ContentCachingRequestWrapper;

import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.common.log.LogPayloads;
import uk.gov.hmcts.appregister.common.log.SecurityEndpointFailureLogger;
import uk.gov.hmcts.appregister.common.util.ObfuscationUtil;
import uk.gov.hmcts.appregister.csds.ingress.database.CsdsBatchUpsertException;

import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.tomcat.util.http.InvalidParameterException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.annotation.AnnotatedElementUtils;
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
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class AppRegExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Set<String> WHOLE_NUMBER_FIELDS =
            Set.of("sequenceNumber", "page", "pageNumber", "pageSize", "size");
    private static final String UNKNOWN_FIELD = "unknown field";
    private static final String ACTIVITY_TYPES_FIELD = "activityTypes";
    private static final String ACTIVITY_TYPES_REQUIRED_MESSAGE =
            "At least 1 activity must be provided";
    private static final Set<String> ACTIVITY_TYPES_REQUIRED_ERROR_CODES =
            Set.of("NotNull", "NotEmpty", "Size");

    private static final Set<String> BOOLEAN_FIELDS = Set.of("feeRequired");

    private final SecurityEndpointFailureLogger securityEndpointFailureLogger;

    @ExceptionHandler(AppRegistryException.class)
    ResponseEntity<ProblemDetail> handleAppRegisterApiException(AppRegistryException exception) {

        // gets the core exception code that we used to apply the application specific code
        ErrorCodeEnum error = exception.getCode();

        ProblemDetail problemDetail = getDetailFromEnum(exception.getCode(), exception);
        HttpStatus httpStatus = error.getCode().getHttpCode();
        logAppRegistryException(httpStatus, problemDetail, exception);

        return new ResponseEntity<>(problemDetail, httpStatus);
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

        if (ex.getConstraintViolations().isEmpty()) {
            problemDetail.setDetail(ex.getMessage() != null ? ex.getMessage() : "");
        } else {
            String constraintDetail =
                    ex.getConstraintViolations().stream()
                            .sorted(
                                    Comparator.comparing(
                                            fieldError -> fieldError.getPropertyPath().toString()))
                            .map(
                                    fieldError ->
                                            fieldError.getPropertyPath()
                                                    + "="
                                                    + fieldError.getMessage())
                            .collect(Collectors.joining(System.lineSeparator()));
            problemDetail.setDetail(
                    "Constraints failed for fields:" + System.lineSeparator() + constraintDetail);
        }
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
    protected @Nullable ResponseEntity<@NonNull Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problemDetail =
                getDetailFromEnum(CommonAppError.METHOD_ARGUMENT_INVALID_ERROR, ex);

        problemDetail.setDetail("Validation failed for fields:");
        problemDetail.setProperties(new HashMap<>());

        Map<String, Object> errors = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .forEach(
                        fieldError ->
                                errors.put(
                                        fieldError.getField(), getFieldErrorMessage(fieldError)));

        problemDetail.setProperty("errors", errors);
        logExpectedClientError(
                resolveStatusCode(status, problemDetail), summariseBindingErrors(errors));

        return new ResponseEntity<>(problemDetail, HttpStatus.valueOf(problemDetail.getStatus()));
    }

    private String getFieldErrorMessage(FieldError fieldError) {
        if (isActivityTypesRequiredError(fieldError)) {
            return ACTIVITY_TYPES_REQUIRED_MESSAGE;
        }

        if (!isTypeMismatch(fieldError)) {
            return fieldError.getDefaultMessage();
        }

        if (WHOLE_NUMBER_FIELDS.contains(fieldError.getField())) {
            return "Please ensure %s is a whole number".formatted(fieldError.getField());
        }

        if (BOOLEAN_FIELDS.contains(fieldError.getField())) {
            return "Please ensure %s is a valid boolean value".formatted(fieldError.getField());
        }

        return "Please ensure that any times are in the format HH:mm and dates are in the"
                + " format yyyy-MM-dd";
    }

    private boolean isActivityTypesRequiredError(FieldError fieldError) {
        return ACTIVITY_TYPES_FIELD.equals(fieldError.getField())
                && ACTIVITY_TYPES_REQUIRED_ERROR_CODES.contains(getPrimaryErrorCode(fieldError));
    }

    private boolean isTypeMismatch(FieldError fieldError) {
        String code = fieldError.getCode();
        return code != null && code.contains("typeMismatch");
    }

    private String getPrimaryErrorCode(FieldError fieldError) {
        String code = fieldError.getCode();
        if (code == null) {
            return "";
        }

        int delimiterIndex = code.indexOf('.');
        return delimiterIndex < 0 ? code : code.substring(0, delimiterIndex);
    }

    @Override
    protected @Nullable ResponseEntity<@NonNull Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        logExpectedClientError(status.value(), summariseMethodValidation(ex));
        return super.handleHandlerMethodValidationException(ex, headers, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<@NonNull Object> handleMethodValidationException(
            MethodValidationException ex,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request) {
        logExpectedClientError(status.value(), summariseMethodValidation(ex));
        return super.handleMethodValidationException(ex, headers, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<@NonNull Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        DateTimeParseException dateException = findCause(ex, DateTimeParseException.class);
        InvalidFormatException invalidFormatException = findCause(ex, InvalidFormatException.class);
        UnrecognizedPropertyException unrecognizedPropertyException =
                findCause(ex, UnrecognizedPropertyException.class);
        ValueInstantiationException valueInstantiationException =
                findCause(ex, ValueInstantiationException.class);

        getLogPayloadsAnnotation(request)
                .ifPresent(
                        logPayloads -> {
                            if(logPayloads.direction().includesRequest()) {
                                logPayloads.level().log(
                                    log,
                                    "{}: {}",
                                    logPayloads.requestPrefix(),
                                    ObfuscationUtil.getObfuscatedString(getRequestPayload(request)));
                            }
                        });

        ProblemDetail problemDetail = getDetailFromEnum(CommonAppError.NOT_READABLE_ERROR, ex);

        // if we have a date exception use that as it gives us a more specific error message
        if (dateException != null) {
            problemDetail.setDetail(dateException.getMessage());
        } else if (isEnumInstantiationProblem(valueInstantiationException)) {
            problemDetail.setDetail(getEnumInstantiationProblemDetail(valueInstantiationException));
        } else if (unrecognizedPropertyException != null) {
            problemDetail.setDetail(
                    "Unsupported request field: " + getJsonPath(unrecognizedPropertyException));
        } else if (invalidFormatException != null) {
            problemDetail.setDetail(
                    "Problem setting value for %s please check the correct type is used"
                            .formatted(
                                    invalidFormatException.getPath().isEmpty()
                                            ? UNKNOWN_FIELD
                                            : invalidFormatException
                                                    .getPath()
                                                    .getFirst()
                                                    .getFieldName()));
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
        return getJsonPath((JsonMappingException) exception);
    }

    private String getJsonPath(JsonMappingException exception) {
        if (exception.getPath().isEmpty()) {
            return UNKNOWN_FIELD;
        }

        StringBuilder path = new StringBuilder();
        for (var reference : exception.getPath()) {
            if (reference.getFieldName() != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(reference.getFieldName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }

        return path.isEmpty() ? UNKNOWN_FIELD : path.toString();
    }

    @Override
    protected @Nullable ResponseEntity<@NonNull Object> handleMissingServletRequestParameter(
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

    @Override
    protected @Nullable ResponseEntity<@NonNull Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ResponseEntity<ProblemDetail> response =
                handleAppRegisterApiException(
                        new AppRegistryException(
                                AppListEntryError.BULK_UPLOAD_FILE_TOO_LARGE,
                                "Uploaded file exceeded the configured maximum size",
                                ex));

        return new ResponseEntity<>(
                response.getBody(), response.getHeaders(), response.getStatusCode());
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

    private void logAppRegistryException(
            HttpStatus httpStatus, ProblemDetail problemDetail, AppRegistryException exception) {
        String detail = problemDetail.getDetail();
        String exceptionMessage = exception.getMessage();

        if (exceptionMessage != null
                && !exceptionMessage.isBlank()
                && !exceptionMessage.equals(detail)) {
            detail = detail + " (" + exceptionMessage + ")";
        }

        if (httpStatus.is4xxClientError()) {
            logExpectedClientError(httpStatus.value(), detail);
            return;
        }

        log.error("[{}]: {}", httpStatus.value(), detail, exception);
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

    @ExceptionHandler(CsdsBatchUpsertException.class)
    public ResponseEntity<ProblemDetail> handleCsdsBatchUpsertException(
            CsdsBatchUpsertException ex) {
        logExpectedClientError(HttpStatus.BAD_REQUEST.value(), ex.logSummary());

        var problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.clientMessage());
        problemDetail.setTitle("CSDS ingest failed");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<ProblemDetail> handleInvalidParameterException(
            InvalidParameterException ex) {
        ProblemDetail problemDetail = getDetailFromEnum(CommonAppError.NOT_READABLE_ERROR, ex);
        problemDetail.setDetail("Malformed query parameter encoding");
        logExpectedClientError(problemDetail.getStatus(), problemDetail.getDetail());

        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ProblemDetail.forStatusAndDetail(
                                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"));
    }

    private Optional<LogPayloads> getLogPayloadsAnnotation(WebRequest request) {
        Object handler =
            request.getAttribute(
                HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE,
                RequestAttributes.SCOPE_REQUEST);

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return Optional.empty();
        }

        Method specificMethod =
            AopUtils.getMostSpecificMethod(
                handlerMethod.getMethod(),
                handlerMethod.getBeanType());

        LogPayloads logPayloads =
            AnnotatedElementUtils.findMergedAnnotation(specificMethod, LogPayloads.class);

        return Optional.ofNullable(logPayloads);
    }

    private String getRequestPayload(WebRequest request) {
        if (!(request instanceof NativeWebRequest nativeWebRequest)) {
            return "";
        }

        ContentCachingRequestWrapper cachingRequest =
            nativeWebRequest.getNativeRequest(ContentCachingRequestWrapper.class);

        if (cachingRequest == null || cachingRequest.getContentAsByteArray().length ==0) {
            return "";
        }

        Charset charset = StandardCharsets.UTF_8;
        String characterEncoding = cachingRequest.getCharacterEncoding();

        if (characterEncoding != null && !characterEncoding.isBlank()) {
            charset = Charset.forName(characterEncoding);
        }

        return new String(cachingRequest.getContentAsByteArray(), charset);
    }
}
