package uk.gov.hmcts.appregister.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import uk.gov.hmcts.appregister.common.log.SecurityEndpointFailureLogger;

class AppRegExceptionHandlerIntegrationTest {

    @Test
    void givenSpringMethodValidationExceptions_whenHandled_thenSafeSummaryIsLogged()
            throws NoSuchMethodException {
        var exceptionHandler =
                new AppRegExceptionHandler(Mockito.mock(SecurityEndpointFailureLogger.class));
        var logCaptor = LogCaptor.forClass(AppRegExceptionHandler.class);
        logCaptor.clearLogs();

        Method method =
                AppRegExceptionHandlerIntegrationTest.class.getDeclaredMethod(
                        "sampleValidationMethod", String.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        ParameterValidationResult parameterValidationResult =
                new ParameterValidationResult(
                        methodParameter,
                        "SensitiveMarker123",
                        List.of(
                                new DefaultMessageSourceResolvable(
                                        "size must be between 0 and 10")),
                        null,
                        null,
                        null,
                        (error, sourceType) -> null);
        MethodValidationResult validationResult =
                MethodValidationResult.create(this, method, List.of(parameterValidationResult));

        var handlerResponse =
                exceptionHandler.handleHandlerMethodValidationException(
                        new HandlerMethodValidationException(validationResult),
                        HttpHeaders.EMPTY,
                        HttpStatus.BAD_REQUEST,
                        null);
        var methodResponse =
                exceptionHandler.handleMethodValidationException(
                        new MethodValidationException(validationResult),
                        HttpHeaders.EMPTY,
                        HttpStatus.BAD_REQUEST,
                        null);

        assertThat(handlerResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(methodResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(logCaptor.getWarnLogs())
                .filteredOn(
                        log ->
                                log.contains(
                                        "[400]: Validation failed for handler method arguments:"
                                                + " code=size must be between 0 and 10"))
                .hasSize(2);
        assertThat(logCaptor.getLogs()).noneMatch(log -> log.contains("SensitiveMarker123"));
    }

    @SuppressWarnings("unused")
    private String sampleValidationMethod(String code) {
        return code;
    }
}
