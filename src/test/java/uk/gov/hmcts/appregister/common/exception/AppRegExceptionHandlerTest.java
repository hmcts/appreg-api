package uk.gov.hmcts.appregister.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.lang.reflect.Method;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import nl.altindag.log.LogCaptor;
import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import uk.gov.hmcts.appregister.applicationcode.exception.ApplicationCodeError;
import uk.gov.hmcts.appregister.generated.model.BulkOfficialsUpdateDto;

class AppRegExceptionHandlerTest {
    private AppRegExceptionHandler exceptionHandler;
    private LogCaptor logCaptor;

    @BeforeEach
    void beforeEach() {
        exceptionHandler = new AppRegExceptionHandler();
        logCaptor = LogCaptor.forClass(AppRegExceptionHandler.class);
        logCaptor.clearLogs();
    }

    @Test
    void
            givenAnAppRegisterExceptionWithoutAppCode_whenTheExceptionIsThrown_thenAProblemDetailIsaReturned()
                    throws Exception {
        // setup
        AppRegistryException exception =
                new AppRegistryException(
                        ApplicationCodeError.CODE_NOT_FOUND, "Test message", (Throwable) null);

        // execute
        ResponseEntity<ProblemDetail> problemDetail =
                exceptionHandler.handleAppRegisterApiException(exception);

        // assert
        Assertions.assertEquals(HttpStatusCode.valueOf(404), problemDetail.getStatusCode());
        Assertions.assertNotNull(problemDetail.getBody());
        Assertions.assertEquals(
                ApplicationCodeError.CODE_NOT_FOUND.getCode().getHttpCode().value(),
                problemDetail.getBody().getStatus());
        Assertions.assertEquals(
                ApplicationCodeError.CODE_NOT_FOUND.getCode().getMessage(),
                problemDetail.getBody().getDetail());
        Assertions.assertEquals(
                new URI(ApplicationCodeError.CODE_NOT_FOUND.getCode().getAppCode()),
                problemDetail.getBody().getType());
    }

    @Test
    void
            givenAnAppRegisterExceptionWithAppCode_whenTheExceptionIsThrown_thenAProblemDetailIsaReturned()
                    throws Exception {
        String customMessage = "Custom message";
        String customType = "CustomType";

        // setup
        AppRegistryException exception =
                new AppRegistryException(
                        () ->
                                new DefaultErrorDetail(
                                        HttpStatus.BAD_REQUEST, customMessage, customType),
                        "Test message",
                        (Throwable) null);

        // execute
        ResponseEntity<ProblemDetail> problemDetail =
                exceptionHandler.handleAppRegisterApiException(exception);

        // assert
        Assertions.assertEquals(HttpStatusCode.valueOf(400), problemDetail.getStatusCode());
        Assertions.assertNotNull(problemDetail.getBody());
        Assertions.assertEquals(400, problemDetail.getBody().getStatus());
        Assertions.assertEquals(customMessage, problemDetail.getBody().getDetail());
        Assertions.assertEquals(new URI(customType), problemDetail.getBody().getType());
    }

    @Test
    void
            givenConstraintExceptionWithAppCode_whenTheExceptionIsThrown_thenAProblemDetailIsaReturned()
                    throws Exception {

        String customMessage = "Custom message";

        ConstraintViolation<?> cv =
                ConstraintViolationImpl.forReturnValueValidation(
                        "invalid value",
                        null,
                        null,
                        null,
                        null,
                        null,
                        "propertyPath",
                        "val",
                        PathImpl.createPathFromString("propertyPath"),
                        null,
                        null,
                        null);
        // setup
        ConstraintViolationException exception =
                new ConstraintViolationException(customMessage, Set.of(cv));

        // execute
        ResponseEntity<ProblemDetail> problemDetail =
                exceptionHandler.handleConstraintViolationException(exception);

        // assert
        Assertions.assertEquals(HttpStatusCode.valueOf(400), problemDetail.getStatusCode());
        Assertions.assertNotNull(problemDetail.getBody());
        Assertions.assertEquals(400, problemDetail.getBody().getStatus());
        Assertions.assertEquals(customMessage, problemDetail.getBody().getDetail());
        Assertions.assertEquals(
                CommonAppError.CONSTRAINT_ERROR.getCode().getType().get(),
                problemDetail.getBody().getType());
        Assertions.assertTrue(
                logCaptor.getWarnLogs().stream()
                        .anyMatch(log -> log.contains("[400]: " + customMessage)));
        Assertions.assertTrue(logCaptor.getErrorLogs().isEmpty());
    }

