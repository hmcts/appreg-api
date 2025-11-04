package uk.gov.hmcts.appregister.testutils.util;

import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

/**
 * A problem details class that allows to assert around problem details.
 */
public class ProblemAssertUtil {

    /**
     * Asserts an expected problem details response on an actual response.
     * actualResponseSpecification the actual response
     *
     * @param expectedErrorDetail The expected detail
     * @param actualResponse The rest assured response
     */
    public static void assertEquals(ErrorDetail expectedErrorDetail, Response actualResponse) {
        ProblemDetail problemDetail = actualResponse.as(ProblemDetail.class);
        Assertions.assertEquals(
                expectedErrorDetail.getAppCode(), problemDetail.getType().toString());
        Assertions.assertEquals(expectedErrorDetail.getMessage(), problemDetail.getTitle());
        Assertions.assertEquals(
                expectedErrorDetail.getHttpCode().value(), problemDetail.getStatus());
        Assertions.assertEquals(expectedErrorDetail.getMessage(), problemDetail.getDetail());
    }
}
