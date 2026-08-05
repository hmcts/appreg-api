package uk.gov.hmcts.appregister.csds.ingress.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.csds.ingress.processor.nationalcourthouse.NationalCourtHouseIngressRecord;

class NationalCourtHouseIngressDatabaseRowMapperTest {
    private final NationalCourtHouseIngressDatabaseRowMapper rowMapper =
            new NationalCourtHouseIngressDatabaseRowMapper();

    @Test
    void given_nationalCourtHouseRecord_when_toRow_then_mapsLegacyFields() {
        var row =
                rowMapper.toRow(
                        new NationalCourtHouseIngressRecord(
                                3106L,
                                "Brentford Magistrates' Court",
                                1L,
                                LocalDate.of(1900, Month.JANUARY, 1),
                                LocalDate.of(2011, Month.DECEMBER, 9),
                                "B01CF00",
                                null));

        assertThat(row)
                .containsEntry("nch_id", 3106L)
                .containsEntry("courthouse_name", "Brentford Magistrates' Court")
                .containsEntry("court_type", "CHOA")
                .containsEntry("changed_by", 0L)
                .containsEntry("court_location_code", "B01CF00")
                .containsEntry("loc_loc_id", null)
                .containsEntry("psa_psa_id", null)
                .containsEntry("norg_id", null);
        assertThat(rowMapper.insertExpressions())
                .containsEntry("changed_date", "current_timestamp");
        assertThat(rowMapper.updateExpressions())
                .containsEntry("changed_date", "current_timestamp");
    }

    @Test
    void given_resultSet_when_mapRow_then_readsComparedFields() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("nch_id")).thenReturn(3106L);
        when(rs.getString("courthouse_name")).thenReturn("Brentford Magistrates' Court");
        when(rs.getLong("version_number")).thenReturn(1L);
        when(rs.getObject("start_date", LocalDate.class))
                .thenReturn(LocalDate.of(1900, Month.JANUARY, 1));
        when(rs.getObject("end_date", LocalDate.class))
                .thenReturn(LocalDate.of(2011, Month.DECEMBER, 9));
        when(rs.getString("court_location_code")).thenReturn("B01CF00");
        when(rs.getString("sl_courthouse_name")).thenReturn(null);

        assertThat(rowMapper.mapRow(rs, 0))
                .isEqualTo(
                        new NationalCourtHouseIngressRecord(
                                3106L,
                                "Brentford Magistrates' Court",
                                1L,
                                LocalDate.of(1900, Month.JANUARY, 1),
                                LocalDate.of(2011, Month.DECEMBER, 9),
                                "B01CF00",
                                null));
    }
}
