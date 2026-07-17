package uk.gov.hmcts.appregister.csds.ingress.processor.standardapplicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcBulkUpsertService;
import uk.gov.hmcts.appregister.csds.ingress.database.StandardApplicantIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;

@ExtendWith(MockitoExtension.class)
class StandardApplicantIngressApplyServiceTest {
    @Mock private NamedParameterJdbcTemplate jdbcTemplate;
    @Mock private JdbcBulkUpsertService bulkUpsertService;
    @Mock private BusinessDateProvider businessDateProvider;

    private StandardApplicantIngressApplyService service;
    private StandardApplicantIngressDatabaseRowMapper rowMapper;

    @BeforeEach
    void setUp() {
        rowMapper = new StandardApplicantIngressDatabaseRowMapper();
        service =
                new StandardApplicantIngressApplyService(
                        jdbcTemplate, bulkUpsertService, rowMapper, businessDateProvider);
        ReflectionTestUtils.setField(service, "schema", "appreg");
        when(businessDateProvider.currentUkDate()).thenReturn(LocalDate.of(2026, 7, 10));
    }

    @Test
    void given_incomingIds_when_reconcileAndUpsert_then_endDatesOnlyMissingRowsInConfiguredTable() {
        var record = record(6278L);
        var diff =
                new StandardApplicantDiffResult(
                        Map.of(record.id(), record),
                        Map.of(),
                        List.of(
                                new IngressDiffRecord<>(
                                        IngressOperation.INSERT,
                                        record,
                                        null,
                                        record,
                                        "no existing sa_id match")));

        service.reconcileAndUpsert("standard_applicants_staging", "sa_id", diff);

        var sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate)
                .update(sqlCaptor.capture(), org.mockito.ArgumentMatchers.<Map<String, ?>>any());
        assertThat(sqlCaptor.getValue())
                .contains("UPDATE appreg.standard_applicants_staging")
                .contains("sa_id NOT IN (:incomingIds)")
                .contains("version = version + 1");
        verify(bulkUpsertService)
                .upsertBatch(
                        eq("standard_applicants_staging"),
                        eq("sa_id"),
                        eq(List.of(record)),
                        eq(rowMapper),
                        org.mockito.ArgumentMatchers.any());
    }

    private StandardApplicantIngressRecord record(Long id) {
        return new StandardApplicantIngressRecord(
                id,
                "DCCMH",
                LocalDate.of(2018, 8, 1),
                null,
                2L,
                "Derbyshire County Council",
                "County Hall",
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
