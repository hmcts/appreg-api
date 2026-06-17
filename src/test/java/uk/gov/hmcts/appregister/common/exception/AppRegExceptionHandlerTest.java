package uk.gov.hmcts.appregister.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.lang.reflect.Method;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import uk.gov.hmcts.appregister.applicationcode.exception.ApplicationCodeError;
import uk.gov.hmcts.appregister.common.log.SecurityEndpointFailureLogger;
import uk.gov.hmcts.appregister.generated.model.BulkOfficialsUpdateDto;

class AppRegExceptionHandlerTest {
    private static final HttpHeaders HEADERS = HttpHeaders.EMPTY;
    private static final HttpStatusCode BAD_REQUEST = HttpStatus.BAD_REQUEST;

    private AppRegExceptionHandler exceptionHandler;
    private SecurityEndpointFailureLogger securityEndpointFailureLogger;
    private LogCaptor logCaptor;
    private WebRequest webRequest;

    @BeforeEach
    void beforeEach() {
        securityEndpointFailureLogger = Mockito.mock(SecurityEndpointFailureLogger.class);
        exceptionHandler = new AppRegExceptionHandler(securityEndpointFailureLogger);
        logCaptor = LogCaptor.forClass(AppRegExceptionHandler.class);
        logCaptor.clearLogs();
        webRequest = Mockito.mock(WebRequest.class);
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
        Assertions.assertTrue(
                logCaptor.getWarnLogs().stream()
                        .anyMatch(
                                log ->
                                        log.contains(
                                                "[404]: Application code not found (Test message)")));
        assertThat(logCaptor.getErrorLogs()).isEmpty();
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
        Assertions.assertTrue(
                logCaptor.getWarnLogs().stream()
                        .anyMatch(log -> log.contains("[400]: Custom message (Test message)")));
        assertThat(logCaptor.getErrorLogs()).isEmpty();
    }

