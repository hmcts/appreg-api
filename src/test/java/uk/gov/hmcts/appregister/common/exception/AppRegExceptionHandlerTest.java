package uk.gov.hmcts.appregister.common.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.NativeWebRequest;
import uk.gov.hmcts.appregister.applicationcode.exception.AppCodeError;

import java.awt.image.ImagingOpException;
import java.net.URI;

public class AppRegExceptionHandlerTest {
    private AppRegExceptionHandler exceptionHandler;

    @BeforeEach
    public void beforeEach() {
        exceptionHandler = new AppRegExceptionHandler();
    }

    @Test
    public void givenAnAppRegisterExceptionWithoutAppCode_whenTheExceptionIsThrown_thenAProblemDetailIsaReturned()
    throws Exception{
        // setup
        AppRegistryException exception = new AppRegistryException(AppCodeError.CODE_NOT_FOUND, "Test message", null);

        // execute
        ResponseEntity<ProblemDetail> problemDetail = exceptionHandler.handleDartsApiException(exception);

        // assert
        Assertions.assertEquals(HttpStatusCode.valueOf(400), problemDetail.getStatusCode());
        Assertions.assertNotNull(problemDetail.getBody());
        Assertions.assertEquals(AppCodeError.CODE_NOT_FOUND.getCode().getHttpCode().value(),
                                problemDetail.getBody().getStatus());
        Assertions.assertEquals(AppCodeError.CODE_NOT_FOUND.getCode().getMessage(), problemDetail.getBody().getDetail());
        Assertions.assertEquals(new URI("about:blank"),
                                problemDetail.getBody().getType());
    }

    @Test
    public void givenAnAppRegisterExceptionWithAppCode_whenTheExceptionIsThrown_thenAProblemDetailIsaReturned()
        throws Exception {
        String customMessage = "Custom message";
        String customType = "CustomType";

        // setup
        AppRegistryException exception = new AppRegistryException(
            new ErrorCodeEnum() {
                @Override
                public ErrorCode getCode() {
                    return new DefaultErrorCode(HttpStatus.BAD_REQUEST, customMessage, customType);
                }
            }, "Test message", null
        );

        // execute
        ResponseEntity<ProblemDetail> problemDetail = exceptionHandler.handleDartsApiException(exception);

        // assert
        Assertions.assertEquals(HttpStatusCode.valueOf(400), problemDetail.getStatusCode());
        Assertions.assertNotNull(problemDetail.getBody());
        Assertions.assertEquals(400,
                                problemDetail.getBody().getStatus());
        Assertions.assertEquals(customMessage, problemDetail.getBody().getDetail());
        Assertions.assertEquals(new URI(customType),
                                problemDetail.getBody().getType());
    }

    @Test
    public void givenAnException_whenTheExceptionIsThrown_thenAProblemDetailIsaReturned() throws Exception {
        // execute
        ResponseEntity<ProblemDetail> problemDetail = exceptionHandler.exception(new IllegalAccessException());

        // assert
        Assertions.assertEquals(HttpStatusCode.valueOf(500), problemDetail.getStatusCode());
        Assertions.assertNotNull(problemDetail.getBody());
        Assertions.assertEquals(500,
                                problemDetail.getBody().getStatus());
        Assertions.assertEquals("A runtime exception occurred", problemDetail.getBody().getDetail());
        Assertions.assertEquals(new URI("about:blank"), problemDetail.getBody().getType());
    }
}
