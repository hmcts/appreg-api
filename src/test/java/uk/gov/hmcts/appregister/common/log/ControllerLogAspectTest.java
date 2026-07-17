package uk.gov.hmcts.appregister.common.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import nl.altindag.log.LogCaptor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.appregister.csds.ingress.database.CsdsBatchUpsertException;
import uk.gov.hmcts.appregister.csds.ingress.database.FailedUpsertRecord;

class ControllerLogAspectTest {

    private final LogCaptor controllerAspectLog = LogCaptor.forClass(ControllerLogAspect.class);
    private final LogCaptor abstractAspectLog =
            LogCaptor.forClass(AbstractOperationDurationAspect.class);

    @BeforeEach
    void beforeEach() {
        controllerAspectLog.clearLogs();
        controllerAspectLog.setLogLevelToDebug();
        abstractAspectLog.clearLogs();
    }

    @Test
    void logController() throws Throwable {
        controllerAspectLog.clearLogs();
        ControllerLogAspect controllerLogAspect = new ControllerLogAspect();
        Signature signature = mock(Signature.class);

        ResponseEntity<String> responseEntity = ResponseEntity.ok("Test Result");
        responseEntity.getHeaders().add("Content-Type", "application/vnd.hmcts.appreg.v1+json");

        ProceedingJoinPoint customProceedingJoinPoint = mock(ProceedingJoinPoint.class);
        when(customProceedingJoinPoint.proceed()).thenReturn(responseEntity);
        when(customProceedingJoinPoint.getArgs()).thenReturn(new Object[] {"arg1", "arg2"});
        when(customProceedingJoinPoint.getSignature()).thenReturn(signature);

        when(signature.getDeclaringType()).thenReturn(ControllerLogAspectTest.class);
        when(signature.getName()).thenReturn("testMethod");

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
        assertThat(controllerAspectLog.getDebugLogs().get(1)).endsWith(" ms");
        verify(customProceedingJoinPoint).proceed();
    }

    @Test
    void logControllerNoResult() throws Throwable {
        controllerAspectLog.clearLogs();
        ControllerLogAspect controllerLogAspect = new ControllerLogAspect();
        Signature signature = mock(Signature.class);

        ProceedingJoinPoint customProceedingJoinPoint = mock(ProceedingJoinPoint.class);
        when(customProceedingJoinPoint.proceed()).thenReturn(null);
        when(customProceedingJoinPoint.getArgs()).thenReturn(new Object[] {"arg1", "arg2"});
        when(customProceedingJoinPoint.getSignature()).thenReturn(signature);

        when(signature.getDeclaringType()).thenReturn(ControllerLogAspectTest.class);
        when(signature.getName()).thenReturn("testMethod");

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
        assertThat(controllerAspectLog.getDebugLogs().get(1)).endsWith(" ms");
    }

