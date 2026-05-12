package uk.gov.hmcts.appregister.testutils.util;

import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

/**
 * A problem details class that allows to assert around problem details.
 */
public class ProblemAssertUtil {

    private static String normalizeLineEndings(String value) {
        return value == null ? null : value.replace("\r\n", "\n");
    }

    /**
     * Asserts an expected problem details response on an actual response.
     * actualResponseSpecification the actual response
     *
     * @param expectedErrorDetail The expected detail
     * @param expectedMessage The expected detail message. If null, uses the message from
     *     expectedErrorDetail
     * @param actualResponse The rest assured response
     */
    public static void assertEquals(
            ErrorDetail expectedErrorDetail, String expectedMessage, Response actualResponse) {
        ProblemDetail problemDetail = actualResponse.as(ProblemDetail.class);
        Assertions.assertEquals(
                expectedErrorDetail.getAppCode(), problemDetail.getType().toString());
        Assertions.assertEquals(expectedErrorDetail.getMessage(), problemDetail.getTitle());
        Assertions.assertEquals(
                expectedErrorDetail.getHttpCode().value(), problemDetail.getStatus());

        if (expectedMessage == null) {
            Assertions.assertEquals(
                    normalizeLineEndings(expectedErrorDetail.getMessage()),
                    normalizeLineEndings(problemDetail.getDetail()));
        } else {
            Assertions.assertEquals(
                    normalizeLineEndings(expectedMessage),
                    normalizeLineEndings(problemDetail.getDetail()));
        }
    }

    /**
     * Asserts an expected problem details response on an actual response.
     * actualResponseSpecification the actual response
     *
     * @param expectedErrorDetail The expected detail
     * @param actualResponse The rest assured response
     */
    public static void assertEquals(ErrorDetail expectedErrorDetail, Response actualResponse) {
        assertEquals(expectedErrorDetail, null, actualResponse);
    }
}
