package uk.gov.hmcts.appregister.csds.ingress.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.csds.ingress.processor.fee.FeeIngressRecord;

class FeeIngressDatabaseRowMapperTest {
    private final FeeIngressDatabaseRowMapper rowMapper = new FeeIngressDatabaseRowMapper();

    @Test
    void given_feeRecord_when_toRow_then_setsIsOffsiteFalse() {
        var row =
                rowMapper.toRow(
                        new FeeIngressRecord(
                                33L,
                                "CO10.1",
                                "Fee",
                                new BigDecimal("245.00"),
                                LocalDate.of(2020, Month.JANUARY, 1),
                                null,
                                1L));

        assertThat(row)
                .containsEntry("is_offsite", false)
                .containsEntry("fee_id", 33L)
                .containsEntry("fee_reference", "CO10.1");
    }

    @Test
    void given_offsiteFeeReference_when_toRow_then_setsIsOffsiteTrue() {
        var row =
                rowMapper.toRow(
                        new FeeIngressRecord(
                                1L,
                                "CO1.1",
                                "Fee",
                                new BigDecimal("245.00"),
                                LocalDate.of(2020, Month.JANUARY, 1),
                                null,
                                1L));

        assertThat(row).containsEntry("is_offsite", true);
    }

    @Test
    void given_resultSet_when_mapRow_then_readsFields() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("fee_id")).thenReturn(33L);
        when(rs.getString("fee_reference")).thenReturn("CO10.1");
        when(rs.getString("fee_description")).thenReturn("Fee");
        when(rs.getBigDecimal("fee_value")).thenReturn(new BigDecimal("245.00"));
        when(rs.getObject("fee_start_date", LocalDate.class))
                .thenReturn(LocalDate.of(2020, Month.JANUARY, 1));
        when(rs.getObject("fee_end_date", LocalDate.class)).thenReturn(null);
        when(rs.getLong("fee_version")).thenReturn(1L);

        var mappedRecord = rowMapper.mapRow(rs, 0);

        assertThat(mappedRecord)
                .isEqualTo(
                        new FeeIngressRecord(
                                33L,
                                "CO10.1",
                                "Fee",
                                new BigDecimal("245.00"),
                                LocalDate.of(2020, Month.JANUARY, 1),
                                null,
                                1L));
    }
}
