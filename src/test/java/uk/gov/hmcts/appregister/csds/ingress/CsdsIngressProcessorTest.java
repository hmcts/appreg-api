package uk.gov.hmcts.appregister.csds.ingress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.common.lock.DistributedJobLock;
import uk.gov.hmcts.appregister.common.lock.DistributedJobLockService;
import uk.gov.hmcts.appregister.csds.ingress.processor.IDataIngressProcessor;

@ExtendWith(MockitoExtension.class)
class CsdsIngressProcessorTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock private CsdsIngressClient ingressClient;
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
                        distributedJobLockService,
                        List.of(dataIngressProcessor));

        List<JsonNode> rawJson = List.of(OBJECT_MAPPER.createObjectNode().put("code", "alpha"));
        var lock =
                new DistributedJobLock(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, "token", Duration.ofMinutes(3));

        when(distributedJobLockService.tryAcquire(
                        eq(CsdsIngressProcessor.DATABASE_JOB_NAME), eq(Duration.ofMinutes(3))))
                .thenReturn(Optional.of(lock));
        when(distributedJobLockService.release(lock)).thenReturn(true);
        when(dataIngressProcessor.datasetName()).thenReturn("test-dataset");
        when(dataIngressProcessor.retrieve(ingressClient)).thenReturn(rawJson);
        when(dataIngressProcessor.preProcess(rawJson)).thenReturn("processed");

        var executed = processor.runIngress();

        assertThat(executed).isTrue();
        verify(dataIngressProcessor).apply("processed");
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
                        distributedJobLockService,
                        List.of(dataIngressProcessor));

        when(distributedJobLockService.tryAcquire(
                        eq(CsdsIngressProcessor.DATABASE_JOB_NAME), eq(Duration.ofMinutes(3))))
                .thenReturn(Optional.empty());

        var executed = processor.runIngress();

        assertThat(executed).isFalse();
        verifyNoInteractions(dataIngressProcessor);
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
                        eq(CsdsIngressProcessor.DATABASE_JOB_NAME), eq(Duration.ofMinutes(3))))
                .thenReturn(Optional.of(lock));
        when(distributedJobLockService.renew(lock)).thenReturn(true);
        when(distributedJobLockService.release(lock)).thenReturn(true);
        when(dataIngressProcessor.datasetName()).thenReturn("test-dataset-1");
        when(dataIngressProcessor.retrieve(ingressClient)).thenReturn(firstRawJson);
        when(dataIngressProcessor.preProcess(firstRawJson)).thenReturn("processed-1");
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
        IDataIngressProcessor<String> secondProcessor =
                org.mockito.Mockito.mock(IDataIngressProcessor.class);
        var logCaptor = LogCaptor.forClass(CsdsIngressProcessor.class);
        logCaptor.clearLogs();

        var processor =
                new CsdsIngressProcessor(
                        properties,
                        ingressClient,
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
                        eq(CsdsIngressProcessor.DATABASE_JOB_NAME), eq(Duration.ofMinutes(3))))
                .thenReturn(Optional.of(lock));
        when(distributedJobLockService.renew(lock)).thenReturn(true);
        when(distributedJobLockService.release(lock)).thenReturn(true);
        when(dataIngressProcessor.datasetName()).thenReturn("test-dataset-1");
        when(dataIngressProcessor.retrieve(ingressClient)).thenReturn(firstRawJson);
        when(dataIngressProcessor.preProcess(firstRawJson)).thenReturn("processed-1");
        doThrow(new IllegalStateException("boom")).when(dataIngressProcessor).apply("processed-1");
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
                                        && log.contains("after failure"));
    }
}
