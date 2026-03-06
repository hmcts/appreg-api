package uk.gov.hmcts.appregister.common.log;

import java.util.regex.Pattern;
import nl.altindag.log.LogCaptor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ServiceLogAspectTest {

    private final LogCaptor serviceAspectLog = LogCaptor.forClass(ServiceLogAspect.class);

    @Test
    void logService() throws Throwable {
        ServiceLogAspect serviceLogAspect = new ServiceLogAspect();
        Signature signature = Mockito.mock(Signature.class);

        ProceedingJoinPoint customProceedingJoinPoint = Mockito.mock(ProceedingJoinPoint.class);
        Mockito.when(customProceedingJoinPoint.proceed()).thenReturn("Test Result");
        Mockito.when(customProceedingJoinPoint.getArgs()).thenReturn(new Object[] {"arg1", "arg2"});
        Mockito.when(customProceedingJoinPoint.getSignature()).thenReturn(signature);

        Mockito.when(signature.getDeclaringType()).thenReturn((Class) ServiceLogAspectTest.class);
        Mockito.when(signature.getName()).thenReturn("testMethod");

        // call the aspect method
        String result = (String) serviceLogAspect.logDuration(customProceedingJoinPoint);

        // assert the log messages are correct and the result is correct
        Assertions.assertEquals("Test Result", result);

        Assertions.assertTrue(
                Pattern.matches(
                        ".*Start: Executing Mock for Signature, .* with arguments: \\[arg1, arg2\\].*",
                        serviceAspectLog.getDebugLogs().get(0)));
        Assertions.assertTrue(
                serviceAspectLog
                        .getDebugLogs()
                        .get(1)
                        .startsWith("Duration of ServiceLogAspectTest.testMethod"));
        Assertions.assertEquals(
                "Finish: Executed and returned \"Test Result\"",
                serviceAspectLog.getDebugLogs().get(2));

        Mockito.verify(customProceedingJoinPoint, Mockito.times(1)).proceed();
    }

    @Test
    void logServiceNoResult() throws Throwable {
        ServiceLogAspect serviceLogAspect = new ServiceLogAspect();
        Signature signature = Mockito.mock(Signature.class);

        ProceedingJoinPoint customProceedingJoinPoint = Mockito.mock(ProceedingJoinPoint.class);
        Mockito.when(customProceedingJoinPoint.proceed()).thenReturn(null);
        Mockito.when(customProceedingJoinPoint.getArgs()).thenReturn(new Object[] {"arg1", "arg2"});
        Mockito.when(customProceedingJoinPoint.getSignature()).thenReturn(signature);

        Mockito.when(signature.getDeclaringType()).thenReturn((Class) ServiceLogAspectTest.class);
        Mockito.when(signature.getName()).thenReturn("testMethod");

        // call the aspect method
        String result = (String) serviceLogAspect.logDuration(customProceedingJoinPoint);

        // assert the log messages are correct and the result is correct
        Assertions.assertNull(result);

        Assertions.assertTrue(
                Pattern.matches(
                        ".*Start: Executing Mock for Signature, .* with arguments: \\[arg1, arg2\\].*",
                        serviceAspectLog.getDebugLogs().get(0)));
        Assertions.assertTrue(
                serviceAspectLog
                        .getDebugLogs()
                        .get(1)
                        .startsWith("Duration of ServiceLogAspectTest.testMethod"));
        Assertions.assertEquals(
                "Finish: Executed and returned null", serviceAspectLog.getDebugLogs().get(2));

        Mockito.verify(customProceedingJoinPoint, Mockito.times(1)).proceed();
    }
}
