package uk.gov.hmcts.appregister.csds.ingress.processor.fee;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class FeeIngressRecordTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void given_pssFixedListIdPresent_when_calculateId_then_useIt() {
        assertThat(FeeIngressRecord.calculateId(77L, 12L)).isEqualTo(77L);
    }

    @Test
    void given_pssFixedListIdMissing_when_calculateId_then_offsetCivilFeeId() {
        assertThat(FeeIngressRecord.calculateId(null, 12L)).isEqualTo(100012L);
    }

    @Test
    void given_feeIdPresent_when_resolveId_then_useIt() {
        var record = OBJECT_MAPPER.createObjectNode().put("FEE_ID", 123L);

        assertThat(FeeIngressRecord.resolveId(record)).isEqualTo(123L);
    }

    @Test
    void given_pssFixedListIdPresent_when_resolveId_then_useIt() {
        var record = OBJECT_MAPPER.createObjectNode().put("PSSFixedListID", 77L);

        assertThat(FeeIngressRecord.resolveId(record)).isEqualTo(77L);
    }

    @Test
    void given_civilFeeIdPresent_when_resolveId_then_offsetIt() {
        var record = OBJECT_MAPPER.createObjectNode().put("CivilFeeID", 12L);

        assertThat(FeeIngressRecord.resolveId(record)).isEqualTo(100012L);
    }

    @Test
    void given_noIdentifiers_when_resolveId_then_returnNull() {
        var record = OBJECT_MAPPER.createObjectNode();

        assertThat(FeeIngressRecord.resolveId(record)).isNull();
    }
}
