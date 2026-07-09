package uk.gov.hmcts.appregister.csds.ingress.processor.resolutioncode;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ResolutionCodeIngressRecordTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void given_pssrcidPresent_when_calculateId_then_useIt() {
        assertThat(ResolutionCodeIngressRecord.calculateId(77L, 12L)).isEqualTo(77L);
    }

    @Test
    void given_pssrcidMissing_when_calculateId_then_offsetResolutionCodeId() {
        assertThat(ResolutionCodeIngressRecord.calculateId(null, 12L)).isEqualTo(100012L);
    }

    @Test
    void given_rcIdPresent_when_resolveId_then_useIt() {
        var record = OBJECT_MAPPER.createObjectNode().put("RC_ID", 123L);

        assertThat(ResolutionCodeIngressRecord.resolveId(record)).isEqualTo(123L);
    }

    @Test
    void given_pssrcidPresent_when_resolveId_then_useIt() {
        var record = OBJECT_MAPPER.createObjectNode().put("PSSRCID", 77L);

        assertThat(ResolutionCodeIngressRecord.resolveId(record)).isEqualTo(77L);
    }

    @Test
    void given_resolutionCodeIdPresent_when_resolveId_then_offsetIt() {
        var record = OBJECT_MAPPER.createObjectNode().put("ResolutionCodeID", 12L);

        assertThat(ResolutionCodeIngressRecord.resolveId(record)).isEqualTo(100012L);
    }

    @Test
    void given_noIdentifiers_when_resolveId_then_returnNull() {
        var record = OBJECT_MAPPER.createObjectNode();

        assertThat(ResolutionCodeIngressRecord.resolveId(record)).isNull();
    }
}
