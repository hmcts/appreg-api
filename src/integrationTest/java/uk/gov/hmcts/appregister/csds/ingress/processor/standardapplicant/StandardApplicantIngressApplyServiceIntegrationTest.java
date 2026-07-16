package uk.gov.hmcts.appregister.csds.ingress.processor.standardapplicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.hmcts.appregister.csds.ingress.database.CsdsBatchUpsertException;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;

class StandardApplicantIngressApplyServiceIntegrationTest extends BaseRepositoryTest {
    @Autowired private StandardApplicantIngressApplyService applyService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @Test
    void
            given_missingStagingApplicant_when_reconcileAndUpsert_then_endDatesItAndUpsertsIncomingRecord() {
        insertOpenStagingApplicant(1L);
        var incoming = record(2L, "County Hall");

        applyService.reconcileAndUpsert("standard_applicants_staging", "sa_id", diffFor(incoming));

        assertThat(endDate(1L)).isEqualTo(LocalDate.now());
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT standard_applicant_code FROM %s.standard_applicants_staging WHERE sa_id = 2"
                                        .formatted(schema),
                                String.class))
                .isEqualTo("DCCMH");
    }

    @Test
    void given_upsertFailure_when_reconcileAndUpsert_then_rollsBackTheEndDate() {
        insertOpenStagingApplicant(1L);
        var invalidIncoming = record(2L, "x".repeat(36));

        assertThatThrownBy(
                        () ->
                                applyService.reconcileAndUpsert(
                                        "standard_applicants_staging",
                                        "sa_id",
                                        diffFor(invalidIncoming)))
                .isInstanceOf(CsdsBatchUpsertException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);

        assertThat(endDate(1L)).isNull();
    }

    private void insertOpenStagingApplicant(Long id) {
        jdbcTemplate.update(
                """
                INSERT INTO %s.standard_applicants_staging (
                    sa_id, standard_applicant_code, standard_applicant_start_date,
                    version, changed_by, changed_date, address_l1
                ) VALUES (?, ?, ?, ?, ?, current_timestamp, ?)
                """
                        .formatted(schema),
                id,
                "OLD",
                LocalDate.of(2020, 1, 1),
                1L,
                0L,
                "Old address");
    }

    private LocalDate endDate(Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT standard_applicant_end_date FROM %s.standard_applicants_staging WHERE sa_id = ?"
                        .formatted(schema),
                LocalDate.class,
                id);
    }

    private StandardApplicantDiffResult diffFor(StandardApplicantIngressRecord incoming) {
        return new StandardApplicantDiffResult(
                Map.of(incoming.id(), incoming),
                Map.of(),
                List.of(
                        new IngressDiffRecord<>(
                                IngressOperation.INSERT,
                                incoming,
                                null,
                                incoming,
                                "no existing sa_id match")));
    }

    private StandardApplicantIngressRecord record(Long id, String addressLine1) {
        return new StandardApplicantIngressRecord(
                id,
                "DCCMH",
                LocalDate.of(2018, 8, 1),
                null,
                2L,
                "Derbyshire County Council",
                addressLine1,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
