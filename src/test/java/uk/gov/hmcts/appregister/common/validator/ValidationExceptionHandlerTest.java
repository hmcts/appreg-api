package uk.gov.hmcts.appregister.common.validator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ValidationExceptionHandlerTest {
    @Test
    void testWrapException() {
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
    void testDoNotWrapOnSuccess() {
        Assertions.assertEquals("success", ValidationExceptionHandler.wrap(() -> "success"));
    }
}
