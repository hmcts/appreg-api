package uk.gov.hmcts.appregister.csds.ingress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.lock.DistributedJobLock;
import uk.gov.hmcts.appregister.common.lock.DistributedJobLockService;
import uk.gov.hmcts.appregister.csds.ingress.processor.IDataIngressProcessor;

@ExtendWith(MockitoExtension.class)
class CsdsIngressProcessorTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock private CsdsIngressClient ingressClient;
    @Mock private CsdsExecutionLogService csdsExecutionLogService;
    @Mock private DistributedJobLockService distributedJobLockService;
    @Mock private IDataIngressProcessor<String> dataIngressProcessor;

    @Test
    void given_registeredProcessors_when_runIngress_then_executesAllProcessorsUnderLock() {
        var properties = new CsdsIngressProperties();
        properties.setLeaseDuration(Duration.ofMinutes(3));
        var logCaptor = LogCaptor.forClass(CsdsIngressProcessor.class);
        logCaptor.clearLogs();

        var processor =
                new CsdsIngressProcessor(
                        properties,
                        ingressClient,
                        csdsExecutionLogService,
                        distributedJobLockService,
                        List.of(dataIngressProcessor));

        List<JsonNode> rawJson = List.of(OBJECT_MAPPER.createObjectNode().put("code", "alpha"));
        var lock =
                new DistributedJobLock(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, "token", Duration.ofMinutes(3));

        when(distributedJobLockService.tryAcquire(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, Duration.ofMinutes(3)))
                .thenReturn(Optional.of(lock));
        when(distributedJobLockService.release(lock)).thenReturn(true);
        when(dataIngressProcessor.enabled()).thenReturn(true);
        when(dataIngressProcessor.datasetName()).thenReturn("test-dataset");
        when(dataIngressProcessor.retrieve(ingressClient)).thenReturn(rawJson);
        when(dataIngressProcessor.preProcess(rawJson)).thenReturn("processed");

        var executed = processor.runIngress();

        assertThat(executed).isTrue();
        var inOrder = inOrder(distributedJobLockService, dataIngressProcessor, ingressClient);
        inOrder.verify(dataIngressProcessor).retrieve(ingressClient);
        inOrder.verify(dataIngressProcessor).backup();
        inOrder.verify(dataIngressProcessor).preProcess(rawJson);
        inOrder.verify(dataIngressProcessor).apply("processed");
        verify(distributedJobLockService).release(lock);
        assertThat(logCaptor.getInfoLogs())
                .anyMatch(log -> log.contains("Starting CSDS ingress processor test-dataset"))
                .anyMatch(
                        log ->
                                log.contains("Completed CSDS ingress processor test-dataset")
                                        && log.contains(" in ")
                                        && log.contains(" ms"));
    }

    @Test
    void given_lockUnavailable_when_runIngress_then_skipsProcessorExecution() {
        var properties = new CsdsIngressProperties();
        properties.setLeaseDuration(Duration.ofMinutes(3));

        var processor =
                new CsdsIngressProcessor(
                        properties,
                        ingressClient,
                        csdsExecutionLogService,
                        distributedJobLockService,
                        List.of(dataIngressProcessor));

        when(distributedJobLockService.tryAcquire(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, Duration.ofMinutes(3)))
                .thenReturn(Optional.empty());

        var executed = processor.runIngress();

        assertThat(executed).isFalse();
        verifyNoInteractions(dataIngressProcessor);
    }

    @Test
    void given_lockUnavailable_when_runScheduledIngress_then_returnsSkippedResult() {
        var properties = new CsdsIngressProperties();
        properties.setLeaseDuration(Duration.ofMinutes(3));

        var processor =
                new CsdsIngressProcessor(
                        properties,
                        ingressClient,
                        csdsExecutionLogService,
                        distributedJobLockService,
                        List.of(dataIngressProcessor));

        when(distributedJobLockService.tryAcquire(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, Duration.ofMinutes(3)))
                .thenReturn(Optional.empty());

        var result = processor.runScheduledIngress(LocalDateTime.parse("2026-07-27T03:05:00"));

        assertThat(result.status())
                .isEqualTo(CsdsIngressProcessor.ScheduledRunStatus.SKIPPED_LOCK_UNAVAILABLE);
        assertThat(result.message()).isNull();
        verifyNoInteractions(dataIngressProcessor);
    }

    @Test
    void given_processorDisabled_when_runIngress_then_skipsProcessorExecution() {
        var properties = new CsdsIngressProperties();
        properties.setLeaseDuration(Duration.ofMinutes(3));
        var logCaptor = LogCaptor.forClass(CsdsIngressProcessor.class);
        logCaptor.clearLogs();

        var processor =
                new CsdsIngressProcessor(
                        properties,
                        ingressClient,
                        csdsExecutionLogService,
                        distributedJobLockService,
                        List.of(dataIngressProcessor));
        var lock =
                new DistributedJobLock(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, "token", Duration.ofMinutes(3));

        when(distributedJobLockService.tryAcquire(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, Duration.ofMinutes(3)))
                .thenReturn(Optional.of(lock));
        when(distributedJobLockService.release(lock)).thenReturn(true);
        when(dataIngressProcessor.enabled()).thenReturn(false);
        when(dataIngressProcessor.datasetName()).thenReturn("test-dataset");
        when(dataIngressProcessor.targetTable()).thenReturn("test_table");
        when(dataIngressProcessor.targetKeyField()).thenReturn("test_id");

        var executed = processor.runIngress();

        assertThat(executed).isTrue();
        verify(distributedJobLockService).release(lock);
        verifyNoMoreInteractions(ingressClient);
        verify(dataIngressProcessor).enabled();
        assertThat(logCaptor.getInfoLogs())
                .anyMatch(
                        log ->
                                log.contains(
                                        "Skipping disabled CSDS ingress processor test-dataset"));
    }

    @Test
    void given_multipleProcessors_when_runIngress_then_renewsLeaseBetweenProcessors() {
        var properties = new CsdsIngressProperties();
        properties.setLeaseDuration(Duration.ofMinutes(3));
        @SuppressWarnings("unchecked")
        IDataIngressProcessor<String> secondProcessor = mock(IDataIngressProcessor.class);

        var processor =
                new CsdsIngressProcessor(
                        properties,
                        ingressClient,
                        csdsExecutionLogService,
                        distributedJobLockService,
                        List.of(dataIngressProcessor, secondProcessor));

        List<JsonNode> firstRawJson =
                List.of(OBJECT_MAPPER.createObjectNode().put("code", "alpha"));
        List<JsonNode> secondRawJson =
                List.of(OBJECT_MAPPER.createObjectNode().put("code", "beta"));
        var lock =
                new DistributedJobLock(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, "token", Duration.ofMinutes(3));

        when(distributedJobLockService.tryAcquire(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, Duration.ofMinutes(3)))
                .thenReturn(Optional.of(lock));
        when(distributedJobLockService.renew(lock)).thenReturn(true);
        when(distributedJobLockService.release(lock)).thenReturn(true);
        when(dataIngressProcessor.enabled()).thenReturn(true);
        when(dataIngressProcessor.datasetName()).thenReturn("test-dataset-1");
        when(dataIngressProcessor.retrieve(ingressClient)).thenReturn(firstRawJson);
        when(dataIngressProcessor.preProcess(firstRawJson)).thenReturn("processed-1");
        when(secondProcessor.enabled()).thenReturn(true);
        when(secondProcessor.datasetName()).thenReturn("test-dataset-2");
        when(secondProcessor.retrieve(ingressClient)).thenReturn(secondRawJson);
        when(secondProcessor.preProcess(secondRawJson)).thenReturn("processed-2");

        var executed = processor.runIngress();

        assertThat(executed).isTrue();
        verify(distributedJobLockService).renew(lock);
        verify(distributedJobLockService).release(lock);
        verify(secondProcessor).apply("processed-2");
    }

    @Test
    void given_processorFails_when_runIngress_then_continueWithNextProcessor() {
        var properties = new CsdsIngressProperties();
        properties.setLeaseDuration(Duration.ofMinutes(3));
        @SuppressWarnings("unchecked")
        IDataIngressProcessor<String> secondProcessor = mock(IDataIngressProcessor.class);
        var logCaptor = LogCaptor.forClass(CsdsIngressProcessor.class);
        logCaptor.clearLogs();

        var processor =
                new CsdsIngressProcessor(
                        properties,
                        ingressClient,
                        csdsExecutionLogService,
                        distributedJobLockService,
                        List.of(dataIngressProcessor, secondProcessor));

        List<JsonNode> firstRawJson =
                List.of(OBJECT_MAPPER.createObjectNode().put("code", "alpha"));
        List<JsonNode> secondRawJson =
                List.of(OBJECT_MAPPER.createObjectNode().put("code", "beta"));
        var lock =
                new DistributedJobLock(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, "token", Duration.ofMinutes(3));

        when(distributedJobLockService.tryAcquire(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, Duration.ofMinutes(3)))
                .thenReturn(Optional.of(lock));
        when(distributedJobLockService.renew(lock)).thenReturn(true);
        when(distributedJobLockService.release(lock)).thenReturn(true);
        when(dataIngressProcessor.enabled()).thenReturn(true);
        when(dataIngressProcessor.datasetName()).thenReturn("test-dataset-1");
        when(dataIngressProcessor.retrieve(ingressClient)).thenReturn(firstRawJson);
        when(dataIngressProcessor.preProcess(firstRawJson)).thenReturn("processed-1");
        doThrow(new IllegalStateException("boom")).when(dataIngressProcessor).apply("processed-1");
        when(secondProcessor.enabled()).thenReturn(true);
        when(secondProcessor.datasetName()).thenReturn("test-dataset-2");
        when(secondProcessor.retrieve(ingressClient)).thenReturn(secondRawJson);
        when(secondProcessor.preProcess(secondRawJson)).thenReturn("processed-2");

        var executed = processor.runIngress();

        assertThat(executed).isTrue();
        verify(distributedJobLockService).renew(lock);
        verify(distributedJobLockService).release(lock);
        verify(secondProcessor).apply("processed-2");
        assertThat(logCaptor.getErrorLogs())
                .anyMatch(
                        log ->
                                log.contains("Skipping CSDS ingress processor test-dataset-1")
                                        && log.contains("after failure: boom"));
    }

    @Test
    void given_processorFails_when_runScheduledIngress_then_returnsFailedResult() {
        var properties = new CsdsIngressProperties();
        properties.setLeaseDuration(Duration.ofMinutes(3));
        List<JsonNode> rawJson = List.of(OBJECT_MAPPER.createObjectNode().put("code", "alpha"));
        var lock =
                new DistributedJobLock(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, "token", Duration.ofMinutes(3));

        when(distributedJobLockService.tryAcquire(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, Duration.ofMinutes(3)))
                .thenReturn(Optional.of(lock));
        when(distributedJobLockService.release(lock)).thenReturn(true);
        when(dataIngressProcessor.enabled()).thenReturn(true);
        when(dataIngressProcessor.datasetName()).thenReturn("test-dataset");
        when(dataIngressProcessor.retrieve(ingressClient)).thenReturn(rawJson);
        when(dataIngressProcessor.preProcess(rawJson)).thenReturn("processed");
        doThrow(new IllegalStateException("boom")).when(dataIngressProcessor).apply("processed");

        var processor =
                new CsdsIngressProcessor(
                        properties,
                        ingressClient,
                        csdsExecutionLogService,
                        distributedJobLockService,
                        List.of(dataIngressProcessor));
        var startedAt = LocalDateTime.parse("2026-07-27T03:05:00");
        var result = processor.runScheduledIngress(startedAt);

        assertThat(result.status()).isEqualTo(CsdsIngressProcessor.ScheduledRunStatus.FAILED);
        assertThat(result.message()).isEqualTo("Failed processors: test-dataset");
        verify(csdsExecutionLogService)
                .recordFailure(
                        CsdsIngressProcessor.DATABASE_JOB_NAME,
                        startedAt,
                        "Failed processors: test-dataset");
        verify(distributedJobLockService).release(lock);
    }

    @Test
    void given_processorFails_when_runManualIngress_then_continuesAndReportsFailure() {
        var properties = new CsdsIngressProperties();
        properties.setLeaseDuration(Duration.ofMinutes(3));
        @SuppressWarnings("unchecked")
        IDataIngressProcessor<String> secondProcessor = mock(IDataIngressProcessor.class);
        var processor =
                new CsdsIngressProcessor(
                        properties,
                        ingressClient,
                        csdsExecutionLogService,
                        distributedJobLockService,
                        List.of(dataIngressProcessor, secondProcessor));
        List<JsonNode> rawJson = List.of(OBJECT_MAPPER.createObjectNode().put("code", "alpha"));
        var lock =
                new DistributedJobLock(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, "token", Duration.ofMinutes(3));

        when(distributedJobLockService.tryAcquire(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, Duration.ofMinutes(3)))
                .thenReturn(Optional.of(lock));
        when(distributedJobLockService.renew(lock)).thenReturn(true);
        when(distributedJobLockService.release(lock)).thenReturn(true);
        when(dataIngressProcessor.enabled()).thenReturn(true);
        when(dataIngressProcessor.datasetName()).thenReturn("failed-dataset");
        when(dataIngressProcessor.retrieve(ingressClient)).thenReturn(rawJson);
        when(dataIngressProcessor.preProcess(rawJson)).thenReturn("processed");
        doThrow(new IllegalStateException("boom")).when(dataIngressProcessor).apply("processed");
        when(secondProcessor.enabled()).thenReturn(true);
        when(secondProcessor.datasetName()).thenReturn("successful-dataset");
        when(secondProcessor.retrieve(ingressClient)).thenReturn(rawJson);
        when(secondProcessor.preProcess(rawJson)).thenReturn("processed");

        assertThatThrownBy(processor::runManualIngress)
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("Failed processors: failed-dataset");

        verify(secondProcessor).apply("processed");
        verify(distributedJobLockService).release(lock);
        verifyNoInteractions(csdsExecutionLogService);
    }

    @Test
    void given_registeredProcessor_when_runManualIngress_then_executesWithoutScheduledLog() {
        var properties = new CsdsIngressProperties();
        properties.setLeaseDuration(Duration.ofMinutes(3));
        List<JsonNode> rawJson = List.of(OBJECT_MAPPER.createObjectNode().put("code", "alpha"));
        var lock =
                new DistributedJobLock(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, "token", Duration.ofMinutes(3));

        when(distributedJobLockService.tryAcquire(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, Duration.ofMinutes(3)))
                .thenReturn(Optional.of(lock));
        when(distributedJobLockService.release(lock)).thenReturn(true);
        when(dataIngressProcessor.enabled()).thenReturn(true);
        when(dataIngressProcessor.datasetName()).thenReturn("test-dataset");
        when(dataIngressProcessor.retrieve(ingressClient)).thenReturn(rawJson);
        when(dataIngressProcessor.preProcess(rawJson)).thenReturn("processed");

        var processor =
                new CsdsIngressProcessor(
                        properties,
                        ingressClient,
                        csdsExecutionLogService,
                        distributedJobLockService,
                        List.of(dataIngressProcessor));
        processor.runManualIngress();

        verify(dataIngressProcessor).apply("processed");
        verify(distributedJobLockService).release(lock);
        verifyNoInteractions(csdsExecutionLogService);
    }

    @Test
    void given_lockUnavailable_when_runManualIngress_then_reportsLocked() {
        var properties = new CsdsIngressProperties();
        properties.setLeaseDuration(Duration.ofMinutes(3));
        var processor =
                new CsdsIngressProcessor(
                        properties,
                        ingressClient,
                        csdsExecutionLogService,
                        distributedJobLockService,
                        List.of(dataIngressProcessor));

        when(distributedJobLockService.tryAcquire(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, Duration.ofMinutes(3)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(processor::runManualIngress)
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("The CSDS ingest is already running");

        verifyNoInteractions(dataIngressProcessor, csdsExecutionLogService);
    }

    @Test
    void given_processorFailsWithCause_when_runIngress_then_logIncludesCauseMessage() {
        var properties = new CsdsIngressProperties();
        properties.setLeaseDuration(Duration.ofMinutes(3));
        var logCaptor = LogCaptor.forClass(CsdsIngressProcessor.class);
        logCaptor.clearLogs();

        var processor =
                new CsdsIngressProcessor(
                        properties,
                        ingressClient,
                        csdsExecutionLogService,
                        distributedJobLockService,
                        List.of(dataIngressProcessor));

        List<JsonNode> rawJson = List.of(OBJECT_MAPPER.createObjectNode().put("code", "alpha"));
        var lock =
                new DistributedJobLock(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, "token", Duration.ofMinutes(3));

        when(distributedJobLockService.tryAcquire(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, Duration.ofMinutes(3)))
                .thenReturn(Optional.of(lock));
        when(distributedJobLockService.release(lock)).thenReturn(true);
        when(dataIngressProcessor.enabled()).thenReturn(true);
        when(dataIngressProcessor.datasetName()).thenReturn("test-dataset");
        when(dataIngressProcessor.retrieve(ingressClient)).thenReturn(rawJson);
        when(dataIngressProcessor.preProcess(rawJson)).thenReturn("processed");
        doThrow(new IllegalStateException("boom", new RuntimeException("404 Not Found")))
                .when(dataIngressProcessor)
                .apply("processed");

        var executed = processor.runIngress();

        assertThat(executed).isTrue();
        assertThat(logCaptor.getErrorLogs())
                .anyMatch(
                        log ->
                                log.contains("Skipping CSDS ingress processor test-dataset")
                                        && log.contains(
                                                "after failure: boom (cause: 404 Not Found)"));
    }

    @Test
    void given_leaseRenewalFails_when_runScheduledIngress_then_returnsFailedResult() {
        var properties = new CsdsIngressProperties();
        properties.setLeaseDuration(Duration.ofMinutes(3));
        @SuppressWarnings("unchecked")
        IDataIngressProcessor<String> secondProcessor = mock(IDataIngressProcessor.class);
        var processor =
                new CsdsIngressProcessor(
                        properties,
                        ingressClient,
                        csdsExecutionLogService,
                        distributedJobLockService,
                        List.of(dataIngressProcessor, secondProcessor));
        List<JsonNode> firstRawJson =
                List.of(OBJECT_MAPPER.createObjectNode().put("code", "alpha"));
        var lock =
                new DistributedJobLock(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, "token", Duration.ofMinutes(3));

        when(distributedJobLockService.tryAcquire(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, Duration.ofMinutes(3)))
                .thenReturn(Optional.of(lock));
        when(distributedJobLockService.renew(lock)).thenReturn(false);
        when(distributedJobLockService.release(lock)).thenReturn(true);
        when(dataIngressProcessor.enabled()).thenReturn(true);
        when(dataIngressProcessor.datasetName()).thenReturn("test-dataset-1");
        when(dataIngressProcessor.retrieve(ingressClient)).thenReturn(firstRawJson);
        when(dataIngressProcessor.preProcess(firstRawJson)).thenReturn("processed-1");

        var startedAt = LocalDateTime.parse("2026-07-27T03:05:00");
        var result = processor.runScheduledIngress(startedAt);

        assertThat(result.status()).isEqualTo(CsdsIngressProcessor.ScheduledRunStatus.FAILED);
        assertThat(result.message())
                .contains("CSDS distributed lease was lost after processor test-dataset-1");
        verify(csdsExecutionLogService)
                .recordFailure(
                        eq(CsdsIngressProcessor.DATABASE_JOB_NAME),
                        eq(startedAt),
                        contains("CSDS distributed lease was lost after processor test-dataset-1"));
        verify(distributedJobLockService).release(lock);
    }

    @Test
    void given_backupFails_when_runIngress_then_continueProcessing() {
        var properties = new CsdsIngressProperties();
        properties.setLeaseDuration(Duration.ofMinutes(3));
        var logCaptor = LogCaptor.forClass(CsdsIngressProcessor.class);
        logCaptor.clearLogs();

        var processor =
                new CsdsIngressProcessor(
                        properties,
                        ingressClient,
                        csdsExecutionLogService,
                        distributedJobLockService,
                        List.of(dataIngressProcessor));
        List<JsonNode> rawJson = List.of(OBJECT_MAPPER.createObjectNode().put("code", "alpha"));
        var lock =
                new DistributedJobLock(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, "token", Duration.ofMinutes(3));

        when(distributedJobLockService.tryAcquire(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, Duration.ofMinutes(3)))
                .thenReturn(Optional.of(lock));
        when(distributedJobLockService.release(lock)).thenReturn(true);
        when(dataIngressProcessor.enabled()).thenReturn(true);
        when(dataIngressProcessor.datasetName()).thenReturn("test-dataset");
        when(dataIngressProcessor.retrieve(ingressClient)).thenReturn(rawJson);
        doThrow(new IllegalStateException("backup boom")).when(dataIngressProcessor).backup();
        when(dataIngressProcessor.preProcess(rawJson)).thenReturn("processed");

        var executed = processor.runIngress();

        assertThat(executed).isTrue();
        verify(dataIngressProcessor).apply("processed");
        assertThat(logCaptor.getErrorLogs())
                .anyMatch(
                        log ->
                                log.contains("Failed CSDS backup step for processor test-dataset")
                                        && log.contains("Continuing ingress"));
    }
}
