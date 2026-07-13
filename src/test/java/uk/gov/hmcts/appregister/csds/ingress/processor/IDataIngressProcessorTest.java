package uk.gov.hmcts.appregister.csds.ingress.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressClient;
import uk.gov.hmcts.appregister.generated.model.CsdsIngestResponse;

class IDataIngressProcessorTest {

    @Test
    void given_defaultMethods_when_used_then_returnExpectedDefaults() {
        var processor = new TestProcessor();
        var ingressClient = mock(CsdsIngressClient.class);
        var firstPage = mock(JsonNode.class);
        var secondPage = mock(JsonNode.class);

        when(ingressClient.retrieveJson("/first")).thenReturn(firstPage);
        when(ingressClient.retrieveJson("/second")).thenReturn(secondPage);

        assertThat(processor.enabled()).isTrue();
        assertThat(processor.datasetName()).isEqualTo("test_processor");
        assertThat(processor.sourcePaths()).containsExactly("/first", "/second");
        assertThat(processor.retrieve(ingressClient)).containsExactly(firstPage, secondPage);
    }

    private static final class TestProcessor implements IDataIngressProcessor<List<JsonNode>> {
        @Override
        public String processorName() {
            return "test_processor";
        }

        @Override
        public String targetTable() {
            return "test_table";
        }

        @Override
        public String targetKeyField() {
            return "test_id";
        }

        @Override
        public List<String> sourcePaths() {
            return List.of("/first", "/second");
        }

        @Override
        public List<JsonNode> preProcess(List<JsonNode> rawJson) {
            return rawJson;
        }

        @Override
        public void apply(List<JsonNode> processedData) {
            // no-op
        }

        @Override
        public CsdsIngestResponse ingest(List<JsonNode> rawJson) {
            return new CsdsIngestResponse();
        }
    }
}
