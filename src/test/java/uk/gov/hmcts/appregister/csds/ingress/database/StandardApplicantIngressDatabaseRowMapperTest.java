package uk.gov.hmcts.appregister.csds.ingress.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.csds.ingress.processor.standardapplicant.StandardApplicantIngressRecord;

class StandardApplicantIngressDatabaseRowMapperTest {
    private final StandardApplicantIngressDatabaseRowMapper rowMapper =
            new StandardApplicantIngressDatabaseRowMapper();

    @Test
    void given_standardApplicantRecord_when_toRow_then_populatesStagingColumns() {
        var record = record();

        assertThat(rowMapper.toRow(record))
                .containsEntry("sa_id", 6278L)
                .containsEntry("standard_applicant_code", "DCCMH")
                .containsEntry("address_l1", "County Hall")
                .containsEntry("changed_by", 0L)
                .containsEntry("user_name", "CSDS_INGRESS")
                .containsEntry("mobile_number", null);
    }

    @Test
    void given_resultSet_when_mapRow_then_readsStagingFields() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("sa_id")).thenReturn(6278L);
        when(rs.getString("standard_applicant_code")).thenReturn("DCCMH");
        when(rs.getObject("standard_applicant_start_date", LocalDate.class))
                .thenReturn(LocalDate.of(2018, 8, 1));
        when(rs.getObject("standard_applicant_end_date", LocalDate.class)).thenReturn(null);
        when(rs.getLong("version")).thenReturn(2L);
        when(rs.getString("name")).thenReturn("Derbyshire County Council");
        when(rs.getString("address_l1")).thenReturn("County Hall");

        var record = rowMapper.mapRow(rs, 0);

        assertThat(record.id()).isEqualTo(6278L);
        assertThat(record.code()).isEqualTo("DCCMH");
        assertThat(record.name()).isEqualTo("Derbyshire County Council");
        assertThat(record.addressLine1()).isEqualTo("County Hall");
    }

    private StandardApplicantIngressRecord record() {
        return new StandardApplicantIngressRecord(
                6278L,
                "DCCMH",
                LocalDate.of(2018, 8, 1),
                null,
                2L,
                "Derbyshire County Council",
                "County Hall",
                "Matlock",
                "Derbyshire",
                null,
                null,
                null,
                "email@example.test",
                "020 1234 5678");
    }
}