    @Test
    void givenMethodArgumentTypeMismatch_whenHandled_thenWarnIsLoggedWithoutError() {
        MethodArgumentTypeMismatchException exception =
                new MethodArgumentTypeMismatchException(
                        "01-01-2026",
                        LocalDate.class,
                        "date",
                        null,
                        new DateTimeParseException(
                                "Text '01-01-2026' could not be parsed at index 0",
                                "01-01-2026",
                                0));

        ResponseEntity<ProblemDetail> problemDetail = exceptionHandler.mismatchType(exception);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, problemDetail.getStatusCode());
        Assertions.assertEquals(
                "Problem with value 01-01-2026 for parameter date",
                problemDetail.getBody().getDetail());
        Assertions.assertTrue(
                logCaptor.getWarnLogs().stream()
                        .anyMatch(
                                log ->
                                        log.contains(
                                                "[400]: Problem with value 01-01-2026 for"
                                                        + " parameter date")));
        Assertions.assertTrue(logCaptor.getErrorLogs().isEmpty());
    }

    @Test
    void
            givenhandleMethodArgumentExceptionWithAppCode_whenTheExceptionIsThrown_thenAProblemDetailIsaReturned()
                    throws Exception {

        String customMessage = "Custom message";

        BindingResult result = Mockito.mock(BindingResult.class);

        List<FieldError> fieldErrors =
                List.of(
                        new FieldError(
                                "objectName",
                                "field",
                                "rejectedValue",
                                false,
                                null,
                                null,
                                "defaultMessage"));

        Mockito.when(result.getFieldErrors()).thenReturn(fieldErrors);

        // setup
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(null, result) {
                    @Override
                    public String getMessage() {
                        return customMessage;
                    }
                };

        // execute
        ResponseEntity<Object> problemDetail =
                exceptionHandler.handleMethodArgumentNotValid(exception, null, null, null);

        // assert
        Assertions.assertEquals(HttpStatusCode.valueOf(400), problemDetail.getStatusCode());
        Assertions.assertNotNull(problemDetail.getBody());
        Assertions.assertTrue(problemDetail.getBody() instanceof ProblemDetail);
        Assertions.assertEquals(400, ((ProblemDetail) problemDetail.getBody()).getStatus());
        Assertions.assertEquals(
                "Validation failed for fields:",
                ((ProblemDetail) problemDetail.getBody()).getDetail());
        Assertions.assertEquals(
                "defaultMessage",
                ((Map) ((ProblemDetail) problemDetail.getBody()).getProperties().get("errors"))
                        .get("field"));

        Assertions.assertEquals(
                CommonAppError.METHOD_ARGUMENT_INVALID_ERROR.getCode().getType().get(),
                ((ProblemDetail) problemDetail.getBody()).getType());
        Assertions.assertTrue(
                logCaptor.getWarnLogs().stream()
                        .anyMatch(log -> log.contains("[400]: Validation failed for fields:")));
        Assertions.assertTrue(logCaptor.getErrorLogs().isEmpty());
    }

    @Test
    void givenMultipleFieldErrors_whenTheExceptionIsThrown_thenErrorsAreReturnedInSortedOrder()
            throws Exception {

        BindingResult result = Mockito.mock(BindingResult.class);

        List<FieldError> fieldErrors =
                List.of(
                        new FieldError(
                                "objectName",
                                "zField",
                                "rejectedValue",
                                false,
                                null,
                                null,
                                "zMessage"),
                        new FieldError(
                                "objectName",
                                "aField",
                                "rejectedValue",
                                false,
                                null,
                                null,
                                "aMessage"));

        Mockito.when(result.getFieldErrors()).thenReturn(fieldErrors);

        String customMessage = "Custom message";

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(null, result) {
                    @Override
                    public String getMessage() {
                        return customMessage;
                    }
                };

        ResponseEntity<Object> problemDetail =
                exceptionHandler.handleMethodArgumentNotValid(exception, null, null, null);

        Assertions.assertNotNull(problemDetail);
        Assertions.assertNotNull(problemDetail.getBody());

        ProblemDetail body = (ProblemDetail) problemDetail.getBody();
        Assertions.assertNotNull(body.getProperties());

        Object errorsObj = body.getProperties().get("errors");
        Assertions.assertInstanceOf(Map.class, errorsObj);

        Map<?, ?> errors = (Map<?, ?>) errorsObj;
        Assertions.assertEquals(List.of("aField", "zField"), List.copyOf(errors.keySet()));
    }

    @Test
    void givenWholeNumberTypeMismatch_whenTheExceptionIsThrown_thenWholeNumberMessageIsReturned()
            throws Exception {

        BindingResult result = Mockito.mock(BindingResult.class);

        List<FieldError> fieldErrors =
                List.of(
                        new FieldError(
                                "objectName",
                                "sequenceNumber",
                                "NaN",
                                false,
                                new String[] {"typeMismatch"},
                                null,
                                "defaultMessage"));

        Mockito.when(result.getFieldErrors()).thenReturn(fieldErrors);

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(null, result) {
                    @Override
                    public String getMessage() {
                        return "type mismatch";
                    }
                };

        ResponseEntity<Object> problemDetail =
                exceptionHandler.handleMethodArgumentNotValid(exception, null, null, null);

        Assertions.assertNotNull(problemDetail);
        Assertions.assertNotNull(problemDetail.getBody());

        ProblemDetail body = (ProblemDetail) problemDetail.getBody();
        Map<?, ?> errors = (Map<?, ?>) body.getProperties().get("errors");

        Assertions.assertEquals(
                "Please ensure sequenceNumber is a whole number", errors.get("sequenceNumber"));
    }

    @Test
    void givenBooleanTypeMismatch_whenTheExceptionIsThrown_thenBooleanMessageIsReturned()
            throws Exception {

        BindingResult result = Mockito.mock(BindingResult.class);

        List<FieldError> fieldErrors =
                List.of(
                        new FieldError(
                                "objectName",
                                "feeRequired",
                                "maybe",
                                false,
                                new String[] {"typeMismatch"},
                                null,
                                "defaultMessage"));

        Mockito.when(result.getFieldErrors()).thenReturn(fieldErrors);

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(null, result) {
                    @Override
                    public String getMessage() {
                        return "type mismatch";
                    }
                };

        ResponseEntity<Object> problemDetail =
                exceptionHandler.handleMethodArgumentNotValid(exception, null, null, null);

        Assertions.assertNotNull(problemDetail);
        Assertions.assertNotNull(problemDetail.getBody());

        ProblemDetail body = (ProblemDetail) problemDetail.getBody();
        Map<?, ?> errors = (Map<?, ?>) body.getProperties().get("errors");

        Assertions.assertEquals(
                "Please ensure feeRequired is a valid boolean value", errors.get("feeRequired"));
    }

    @Test
    void
            givenHttpMessageNotReadableExceptionWithAppCode_whenTheExceptionIsThrown_thenAProblemDetailIsaReturned()
                    throws Exception {

        String content = "Type conversion problem. Something in the payload is not correct";

        // setup
        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException(content, (HttpInputMessage) null);

        // execute
        ResponseEntity<Object> problemDetail =
                exceptionHandler.handleHttpMessageNotReadable(exception, null, null, null);

        // assert
        Assertions.assertEquals(HttpStatusCode.valueOf(400), problemDetail.getStatusCode());
        Assertions.assertNotNull(problemDetail.getBody());
        Assertions.assertTrue(problemDetail.getBody() instanceof ProblemDetail);

        Assertions.assertEquals(400, ((ProblemDetail) problemDetail.getBody()).getStatus());
        Assertions.assertEquals(content, ((ProblemDetail) problemDetail.getBody()).getDetail());
        Assertions.assertEquals(
                CommonAppError.NOT_READABLE_ERROR.getCode().getType().get(),
                ((ProblemDetail) problemDetail.getBody()).getType());
        Assertions.assertTrue(
                logCaptor.getWarnLogs().stream()
                        .anyMatch(log -> log.contains("[400]: " + content)));
        Assertions.assertTrue(logCaptor.getErrorLogs().isEmpty());
    }

    @Test
    void
            givenHttpMessageNotReadableDateExceptionWithAppCode_whenTheExceptionIsThrown_thenAProblemDetailIsaReturned()
                    throws Exception {

        String content = "Not Readable Error";
        String dateExContent = "Date type mismatch error somewhere in payload";

        DateTimeParseException dateTimeParseException =
                new DateTimeParseException(dateExContent, "parsedString", 0);

        // setup
        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException(content, dateTimeParseException, null);

        // execute
        ResponseEntity<Object> problemDetail =
                exceptionHandler.handleHttpMessageNotReadable(exception, null, null, null);

        // assert
        Assertions.assertEquals(HttpStatusCode.valueOf(400), problemDetail.getStatusCode());
        Assertions.assertNotNull(problemDetail.getBody());
        Assertions.assertTrue(problemDetail.getBody() instanceof ProblemDetail);

        Assertions.assertEquals(400, problemDetail.getStatusCode().value());
        Assertions.assertEquals(
                dateExContent, ((ProblemDetail) problemDetail.getBody()).getDetail());
        Assertions.assertEquals(
                CommonAppError.NOT_READABLE_ERROR.getCode().getType().get(),
                ((ProblemDetail) problemDetail.getBody()).getType());
        Assertions.assertTrue(
                logCaptor.getWarnLogs().stream()
                        .anyMatch(log -> log.contains("[400]: " + dateExContent)));
        Assertions.assertTrue(logCaptor.getErrorLogs().isEmpty());
    }

    @Test
    void givenHttpMessageNotReadableEnumException_whenThrown_thenProblemDetailIsReturned() {
        String content = "Not Readable Error";
        String body =
                """
                {
                  "entryIds": ["3fa85f64-5717-4562-b3fc-2c963f66afa6"],
                  "officials": [
                    {
                      "type": "JUDGE"
                    }
                  ]
                }
                """;

        Exception cause =
                Assertions.assertThrows(
                        Exception.class,
                        () -> new ObjectMapper().readValue(body, BulkOfficialsUpdateDto.class));

        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException(content, cause, null);

        ResponseEntity<Object> problemDetail =
                exceptionHandler.handleHttpMessageNotReadable(exception, null, null, null);

        Assertions.assertEquals(HttpStatusCode.valueOf(400), problemDetail.getStatusCode());
        Assertions.assertNotNull(problemDetail.getBody());
        Assertions.assertTrue(problemDetail.getBody() instanceof ProblemDetail);

        Assertions.assertEquals(400, problemDetail.getStatusCode().value());
        Assertions.assertEquals(
                "Problem setting value for officials[0].type. Accepted values are: MAGISTRATE, CLERK",
                ((ProblemDetail) problemDetail.getBody()).getDetail());
        Assertions.assertEquals(
                CommonAppError.NOT_READABLE_ERROR.getCode().getType().get(),
                ((ProblemDetail) problemDetail.getBody()).getType());
        Assertions.assertTrue(
                logCaptor.getWarnLogs().stream()
                        .anyMatch(
                                log ->
                                        log.contains(
                                                "[400]: Problem setting value for officials[0].type."
                                                        + " Accepted values are: MAGISTRATE,"
                                                        + " CLERK")));
        Assertions.assertTrue(logCaptor.getErrorLogs().isEmpty());
    }

    @Test
    void givenMissingRequestParameter_whenHandled_thenWarnIsLoggedWithoutError() {
        MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("date", "LocalDate");

        ResponseEntity<Object> problemDetail =
                exceptionHandler.handleMissingServletRequestParameter(exception, null, null, null);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, problemDetail.getStatusCode());
        Assertions.assertTrue(
                logCaptor.getWarnLogs().stream()
                        .anyMatch(
                                log ->
                                        log.contains(
                                                "[400]: Required request parameter 'date' is"
                                                        + " missing")));
        Assertions.assertTrue(logCaptor.getErrorLogs().isEmpty());
    }

    @Test
    void givenHandlerMethodValidationException_whenHandled_thenWarnIsLoggedWithoutError()
            throws NoSuchMethodException {
        MethodParameter methodParameter =
                new MethodParameter(
                        AppRegExceptionHandlerTest.class.getDeclaredMethod(
                                "sampleValidationMethod", String.class),
                        0);

        ParameterValidationResult validationResult =
                new ParameterValidationResult(
                        methodParameter,
                        "ABCDEFGHIJK",
                        List.of(
                                new DefaultMessageSourceResolvable(
                                        "size must be between 0 and 10")),
                        null,
                        null,
                        null,
                        (error, sourceType) -> null);

        HandlerMethodValidationException exception =
                new HandlerMethodValidationException(
                        MethodValidationResult.create(
                                this, methodParameter.getMethod(), List.of(validationResult)));

        ResponseEntity<Object> problemDetail =
                exceptionHandler.handleHandlerMethodValidationException(
                        exception, null, HttpStatus.BAD_REQUEST, null);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, problemDetail.getStatusCode());
        Assertions.assertTrue(
                logCaptor.getWarnLogs().stream()
                        .anyMatch(
                                log ->
                                        log.contains(
                                                "[400]: Validation failed for handler method"
                                                        + " arguments: code=size must be between 0"
                                                        + " and 10")));
        Assertions.assertTrue(logCaptor.getErrorLogs().isEmpty());
    }

    @Test
    void givenEmptyHandlerMethodValidationException_whenHandled_thenGenericWarnIsLogged()
            throws NoSuchMethodException {
        MethodValidationResult validationResult = Mockito.mock(MethodValidationResult.class);
        Mockito.when(validationResult.getParameterValidationResults()).thenReturn(List.of());
        Mockito.when(validationResult.getCrossParameterValidationResults()).thenReturn(List.of());

        HandlerMethodValidationException exception =
                new HandlerMethodValidationException(validationResult);

        ResponseEntity<Object> problemDetail =
                exceptionHandler.handleHandlerMethodValidationException(
                        exception, null, HttpStatus.BAD_REQUEST, null);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, problemDetail.getStatusCode());
        Assertions.assertTrue(
                logCaptor.getWarnLogs().stream()
                        .anyMatch(
                                log ->
                                        log.contains(
                                                "[400]: Validation failed for handler method"
                                                        + " arguments")));
    }

    @Test
    void
            givenBlankParameterNameAndCodeOnlyError_whenFormattingValidationMessage_thenUnknownFieldIsUsed()
                    throws Exception {
        Method method =
                AppRegExceptionHandler.class.getDeclaredMethod(
                        "formatValidationMessage",
                        String.class,
                        org.springframework.context.MessageSourceResolvable.class);
        method.setAccessible(true);

        String formatted =
                (String)
                        method.invoke(
                                exceptionHandler,
                                " ",
                                new DefaultMessageSourceResolvable(
                                        new String[] {"size must be less than or equal to 100"},
                                        null,
                                        null));

        Assertions.assertEquals("unknown field=size must be less than or equal to 100", formatted);
    }

    @Test
    void
            givenResolvableWithoutMessageOrCodes_whenFormattingValidationMessage_thenToStringFallbackIsUsed()
                    throws Exception {
        Method method =
                AppRegExceptionHandler.class.getDeclaredMethod(
                        "formatValidationMessage",
                        String.class,
                        org.springframework.context.MessageSourceResolvable.class);
        method.setAccessible(true);

        String formatted =
                (String)
                        method.invoke(
                                exceptionHandler,
                                null,
                                new DefaultMessageSourceResolvable(null, null, null) {
                                    @Override
                                    public String toString() {
                                        return "fallback-text";
                                    }
                                });

        Assertions.assertEquals("unknown field=fallback-text", formatted);
    }

    @Test
    void givenNonNullStatus_whenResolvingStatusCode_thenExplicitStatusIsUsed() throws Exception {
        Method method =
                AppRegExceptionHandler.class.getDeclaredMethod(
                        "resolveStatusCode", HttpStatusCode.class, ProblemDetail.class);
        method.setAccessible(true);

        int status =
                (int)
                        method.invoke(
                                exceptionHandler,
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST));

        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), status);
    }

    @Test
    void
            givenAccessDeniedException_whenTheExceptionIsThrown_thenForbiddenProblemDetailIsReturned() {
        // setup
        AccessDeniedException exception = new AccessDeniedException("Forbidden");

        // execute
        ResponseEntity<ProblemDetail> problemDetail =
                exceptionHandler.handleAccessDenied(exception);

        // assert
        Assertions.assertEquals(HttpStatusCode.valueOf(403), problemDetail.getStatusCode());
        Assertions.assertNotNull(problemDetail.getBody());
        Assertions.assertEquals(403, problemDetail.getBody().getStatus());
        Assertions.assertEquals("Access denied", problemDetail.getBody().getDetail());
    }

    @Test
    void givenUnexpectedException_whenTheExceptionIsThrown_thenAProblemDetailIsReturned() {
        // setup
        RuntimeException exception = new RuntimeException("boom");

        // execute
        ResponseEntity<ProblemDetail> problemDetail =
                exceptionHandler.handleUnexpectedException(exception);

        // assert
        Assertions.assertEquals(HttpStatusCode.valueOf(500), problemDetail.getStatusCode());
        Assertions.assertNotNull(problemDetail.getBody());
        Assertions.assertEquals(500, problemDetail.getBody().getStatus());
        Assertions.assertEquals(
                "An unexpected error occurred", problemDetail.getBody().getDetail());
    }

    @SuppressWarnings("unused")
    private void sampleValidationMethod(String code) {
        // used to create a MethodParameter with a stable name for validation tests
    }
}
