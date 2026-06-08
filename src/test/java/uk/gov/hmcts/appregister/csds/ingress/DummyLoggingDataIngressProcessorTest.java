package uk.gov.hmcts.appregister.csds.ingress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DummyLoggingDataIngressProcessorTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock private CsdsIngressClient ingressClient;

    @Test
    void given_dummyProcessor_when_invoked_then_logsAndPassesThroughJsonPayloads() {
        var firstJson = OBJECT_MAPPER.createObjectNode().put("endpoint", "primary");
        var secondJson = OBJECT_MAPPER.createObjectNode().put("endpoint", "secondary");

        when(ingressClient.retrieveJson("/dummy/primary")).thenReturn(firstJson);
        when(ingressClient.retrieveJson("/dummy/secondary")).thenReturn(secondJson);

        var processor = new DummyLoggingDataIngressProcessor();
        var logCaptor = LogCaptor.forClass(DummyLoggingDataIngressProcessor.class);
        logCaptor.clearLogs();
        var retrieved = processor.retrieve(ingressClient);
        var processed = processor.preProcess(retrieved);
        processor.handle(processed);

        assertThat(retrieved).containsExactly(firstJson, secondJson);
        assertThat(processed).containsExactlyElementsOf(retrieved);
        assertThat(logCaptor.getInfoLogs())
                .anyMatch(log -> log.contains("retrieve invoked"))
                .anyMatch(log -> log.contains("preProcess invoked"))
                .anyMatch(log -> log.contains("handle invoked"));
    }
}
