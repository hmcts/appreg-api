package uk.gov.hmcts.appregister.csds.ingress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

        when(distributedJobLockService.executeWithLock(
                        eq(CsdsIngressProcessor.DATABASE_JOB_NAME),
                        eq(Duration.ofMinutes(3)),
                        any(Runnable.class)))
                .thenAnswer(
                        invocation -> {
                            invocation.<Runnable>getArgument(2).run();
                            return true;
                        });
        when(dataIngressProcessor.datasetName()).thenReturn("test-dataset");
        when(dataIngressProcessor.retrieve(ingressClient)).thenReturn(rawJson);
        when(dataIngressProcessor.preProcess(rawJson)).thenReturn("processed");

        var executed = processor.runIngress();

        assertThat(executed).isTrue();
        verify(dataIngressProcessor).apply("processed");
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

        when(distributedJobLockService.executeWithLock(
                        eq(CsdsIngressProcessor.DATABASE_JOB_NAME),
                        eq(Duration.ofMinutes(3)),
                        any(Runnable.class)))
                .thenReturn(false);

        var executed = processor.runIngress();

        assertThat(executed).isFalse();
        verifyNoInteractions(dataIngressProcessor);
    }
}
