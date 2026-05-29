package uk.gov.hmcts.appregister.common.log;

import nl.altindag.log.LogCaptor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

public class ControllerLogAspectTest {

    private final LogCaptor controllerAspectLog = LogCaptor.forClass(ControllerLogAspect.class);

    @Test
    void logController() throws Throwable {
        controllerAspectLog.clearLogs();
        ControllerLogAspect controllerLogAspect = new ControllerLogAspect();
        Signature signature = Mockito.mock(Signature.class);

        ResponseEntity<String> responseEntity = ResponseEntity.ok("Test Result");
        responseEntity.getHeaders().add("Content-Type", "application/vnd.hmcts.appreg.v1+json");

        ProceedingJoinPoint customProceedingJoinPoint = Mockito.mock(ProceedingJoinPoint.class);
        Mockito.when(customProceedingJoinPoint.proceed()).thenReturn(responseEntity);
        Mockito.when(customProceedingJoinPoint.getArgs()).thenReturn(new Object[] {"arg1", "arg2"});
        Mockito.when(customProceedingJoinPoint.getSignature()).thenReturn(signature);

        Mockito.when(signature.getDeclaringType()).thenReturn(ControllerLogAspectTest.class);
        Mockito.when(signature.getName()).thenReturn("testMethod");

        // call the aspect method
        ResponseEntity<?> result =
                (ResponseEntity<?>) controllerLogAspect.logDuration(customProceedingJoinPoint);

        // assert the log messages are correct and the result is correct
        Assertions.assertEquals("Test Result", result.getBody());
        Assertions.assertEquals(
                "Start: Executing ControllerLogAspectTest.testMethod",
                controllerAspectLog.getDebugLogs().get(0));
        Assertions.assertTrue(
                controllerAspectLog
                        .getDebugLogs()
                        .get(1)
                        .startsWith("Finish: Executed ControllerLogAspectTest.testMethod in "));
        Assertions.assertTrue(controllerAspectLog.getDebugLogs().get(1).endsWith(" ms"));
        Mockito.verify(customProceedingJoinPoint, Mockito.times(1)).proceed();
    }

    @Test
    void logControllerNoResult() throws Throwable {
        controllerAspectLog.clearLogs();
        ControllerLogAspect controllerLogAspect = new ControllerLogAspect();
        Signature signature = Mockito.mock(Signature.class);

        ProceedingJoinPoint customProceedingJoinPoint = Mockito.mock(ProceedingJoinPoint.class);
        Mockito.when(customProceedingJoinPoint.proceed()).thenReturn(null);
        Mockito.when(customProceedingJoinPoint.getArgs()).thenReturn(new Object[] {"arg1", "arg2"});
        Mockito.when(customProceedingJoinPoint.getSignature()).thenReturn(signature);

        Mockito.when(signature.getDeclaringType()).thenReturn(ControllerLogAspectTest.class);
        Mockito.when(signature.getName()).thenReturn("testMethod");

        // call the aspect method
        String result = (String) controllerLogAspect.logDuration(customProceedingJoinPoint);

        // assert the log messages are correct and the result is correct
        Assertions.assertNull(result);
        Assertions.assertEquals(
                "Start: Executing ControllerLogAspectTest.testMethod",
                controllerAspectLog.getDebugLogs().get(0));
        Assertions.assertTrue(
                controllerAspectLog
                        .getDebugLogs()
                        .get(1)
                        .startsWith("Finish: Executed ControllerLogAspectTest.testMethod in "));
        Assertions.assertTrue(controllerAspectLog.getDebugLogs().get(1).endsWith(" ms"));
    }
}
