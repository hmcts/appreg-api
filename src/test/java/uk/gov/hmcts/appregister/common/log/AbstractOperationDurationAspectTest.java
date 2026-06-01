package uk.gov.hmcts.appregister.common.log;

import jakarta.validation.ConstraintViolationException;
import java.lang.reflect.Method;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

class AbstractOperationDurationAspectTest {

    private final AbstractOperationDurationAspect aspect = new AbstractOperationDurationAspect();

    @Test
    void givenExistingMdcOperation_whenOperationCompletes_thenPreviousOperationIsRestored()
            throws Throwable {
        MDC.put(AbstractOperationDurationAspect.OPERATION, "Controller.method");

        try {
            var pjp = joinPoint("method", "result");

            var result =
                    aspect.invokeOperationMDC(
                            Assertions::assertNotNull,
                            (op, duration, returned) -> Assertions.assertEquals("result", returned),
                            pjp);

            Assertions.assertEquals("result", result);
            Assertions.assertEquals(
                    "Controller.method", MDC.get(AbstractOperationDurationAspect.OPERATION));
        } finally {
            MDC.remove(AbstractOperationDurationAspect.OPERATION);
        }
    }

    @Test
    void givenNoMdcOperation_whenOperationCompletes_thenOperationIsRemoved() throws Throwable {
        MDC.remove(AbstractOperationDurationAspect.OPERATION);

        var pjp = joinPoint("method", "result");

        var result =
                aspect.invokeOperationMDC(
                        Assertions::assertNotNull,
                        (op, duration, returned) -> Assertions.assertEquals("result", returned),
                        pjp);

        Assertions.assertEquals("result", result);
        Assertions.assertNull(MDC.get(AbstractOperationDurationAspect.OPERATION));
    }

    @Test
    void givenExpectedRequestValidationExceptions_whenChecked_thenTrueIsReturned()
            throws Exception {
        Method method =
                AbstractOperationDurationAspect.class.getDeclaredMethod(
                        "isExpectedRequestValidationException", Throwable.class);
        method.setAccessible(true);

        MethodValidationResult validationResult = Mockito.mock(MethodValidationResult.class);
        Mockito.when(validationResult.getParameterValidationResults()).thenReturn(List.of());
        Mockito.when(validationResult.getCrossParameterValidationResults()).thenReturn(List.of());

        Assertions.assertTrue(
                (boolean)
                        method.invoke(
                                aspect,
                                new ConstraintViolationException("validation failed", null)));
        Assertions.assertTrue(
                (boolean)
                        method.invoke(
                                aspect,
                                new MethodArgumentTypeMismatchException(
                                        "01-01-2026", null, "date", null, null)));
        Assertions.assertTrue(
                (boolean)
                        method.invoke(
                                aspect,
                                new MissingServletRequestParameterException("date", "LocalDate")));
        Assertions.assertTrue(
                (boolean)
                        method.invoke(
                                aspect,
                                new HttpMessageNotReadableException(
                                        "payload invalid", (HttpInputMessage) null)));
        Assertions.assertTrue(
                (boolean)
                        method.invoke(
                                aspect, new HandlerMethodValidationException(validationResult)));
        Assertions.assertTrue(
                (boolean) method.invoke(aspect, new MethodValidationException(validationResult)));
        Assertions.assertTrue(
                (boolean)
                        method.invoke(
                                aspect,
                                new AppRegistryException(
                                        CommonAppError.SORT_NOT_SUITABLE, "bad sort")));
    }

    @Test
    void givenUnexpectedException_whenChecked_thenFalseIsReturned() throws Exception {
        Method method =
                AbstractOperationDurationAspect.class.getDeclaredMethod(
                        "isExpectedRequestValidationException", Throwable.class);
        method.setAccessible(true);

        Assertions.assertFalse((boolean) method.invoke(aspect, new RuntimeException("boom")));
        Assertions.assertFalse(
                (boolean)
                        method.invoke(
                                aspect,
                                new AppRegistryException(
                                        CommonAppError.INTERNAL_SERVER_ERROR, "server failure")));
    }

    private ProceedingJoinPoint joinPoint(String methodName, Object result) throws Throwable {
        var pjp = Mockito.mock(ProceedingJoinPoint.class);
        var signature = Mockito.mock(Signature.class);

        Mockito.when(signature.getDeclaringType())
                .thenReturn(AbstractOperationDurationAspectTest.class);
        Mockito.when(signature.getName()).thenReturn(methodName);
        Mockito.when(pjp.getSignature()).thenReturn(signature);
        Mockito.when(pjp.proceed()).thenReturn(result);

        return pjp;
    }
}
