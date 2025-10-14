package uk.gov.hmcts.appregister.common.validator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ValidationExceptionHandlerTest {
    @Test
    public void testWrapException() {
        ResponseStatusException ex =
                Assertions.assertThrows(
                        ResponseStatusException.class,
                        () ->
                                ValidationExceptionHandler.wrap(
                                        () -> {
                                            throw new IllegalArgumentException("test");
                                        }));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void testDoNotWrapOnSuccess() {
        Assertions.assertEquals("success", ValidationExceptionHandler.wrap(() -> "success"));
    }
}
