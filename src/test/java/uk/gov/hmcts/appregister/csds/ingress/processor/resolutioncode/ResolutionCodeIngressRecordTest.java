package uk.gov.hmcts.appregister.csds.ingress.processor.resolutioncode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ResolutionCodeIngressRecordTest {
    @Test
    void pssIdTakesPrecedenceWithoutCalculatingOffset() {
        assertThat(ResolutionCodeIngressRecord.calculateId(77L, Long.MAX_VALUE)).isEqualTo(77L);
    }

    @Test
    void newIdUsesSourceOffsetOrReturnsNullIfMissing() {
        assertThat(ResolutionCodeIngressRecord.calculateId(null, 12L)).isEqualTo(100012L);
        assertThat(ResolutionCodeIngressRecord.calculateId(null, null)).isNull();
    }

    @Test
    void overflowingOffsetFailsRatherThanWrapping() {
        assertThatThrownBy(() -> ResolutionCodeIngressRecord.calculateId(null, Long.MAX_VALUE))
                .isInstanceOf(ArithmeticException.class);
    }
}
