package uk.gov.hmcts.appregister.csds.ingress.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode.ApplicationCodeIngressRecord;

class ApplicationCodeIngressDatabaseRowMapperTest {
    private final ApplicationCodeIngressDatabaseRowMapper mapper =
            new ApplicationCodeIngressDatabaseRowMapper();

    @Test
    void given_ingressRecord_when_toRow_then_mapExpectedColumnsAndDefaults() {
        var item =
                new ApplicationCodeIngressRecord(
                        12L,
                        "AA00001",
                        "Title",
                        "Wording",
                        "Legislation",
                        YesOrNo.YES,
                        YesOrNo.NO,
                        LocalDate.of(2020, Month.JANUARY, 1),
                        LocalDate.of(2020, Month.FEBRUARY, 1),
                        YesOrNo.NO,
                        3L,
                        "FEE-1");

        var row = mapper.toRow(item);

        assertThat(mapper.columns()).contains("ac_id", "changed_date", "user_name");
        assertThat(mapper.updatableColumns()).doesNotContain("ac_id").contains("changed_date");
        assertThat(mapper.insertExpressions()).containsEntry("changed_date", "current_timestamp");
        assertThat(mapper.updateExpressions()).containsEntry("changed_date", "current_timestamp");
        assertThat(row)
                .containsEntry("ac_id", 12L)
                .containsEntry("application_code", "AA00001")
                .containsEntry("application_code_title", "Title")
                .containsEntry("application_code_wording", "Wording")
                .containsEntry("application_legislation", "Legislation")
                .containsEntry("fee_due", "Y")
                .containsEntry("application_code_respondent", "N")
                .containsEntry("application_code_start_date", LocalDate.of(2020, Month.JANUARY, 1))
                .containsEntry("application_code_end_date", LocalDate.of(2020, Month.FEBRUARY, 1))
                .containsEntry("bulk_respondent_allowed", "N")
                .containsEntry("version", 3L)
                .containsEntry("changed_by", 0L)
                .containsEntry("user_name", "CSDS_INGRESS")
                .containsEntry("ac_fee_reference", "FEE-1");
        assertThat(row.get("ac_destination_email_address_1")).isNull();
        assertThat(row.get("ac_destination_email_address_2")).isNull();
        assertThat(row.get("changed_date")).isNull();
    }
}