    @Test
    void givenServerSideAppRegisterException_whenHandled_thenErrorIsLoggedWithStatusAndDetail() {
        AppRegistryException exception =
                new AppRegistryException(
                        CommonAppError.INTERNAL_SERVER_ERROR, "Report output file failed");

        ResponseEntity<ProblemDetail> problemDetail =
                exceptionHandler.handleAppRegisterApiException(exception);

        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, problemDetail.getStatusCode());
        Assertions.assertNotNull(problemDetail.getBody());
        Assertions.assertEquals(500, problemDetail.getBody().getStatus());
        Assertions.assertEquals(
                CommonAppError.INTERNAL_SERVER_ERROR.getCode().getMessage(),
                problemDetail.getBody().getDetail());
        assertThat(logCaptor.getWarnLogs()).isEmpty();
        Assertions.assertTrue(
                logCaptor.getErrorLogs().stream()
                        .anyMatch(
                                log ->
                                        log.contains(
                                                "[500]: General unexpected failure"
                                                        + " (Report output file failed)")));
    }

    @Test
    void
            givenConstraintExceptionWithAppCode_whenTheExceptionIsThrown_thenAProblemDetailIsReturned() {

        String customMessage = "Custom message";

        ConstraintViolation<?> cv = Mockito.mock(ConstraintViolation.class);
        Mockito.when(cv.getPropertyPath()).thenReturn(path("propertyPath"));
        Mockito.when(cv.getMessage()).thenReturn("invalid value");
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
        Assertions.assertEquals(
                "Constraints failed for fields:"
                        + System.lineSeparator()
                        + "propertyPath=invalid value",
                problemDetail.getBody().getDetail());
        Assertions.assertEquals(
                CommonAppError.CONSTRAINT_ERROR.getCode().getType().get(),
                problemDetail.getBody().getType());
        Assertions.assertTrue(
                logCaptor.getWarnLogs().stream()
                        .anyMatch(
                                log ->
                                        log.contains(
                                                "[400]: Constraints failed for fields:"
                                                        + System.lineSeparator()
                                                        + "propertyPath=invalid value")));
        assertThat(logCaptor.getErrorLogs()).isEmpty();
    }

    @Test
    void givenConstraintViolations_whenHandled_thenFieldDetailsAreReturnedAndLogged() {
        ConstraintViolation<?> secondViolation = Mockito.mock(ConstraintViolation.class);
        Mockito.when(secondViolation.getPropertyPath()).thenReturn(path("bField"));
        Mockito.when(secondViolation.getMessage()).thenReturn("another invalid value");
        ConstraintViolation<?> firstViolation = Mockito.mock(ConstraintViolation.class);
        Mockito.when(firstViolation.getPropertyPath()).thenReturn(path("aField"));
        Mockito.when(firstViolation.getMessage()).thenReturn("invalid value");

        ConstraintViolationException exception =
                new ConstraintViolationException(
                        "Custom message", Set.of(secondViolation, firstViolation));

        ResponseEntity<ProblemDetail> problemDetail =
                exceptionHandler.handleConstraintViolationException(exception);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, problemDetail.getStatusCode());
        Assertions.assertEquals(
                "Constraints failed for fields:"
                        + System.lineSeparator()
                        + "aField=invalid value"
                        + System.lineSeparator()
                        + "bField=another invalid value",
                problemDetail.getBody().getDetail());
        Assertions.assertTrue(
                logCaptor.getWarnLogs().stream()
                        .anyMatch(
                                log ->
                                        log.contains(
                                                "[400]: Constraints failed for fields:"
                                                        + System.lineSeparator()
                                                        + "aField=invalid value"
                                                        + System.lineSeparator()
                                                        + "bField=another invalid value")));
    }

    @Test
    void givenConstraintViolationsAreEmpty_whenHandled_thenExceptionMessageIsReturned() {
        ConstraintViolationException exception =
                new ConstraintViolationException("Custom message", Set.of());

        ResponseEntity<ProblemDetail> problemDetail =
                exceptionHandler.handleConstraintViolationException(exception);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, problemDetail.getStatusCode());
        Assertions.assertEquals("Custom message", problemDetail.getBody().getDetail());
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
        assertThat(logCaptor.getErrorLogs()).isEmpty();
    }

    @Test
    void
            givenhandleMethodArgumentExceptionWithAppCode_whenTheExceptionIsThrown_thenAProblemDetailIsaReturned() {

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
                exceptionHandler.handleMethodArgumentNotValid(
                        exception, HEADERS, BAD_REQUEST, webRequest);

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
        assertThat(logCaptor.getErrorLogs()).isEmpty();
    }

    @Test
    void givenMultipleFieldErrors_whenTheExceptionIsThrown_thenErrorsAreReturnedInSortedOrder() {

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
                exceptionHandler.handleMethodArgumentNotValid(
                        exception, HEADERS, BAD_REQUEST, webRequest);

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
    void givenWholeNumberTypeMismatch_whenTheExceptionIsThrown_thenWholeNumberMessageIsReturned() {

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
                exceptionHandler.handleMethodArgumentNotValid(
                        exception, HEADERS, BAD_REQUEST, webRequest);

        Assertions.assertNotNull(problemDetail);
        Assertions.assertNotNull(problemDetail.getBody());

        ProblemDetail body = (ProblemDetail) problemDetail.getBody();
        Map<?, ?> errors = (Map<?, ?>) body.getProperties().get("errors");

        Assertions.assertEquals(
                "Please ensure sequenceNumber is a whole number", errors.get("sequenceNumber"));
    }

    @Test
    void givenBooleanTypeMismatch_whenTheExceptionIsThrown_thenBooleanMessageIsReturned() {

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
                exceptionHandler.handleMethodArgumentNotValid(
                        exception, HEADERS, BAD_REQUEST, webRequest);

        Assertions.assertNotNull(problemDetail);
        Assertions.assertNotNull(problemDetail.getBody());

        ProblemDetail body = (ProblemDetail) problemDetail.getBody();
        Map<?, ?> errors = (Map<?, ?>) body.getProperties().get("errors");

        Assertions.assertEquals(
                "Please ensure feeRequired is a valid boolean value", errors.get("feeRequired"));
    }

    @Test
    void
            givenHttpMessageNotReadableExceptionWithAppCode_whenTheExceptionIsThrown_thenAProblemDetailIsaReturned() {

        String content = "Type conversion problem. Something in the payload is not correct";

        // setup
        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException(content, (HttpInputMessage) null);

        // execute
        ResponseEntity<Object> problemDetail =
                exceptionHandler.handleHttpMessageNotReadable(
                        exception, HEADERS, BAD_REQUEST, webRequest);

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
        assertThat(logCaptor.getErrorLogs()).isEmpty();
    }

    @Test
    void
            givenHttpMessageNotReadableDateException_whenTheExceptionIsThrown_thenAProblemDetailIsReturned() {

        String content = "Not Readable Error";
        String dateExContent = "Date type mismatch error somewhere in payload";

        DateTimeParseException dateTimeParseException =
                new DateTimeParseException(dateExContent, "parsedString", 0);

        // setup
        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException(content, dateTimeParseException, null);

        // execute
        ResponseEntity<Object> problemDetail =
                exceptionHandler.handleHttpMessageNotReadable(
                        exception, HEADERS, BAD_REQUEST, webRequest);

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
        assertThat(logCaptor.getErrorLogs()).isEmpty();
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
        ObjectMapper objectMapper = new ObjectMapper();

        Exception cause =
                Assertions.assertThrows(
                        Exception.class,
                        () -> objectMapper.readValue(body, BulkOfficialsUpdateDto.class));

        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException(content, cause, null);

        ResponseEntity<Object> problemDetail =
                exceptionHandler.handleHttpMessageNotReadable(
                        exception, HEADERS, BAD_REQUEST, webRequest);

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
        assertThat(logCaptor.getErrorLogs()).isEmpty();
    }

    @Test
    void
            givenHttpMessageNotReadableUnknownPropertyException_whenThrown_thenProblemDetailIsReturned() {
        String content = "Not Readable Error";
        String body =
                """
                {
                  "dateFrom": "2025-10-01",
                  "courtCode": "LOC123"
                }
                """;
        ObjectMapper objectMapper =
                new ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

        var cause =
                Assertions.assertThrows(
                        Exception.class,
                        () -> objectMapper.readValue(body, StrictRequestDto.class));

        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException(content, cause, null);

        ResponseEntity<Object> problemDetail =
                exceptionHandler.handleHttpMessageNotReadable(exception, null, null, null);

        Assertions.assertEquals(HttpStatusCode.valueOf(400), problemDetail.getStatusCode());
        Assertions.assertNotNull(problemDetail.getBody());
        Assertions.assertTrue(problemDetail.getBody() instanceof ProblemDetail);

        Assertions.assertEquals(400, problemDetail.getStatusCode().value());
        Assertions.assertEquals(
                "Unsupported request field: courtCode",
                ((ProblemDetail) problemDetail.getBody()).getDetail());
        Assertions.assertEquals(
                CommonAppError.NOT_READABLE_ERROR.getCode().getType().get(),
                ((ProblemDetail) problemDetail.getBody()).getType());
        Assertions.assertTrue(
                logCaptor.getWarnLogs().stream()
                        .anyMatch(
                                log ->
                                        log.contains(
                                                "[400]: Unsupported request field: courtCode")));
        assertThat(logCaptor.getErrorLogs()).isEmpty();
    }

    @Test
    void
            givenHttpMessageNotReadableNestedUnknownPropertyException_whenThrown_thenProblemDetailUsesJsonPath() {
        String content = "Not Readable Error";
        String body =
                """
                {
                  "nested": {
                    "unexpected": "value"
                  }
                }
                """;
        ObjectMapper objectMapper =
                new ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

        var cause =
                Assertions.assertThrows(
                        Exception.class,
                        () -> objectMapper.readValue(body, StrictNestedRequestDto.class));

        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException(content, cause, null);

        ResponseEntity<Object> problemDetail =
                exceptionHandler.handleHttpMessageNotReadable(exception, null, null, null);

        Assertions.assertEquals(HttpStatusCode.valueOf(400), problemDetail.getStatusCode());
        Assertions.assertNotNull(problemDetail.getBody());
        Assertions.assertTrue(problemDetail.getBody() instanceof ProblemDetail);

        Assertions.assertEquals(400, problemDetail.getStatusCode().value());
        Assertions.assertEquals(
                "Unsupported request field: nested.unexpected",
                ((ProblemDetail) problemDetail.getBody()).getDetail());
        Assertions.assertEquals(
                CommonAppError.NOT_READABLE_ERROR.getCode().getType().get(),
                ((ProblemDetail) problemDetail.getBody()).getType());
        Assertions.assertTrue(
                logCaptor.getWarnLogs().stream()
                        .anyMatch(
                                log ->
                                        log.contains(
                                                "[400]: Unsupported request field:"
                                                        + " nested.unexpected")));
        assertThat(logCaptor.getErrorLogs()).isEmpty();
    }

    @Test
    void givenMissingRequestParameter_whenHandled_thenWarnIsLoggedWithoutError() {
        MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("date", "LocalDate");

        ResponseEntity<Object> problemDetail =
                exceptionHandler.handleMissingServletRequestParameter(
                        exception, HEADERS, BAD_REQUEST, webRequest);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, problemDetail.getStatusCode());
        Assertions.assertTrue(
                logCaptor.getWarnLogs().stream()
                        .anyMatch(
                                log ->
                                        log.contains(
                                                "[400]: Required request parameter 'date' is"
                                                        + " missing")));
        assertThat(logCaptor.getErrorLogs()).isEmpty();
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
                        exception, HEADERS, BAD_REQUEST, webRequest);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, problemDetail.getStatusCode());
        Assertions.assertTrue(
                logCaptor.getWarnLogs().stream()
                        .anyMatch(
                                log ->
                                        log.contains(
                                                "[400]: Validation failed for handler method"
                                                        + " arguments: code=size must be between 0"
                                                        + " and 10")));
        assertThat(logCaptor.getErrorLogs()).isEmpty();
    }

    @Test
    void givenEmptyHandlerMethodValidationException_whenHandled_thenGenericWarnIsLogged() {
        MethodValidationResult validationResult = Mockito.mock(MethodValidationResult.class);
        Mockito.when(validationResult.getParameterValidationResults()).thenReturn(List.of());
        Mockito.when(validationResult.getCrossParameterValidationResults()).thenReturn(List.of());

        HandlerMethodValidationException exception =
                new HandlerMethodValidationException(validationResult);

        ResponseEntity<Object> problemDetail =
                exceptionHandler.handleHandlerMethodValidationException(
                        exception, HEADERS, BAD_REQUEST, webRequest);

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
    void givenMethodValidationException_whenHandled_thenWarnIsLoggedWithoutError()
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

        MethodValidationException exception =
                new MethodValidationException(
                        MethodValidationResult.create(
                                this, methodParameter.getMethod(), List.of(validationResult)));

        ResponseEntity<Object> problemDetail =
                exceptionHandler.handleMethodValidationException(
                        exception, HEADERS, HttpStatus.BAD_REQUEST, webRequest);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, problemDetail.getStatusCode());
        Assertions.assertTrue(
                logCaptor.getWarnLogs().stream()
                        .anyMatch(
                                log ->
                                        log.contains(
                                                "[400]: Validation failed for handler method"
                                                        + " arguments: code=size must be between 0"
                                                        + " and 10")));
        assertThat(logCaptor.getErrorLogs()).isEmpty();
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
        var method =
                AppRegExceptionHandler.class.getDeclaredMethod(
                        "resolveStatusCode", HttpStatusCode.class, ProblemDetail.class);
        method.setAccessible(true);

        var explicitStatus = HttpStatus.valueOf(422);
        var status =
                (int)
                        method.invoke(
                                exceptionHandler,
                                explicitStatus,
                                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST));

        Assertions.assertEquals(explicitStatus.value(), status);
    }

    @Test
    void
            givenAccessDeniedException_whenTheExceptionIsThrown_thenForbiddenProblemDetailIsReturned() {
        // setup
        AccessDeniedException exception = new AccessDeniedException("Forbidden");
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        // execute
        ResponseEntity<ProblemDetail> problemDetail =
                exceptionHandler.handleAccessDenied(exception, request);

        // assert
        Assertions.assertEquals(HttpStatusCode.valueOf(403), problemDetail.getStatusCode());
        Assertions.assertNotNull(problemDetail.getBody());
        Assertions.assertEquals(403, problemDetail.getBody().getStatus());
        Assertions.assertEquals("Access denied", problemDetail.getBody().getDetail());
        Mockito.verify(securityEndpointFailureLogger)
                .logFailure(request, 403, SecurityEndpointFailureLogger.ACCESS_DENIED);
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

    private static Path path(String value) {
        Path path = Mockito.mock(Path.class);
        Mockito.when(path.toString()).thenReturn(value);
        return path;
    }

    @SuppressWarnings("unused")
    private void sampleValidationMethod(String code) {
        // used to create a MethodParameter with a stable name for validation tests
    }

    private static class StrictRequestDto {
        private String dateFrom;

        public String getDateFrom() {
            return dateFrom;
        }

        public void setDateFrom(String dateFrom) {
            this.dateFrom = dateFrom;
        }
    }

    private static class StrictNestedRequestDto {
        private StrictNestedDto nested;

        public StrictNestedDto getNested() {
            return nested;
        }

        public void setNested(StrictNestedDto nested) {
            this.nested = nested;
        }
    }

    private static class StrictNestedDto {
        private String known;

        public String getKnown() {
            return known;
        }

        public void setKnown(String known) {
            this.known = known;
        }
    }
}
