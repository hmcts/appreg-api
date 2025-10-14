package uk.gov.hmcts.appregister.common.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class DefaultErrorDetailTest {

    @Test
    public void testDefaultErrorDetail() {
        DefaultErrorDetail errorDetail =
                new DefaultErrorDetail(HttpStatus.BAD_REQUEST, "An error occurred", "appCode");
        Assertions.assertEquals("appCode", errorDetail.getAppCode());
        Assertions.assertEquals("An error occurred", errorDetail.getMessage());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, errorDetail.getHttpCode());
    }
}
