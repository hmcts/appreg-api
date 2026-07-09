package uk.gov.hmcts.appregister.csds.ingress.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.csds.ingress.processor.resolutioncode.ResolutionCodeIngressRecord;

class ResolutionCodeIngressDatabaseRowMapperTest {
    private final ResolutionCodeIngressDatabaseRowMapper mapper =
            new ResolutionCodeIngressDatabaseRowMapper();

    @Test
    void given_ingressRecord_when_toRow_then_mapExpectedColumnsAndDefaults() {
        var item =
                new ResolutionCodeIngressRecord(
                        12L,
                        "APPC",
                        "Title",
                        "Wording",
                        "Legislation",
                        "email1@example.com",
                        "email2@example.com",
                        LocalDate.of(2020, Month.JANUARY, 1),
                        LocalDate.of(2020, Month.FEBRUARY, 1),
                        3L);

        var row = mapper.toRow(item);

        assertThat(mapper.columns()).contains("rc_id", "changed_date", "user_name");
        assertThat(mapper.columns()).doesNotContain("bulk_respondent_allowed");
        assertThat(mapper.updatableColumns()).doesNotContain("rc_id");
        assertThat(mapper.updatableColumns()).doesNotContain("bulk_respondent_allowed");
        assertThat(mapper.insertExpressions()).containsEntry("changed_date", "current_timestamp");
        assertThat(mapper.updateExpressions()).containsEntry("changed_date", "current_timestamp");
        assertThat(row)
                .containsEntry("rc_id", 12L)
                .containsEntry("resolution_code", "APPC")
                .containsEntry("resolution_code_title", "Title")
                .containsEntry("resolution_code_wording", "Wording")
                .containsEntry("resolution_legislation", "Legislation")
                .containsEntry("rc_destination_email_address_1", "email1@example.com")
                .containsEntry("rc_destination_email_address_2", "email2@example.com")
                .containsEntry("resolution_code_start_date", LocalDate.of(2020, Month.JANUARY, 1))
                .containsEntry("resolution_code_end_date", LocalDate.of(2020, Month.FEBRUARY, 1))
                .containsEntry("version", 3L)
                .containsEntry("changed_by", 0L)
                .containsEntry("user_name", "CSDS_INGRESS");
        assertThat(row.get("changed_date")).isNull();
    }

    @Test
    void given_resultSet_when_mapRow_then_createExpectedRecord() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("rc_id")).thenReturn(12L);
        when(rs.getString("resolution_code")).thenReturn("APPC");
        when(rs.getString("resolution_code_title")).thenReturn("Title");
        when(rs.getString("resolution_code_wording")).thenReturn("Wording");
        when(rs.getString("resolution_legislation")).thenReturn("Legislation");
        when(rs.getString("rc_destination_email_address_1")).thenReturn("email1@example.com");
        when(rs.getString("rc_destination_email_address_2")).thenReturn("email2@example.com");
        when(rs.getObject("resolution_code_start_date", LocalDate.class))
                .thenReturn(LocalDate.of(2020, Month.JANUARY, 1));
        when(rs.getObject("resolution_code_end_date", LocalDate.class))
                .thenReturn(LocalDate.of(2020, Month.FEBRUARY, 1));
        when(rs.getLong("version")).thenReturn(3L);

        var mapped = mapper.mapRow(rs, 0);

        assertThat(mapped)
                .isEqualTo(
                        new ResolutionCodeIngressRecord(
                                12L,
                                "APPC",
                                "Title",
                                "Wording",
                                "Legislation",
                                "email1@example.com",
                                "email2@example.com",
                                LocalDate.of(2020, Month.JANUARY, 1),
                                LocalDate.of(2020, Month.FEBRUARY, 1),
                                3L));
    }
}
