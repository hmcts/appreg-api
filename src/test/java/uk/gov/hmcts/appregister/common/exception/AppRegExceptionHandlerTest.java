package uk.gov.hmcts.appregister.common.exception;

import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
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

        // setup
        ConstraintViolationException exception =
                new ConstraintViolationException(customMessage, null) {
                    ;
                    @Override
                    public String getMessage() {
                        return customMessage;
                    }
                };

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
                List.of(new FieldError("objectName", "field", "defaultMessage"));
        // setup
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(null, result) {
                    @Override
                    public String getMessage() {
                        return customMessage;
                    }

                    @Override
                    public List<FieldError> getFieldErrors() {
                        return fieldErrors;
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
                "Custom message. field=defaultMessage\n",
                ((ProblemDetail) problemDetail.getBody()).getDetail());
        Assertions.assertEquals(
                CommonAppError.METHOD_ARGUMENT_INVALID_ERROR.getCode().getType().get(),
                ((ProblemDetail) problemDetail.getBody()).getType());
    }

    @Test
    void
            givenHandleMethodValidationExceptionWithAppCode_whenTheExceptionIsThrown_thenAProblemDetailIsaReturned()
                    throws Exception {

        MethodValidationResult result = Mockito.mock(MethodValidationResult.class);

        // setup
        HandlerMethodValidationException exception = new HandlerMethodValidationException(result);

        // execute
        ResponseEntity<Object> problemDetail =
                exceptionHandler.handleHandlerMethodValidationException(
                        exception, null, null, null);

        // assert
        Assertions.assertEquals(HttpStatusCode.valueOf(400), problemDetail.getStatusCode());
        Assertions.assertNotNull(problemDetail.getBody());
        Assertions.assertTrue(problemDetail.getBody() instanceof ProblemDetail);
        Assertions.assertEquals(400, ((ProblemDetail) problemDetail.getBody()).getStatus());
        Assertions.assertEquals(
                CommonAppError.METHOD_VALIDATION_INVALID_ERROR.getCode().getType().get(),
                ((ProblemDetail) problemDetail.getBody()).getType());
    }

    @Test
    void
            givenHttpMessageNotReadableExceptionWithAppCode_whenTheExceptionIsThrown_thenAProblemDetailIsaReturned()
                    throws Exception {

        String content = "test";

        // setup
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(content);

        // execute
        ResponseEntity<Object> problemDetail =
                exceptionHandler.handleHttpMessageNotReadable(exception, null, null, null);

        // assert
        Assertions.assertEquals(HttpStatusCode.valueOf(400), problemDetail.getStatusCode());
        Assertions.assertNotNull(problemDetail.getBody());
        Assertions.assertTrue(problemDetail.getBody() instanceof ProblemDetail);

        Assertions.assertEquals(400, ((ProblemDetail) problemDetail.getBody()).getStatus());
        Assertions.assertEquals(
                content, ((ProblemDetail) (ProblemDetail) problemDetail.getBody()).getDetail());
        Assertions.assertEquals(
                CommonAppError.NOT_READABLE_ERROR.getCode().getType().get(),
                ((ProblemDetail) problemDetail.getBody()).getType());
    }

    @Test
    void
            givenHttpMessageNotReadableDateExceptionWithAppCode_whenTheExceptionIsThrown_thenAProblemDetailIsaReturned()
                    throws Exception {

        String content = "test";
        String dateExContent = "date ex";

        DateTimeParseException dateTimeParseException =
                new DateTimeParseException(dateExContent, "parsedString", 0);

        // setup
        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException(content, dateTimeParseException);

        // execute
        ResponseEntity<Object> problemDetail =
                exceptionHandler.handleHttpMessageNotReadable(exception, null, null, null);

        // assert
        Assertions.assertEquals(HttpStatusCode.valueOf(400), problemDetail.getStatusCode());
        Assertions.assertNotNull(problemDetail.getBody());
        Assertions.assertTrue(problemDetail.getBody() instanceof ProblemDetail);

        Assertions.assertEquals(400, ((ProblemDetail) problemDetail.getBody()).getStatus());
        Assertions.assertEquals(
                dateExContent,
                ((ProblemDetail) (ProblemDetail) problemDetail.getBody()).getDetail());
        Assertions.assertEquals(
                CommonAppError.NOT_READABLE_ERROR.getCode().getType().get(),
                ((ProblemDetail) problemDetail.getBody()).getType());
    }
}
