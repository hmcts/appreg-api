package uk.gov.hmcts.appregister.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import uk.gov.hmcts.appregister.applicationcode.exception.ApplicationCodeError;

class AppRegExceptionHandlerTest {
    private AppRegExceptionHandler exceptionHandler;

    @BeforeEach
    void beforeEach() {
        exceptionHandler = new AppRegExceptionHandler();
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
}
