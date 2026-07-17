package uk.gov.hmcts.appregister.csds.ingress.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
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
                service.upsertBatch("application_codes_staging", "ac_id", List.of(item), rowMapper);

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
        assertThatThrownBy(
                        () ->
                                service.upsertBatch(
                                        "application-codes-test",
                                        "ac_id",
                                        List.of(item),
                                        rowMapper))
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
                                "ac_id",
                                List.of(item),
                                rowMapper,
                                ApplicationCodeIngressRecord::id);

        assertThatThrownBy(upsertCall)
                .isInstanceOf(CsdsBatchUpsertException.class)
                .hasMessageContaining("CSDS batch upsert failed");
    }
}