    @Test
    void logControllerJsonResponseWithContentTypeParameters() throws Throwable {
        ControllerLogAspect controllerLogAspect = new ControllerLogAspect();
        Signature signature = mock(Signature.class);

        ResponseEntity<String> responseEntity =
                ResponseEntity.ok()
                        .header(
                                HttpHeaders.CONTENT_TYPE,
                                "application/vnd.hmcts.appreg.v1+json;charset=UTF-8")
                        .body("Test Result");

        ProceedingJoinPoint customProceedingJoinPoint = mock(ProceedingJoinPoint.class);
        when(customProceedingJoinPoint.proceed()).thenReturn(responseEntity);
        when(customProceedingJoinPoint.getArgs()).thenReturn(new Object[] {"arg1", "arg2"});
        when(customProceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn(ControllerLogAspectTest.class);
        when(signature.getName()).thenReturn("testMethod");

        ResponseEntity<?> result =
                (ResponseEntity<?>) controllerLogAspect.logDuration(customProceedingJoinPoint);

        Assertions.assertEquals("Test Result", result.getBody());
        Assertions.assertTrue(
                controllerAspectLog
                        .getDebugLogs()
                        .get(1)
                        .startsWith("Finish: Executed ControllerLogAspectTest.testMethod in "));
        assertThat(controllerAspectLog.getDebugLogs().get(1)).endsWith(" ms");
    }

    @Test
    void logControllerExpectedValidationExceptionWithoutErrorStackTrace() throws Throwable {
        ControllerLogAspect controllerLogAspect = new ControllerLogAspect();
        Signature signature = mock(Signature.class);

        ProceedingJoinPoint customProceedingJoinPoint = mock(ProceedingJoinPoint.class);
        ConstraintViolationException exception =
                new ConstraintViolationException("validation failed", Set.of());
        when(customProceedingJoinPoint.proceed()).thenThrow(exception);
        when(customProceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn(ControllerLogAspectTest.class);
        when(signature.getName()).thenReturn("testMethod");

        ConstraintViolationException thrown =
                Assertions.assertThrows(
                        ConstraintViolationException.class,
                        () -> controllerLogAspect.logDuration(customProceedingJoinPoint));

        Assertions.assertSame(exception, thrown);
        assertThat(abstractAspectLog.getErrorLogs()).isEmpty();
    }

    @Test
    void logControllerWhenDebugDisabledDoesNotLogDebugMessages() throws Throwable {
        controllerAspectLog.setLogLevelToInfo();

        ControllerLogAspect controllerLogAspect = new ControllerLogAspect();
        Signature signature = mock(Signature.class);

        ResponseEntity<String> responseEntity = ResponseEntity.ok("Test Result");
        responseEntity.getHeaders().add("Content-Type", "application/vnd.hmcts.appreg.v1+json");

        ProceedingJoinPoint customProceedingJoinPoint = mock(ProceedingJoinPoint.class);
        when(customProceedingJoinPoint.proceed()).thenReturn(responseEntity);
        when(customProceedingJoinPoint.getArgs()).thenReturn(new Object[] {"arg1", "arg2"});
        when(customProceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn(ControllerLogAspectTest.class);
        when(signature.getName()).thenReturn("testMethod");

        ResponseEntity<?> result =
                (ResponseEntity<?>) controllerLogAspect.logDuration(customProceedingJoinPoint);

        Assertions.assertEquals("Test Result", result.getBody());
        assertThat(controllerAspectLog.getDebugLogs()).isEmpty();
    }

    @Test
    void logControllerUnexpectedExceptionStillLogsError() throws Throwable {
        ControllerLogAspect controllerLogAspect = new ControllerLogAspect();
        Signature signature = mock(Signature.class);

        ProceedingJoinPoint customProceedingJoinPoint = mock(ProceedingJoinPoint.class);
        RuntimeException exception = new RuntimeException("boom");
        when(customProceedingJoinPoint.proceed()).thenThrow(exception);
        when(customProceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn(ControllerLogAspectTest.class);
        when(signature.getName()).thenReturn("testMethod");

        RuntimeException thrown =
                Assertions.assertThrows(
                        RuntimeException.class,
                        () -> controllerLogAspect.logDuration(customProceedingJoinPoint));

        Assertions.assertSame(exception, thrown);
        Assertions.assertEquals(
                "Exception occurred during execution", abstractAspectLog.getErrorLogs().getFirst());
    }

    @Test
    void logControllerCsdsBatchUpsertExceptionDoesNotLogAspectError() throws Throwable {
        ControllerLogAspect controllerLogAspect = new ControllerLogAspect();
        Signature signature = mock(Signature.class);

        ProceedingJoinPoint customProceedingJoinPoint = mock(ProceedingJoinPoint.class);
        var exception =
                new CsdsBatchUpsertException(
                        "CSDS batch upsert failed for application_codes_staging.ac_id",
                        new RuntimeException("batch failed"),
                        List.of(
                                new FailedUpsertRecord<>(
                                        3L,
                                        "ERROR: value too long for type character varying(10)")));
        when(customProceedingJoinPoint.proceed()).thenThrow(exception);
        when(customProceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn(ControllerLogAspectTest.class);
        when(signature.getName()).thenReturn("testMethod");

        var thrown =
                Assertions.assertThrows(
                        CsdsBatchUpsertException.class,
                        () -> controllerLogAspect.logDuration(customProceedingJoinPoint));

        Assertions.assertSame(exception, thrown);
        assertThat(abstractAspectLog.getErrorLogs()).isEmpty();
    }
}
