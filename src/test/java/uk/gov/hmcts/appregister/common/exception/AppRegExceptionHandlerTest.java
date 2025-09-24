package uk.gov.hmcts.appregister.common.exception;

import java.net.URI;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.appregister.applicationcode.exception.AppCodeError;

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
                new AppRegistryException(AppCodeError.CODE_NOT_FOUND, "Test message", null);

        // execute
        ResponseEntity<ProblemDetail> problemDetail =
                exceptionHandler.handleDartsApiException(exception);

        // assert
        Assertions.assertEquals(HttpStatusCode.valueOf(404), problemDetail.getStatusCode());
        Assertions.assertNotNull(problemDetail.getBody());
        Assertions.assertEquals(
                AppCodeError.CODE_NOT_FOUND.getCode().getHttpCode().value(),
                problemDetail.getBody().getStatus());
        Assertions.assertEquals(
                AppCodeError.CODE_NOT_FOUND.getCode().getMessage(),
                problemDetail.getBody().getDetail());
        Assertions.assertEquals(
                new URI(AppCodeError.CODE_NOT_FOUND.getCode().getAppCode()),
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
                        null);

        // execute
        ResponseEntity<ProblemDetail> problemDetail =
                exceptionHandler.handleDartsApiException(exception);

        // assert
        Assertions.assertEquals(HttpStatusCode.valueOf(400), problemDetail.getStatusCode());
        Assertions.assertNotNull(problemDetail.getBody());
        Assertions.assertEquals(400, problemDetail.getBody().getStatus());
        Assertions.assertEquals(customMessage, problemDetail.getBody().getDetail());
        Assertions.assertEquals(new URI(customType), problemDetail.getBody().getType());
    }
}
