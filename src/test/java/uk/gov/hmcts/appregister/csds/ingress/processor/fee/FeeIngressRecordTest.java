package uk.gov.hmcts.appregister.csds.ingress.processor.fee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FeeIngressRecordTest {

    @Test
    void given_pssFixedListIdPresent_when_calculateId_then_useIt() {
        assertThat(FeeIngressRecord.calculateId(77L, 12L)).isEqualTo(77L);
    }

    @Test
    void given_pssFixedListIdMissing_when_calculateId_then_offsetCivilFeeId() {
        assertThat(FeeIngressRecord.calculateId(null, 12L)).isEqualTo(100012L);
    }

    @Test
    void overflowIsRejectedOnlyForFallbackAllocation() {
        assertThatThrownBy(() -> FeeIngressRecord.calculateId(null, Long.MAX_VALUE))
                .isInstanceOf(ArithmeticException.class);
        assertThat(FeeIngressRecord.calculateId(77L, Long.MAX_VALUE)).isEqualTo(77L);
    }

    @Test
    void absentIdsReturnNull() {
        assertThat(FeeIngressRecord.calculateId(null, null)).isNull();
    }

    @Test
    void businessKeyAndResolvedCopyPreserveAllOtherFields() {
        var start = LocalDate.parse("2020-01-01");
        var record =
                new FeeIngressRecord(
                        12L, "CO1.1", "Fee", BigDecimal.TEN, start, start.plusYears(1), 2L, 77L);
        assertThat(record.businessKey())
                .isEqualTo(new FeeIngressRecord.BusinessKey("CO1.1", start));
        assertThat(record.withId(33L))
                .isEqualTo(
                        new FeeIngressRecord(
                                33L,
                                "CO1.1",
                                "Fee",
                                BigDecimal.TEN,
                                start,
                                start.plusYears(1),
                                2L,
                                77L));
    }
}
