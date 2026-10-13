package uk.gov.hmcts.appregister.csds.ingress.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode.ApplicationCodeIngressRecord;
import uk.gov.hmcts.appregister.csds.ingress.processor.fee.FeeIngressRecord;

@ExtendWith(MockitoExtension.class)
class JdbcBulkUpsertServiceTest {
    @Mock private NamedParameterJdbcTemplate jdbcTemplate;
    @Mock private JdbcBatchFailureIsolationService jdbcBatchFailureIsolationService;

    @InjectMocks private JdbcBulkUpsertService service;
    private ApplicationCodeIngressDatabaseRowMapper rowMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "schema", "appreg");
        rowMapper = new ApplicationCodeIngressDatabaseRowMapper();
    }

    @Test
    void given_records_when_upsertBatch_then_buildExpectedSqlAndParameters() {
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
                        null,
                        YesOrNo.NO,
                        3L,
                        "FEE-1");
        when(jdbcTemplate.batchUpdate(anyString(), any(MapSqlParameterSource[].class)))
                .thenReturn(new int[] {1});

        var result =
                service.upsertBatch(
                        "application_codes_staging", List.of("ac_id"), List.of(item), rowMapper);

        var sqlCaptor = ArgumentCaptor.forClass(String.class);
        var paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource[].class);
        verify(jdbcTemplate).batchUpdate(sqlCaptor.capture(), paramsCaptor.capture());

        assertThat(result).containsExactly(1);
        assertThat(sqlCaptor.getValue())
                .contains("INSERT INTO appreg.application_codes_staging")
                .contains("ON CONFLICT (ac_id) DO UPDATE")
                .contains("changed_date = current_timestamp");
        assertThat(paramsCaptor.getValue()).hasSize(1);
        assertThat(paramsCaptor.getValue()[0].getValue("ac_id")).isEqualTo(12L);
        assertThat(paramsCaptor.getValue()[0].getValue("changed_by")).isEqualTo(0L);
        assertThat(paramsCaptor.getValue()[0].getValue("user_name")).isEqualTo("CSDS_INGRESS");
    }

    @Test
    void given_compositeKey_when_upsertBatch_then_useBothColumnsWithoutUpdatingEither() {
        var item =
                new FeeIngressRecord(
                        12L, "FEE-1", "Fee", BigDecimal.ONE, LocalDate.of(2026, 1, 1), null, 1L);
        when(jdbcTemplate.batchUpdate(anyString(), any(MapSqlParameterSource[].class)))
                .thenReturn(new int[] {1});
        service.upsertBatch(
                "fee",
                List.of("fee_reference", "fee_start_date"),
                List.of(item),
                new FeeIngressDatabaseRowMapper());
        var sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).batchUpdate(sql.capture(), any(MapSqlParameterSource[].class));
        assertThat(sql.getValue())
                .contains("ON CONFLICT (fee_reference, fee_start_date) DO UPDATE")
                .doesNotContain(
                        "fee_reference = EXCLUDED",
                        "fee_start_date = EXCLUDED",
                        "fee_id = EXCLUDED")
                .contains("fee_value = EXCLUDED.fee_value");
    }

    @Test
    void given_invalidKeys_when_upsertBatch_then_rejectBeforeJdbc() {
        var item =
                new FeeIngressRecord(
                        12L, "FEE-1", "Fee", BigDecimal.ONE, LocalDate.of(2026, 1, 1), null, 1L);
        var mapper = new FeeIngressDatabaseRowMapper();
        List<List<String>> invalidKeys =
                Arrays.asList(
                        null,
                        List.of(),
                        List.of("fee_id", "fee_id"),
                        List.of("missing_column"),
                        List.of("fee_id; DROP TABLE fee"),
                        Arrays.asList((String) null));
        for (var keys : invalidKeys) {
            assertThatThrownBy(() -> service.upsertBatch("fee", keys, List.of(item), mapper))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(service.upsertBatch("fee", List.of("fee_id"), List.of(), mapper)).isEmpty();
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void given_invalidTableName_when_upsertBatch_then_rejectIt() {
        var item =
                new ApplicationCodeIngressRecord(
                        12L,
                        "AA00001",
                        "Title",
                        "Wording",
                        null,
                        YesOrNo.YES,
                        YesOrNo.NO,
                        LocalDate.of(2020, Month.JANUARY, 1),
                        null,
                        YesOrNo.NO,
                        3L,
                        null);
        var tableName = "application-codes-test";
        var rows = List.of(item);
        assertThatThrownBy(() -> service.upsertBatch(tableName, List.of("ac_id"), rows, rowMapper))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid SQL tableName");
    }

    @Test
    void given_batchFailure_when_upsertBatch_then_throwCsdsBatchUpsertException() {
        var item =
                new ApplicationCodeIngressRecord(
                        12L,
                        "AA00001",
                        "Title",
                        "Wording",
                        null,
                        YesOrNo.YES,
                        YesOrNo.NO,
                        LocalDate.of(2020, Month.JANUARY, 1),
                        null,
                        YesOrNo.NO,
                        3L,
                        null);
        when(jdbcTemplate.batchUpdate(anyString(), any(MapSqlParameterSource[].class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("boom"));
        when(jdbcBatchFailureIsolationService.identifyFailures(
                        anyString(), any(), any(), any(), any(RuntimeException.class)))
                .thenReturn(List.of(new FailedUpsertRecord<>(item, "boom")));

        ThrowingCallable upsertCall =
                () ->
                        service.upsertBatch(
                                "application_codes_staging",
                                List.of("ac_id"),
                                List.of(item),
                                rowMapper,
                                ApplicationCodeIngressRecord::id);

        assertThatThrownBy(upsertCall)
                .isInstanceOf(CsdsBatchUpsertException.class)
                .hasMessageContaining("CSDS batch upsert failed");
    }
}
