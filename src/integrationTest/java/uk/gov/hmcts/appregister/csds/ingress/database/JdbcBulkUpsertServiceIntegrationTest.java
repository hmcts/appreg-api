package uk.gov.hmcts.appregister.csds.ingress.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.hmcts.appregister.csds.ingress.processor.fee.FeeIngressRecord;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;

class JdbcBulkUpsertServiceIntegrationTest extends BaseRepositoryTest {
    @Autowired private JdbcBulkUpsertService service;
    @Autowired private FeeIngressDatabaseRowMapper mapper;
    @Autowired private JdbcTemplate jdbc;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @Test
    void given_compositeFeeKey_when_upsert_then_preserveIdsAndKeepDifferentStartDates() {
        var firstDate = LocalDate.of(2020, 1, 1);
        var secondDate = firstDate.plusYears(1);
        var keys = List.of("fee_reference", "fee_start_date");
        service.upsertBatch(
                "fee",
                keys,
                List.of(
                        new FeeIngressRecord(
                                9000001L, "KEYTEST", "First", BigDecimal.ONE, firstDate, null, 1L),
                        new FeeIngressRecord(
                                9000002L,
                                "KEYTEST",
                                "Second",
                                BigDecimal.TEN,
                                secondDate,
                                null,
                                1L)),
                mapper);

        // A conflicting business key must update the row, not its stable numeric ID.
        service.upsertBatch(
                "fee",
                keys,
                List.of(
                        new FeeIngressRecord(
                                9000003L,
                                "KEYTEST",
                                "Updated",
                                BigDecimal.TEN,
                                firstDate,
                                null,
                                2L)),
                mapper);

        assertThat(
                        jdbc.queryForList(
                                "SELECT fee_id FROM %s.fee WHERE fee_reference = 'KEYTEST' ORDER BY fee_start_date"
                                        .formatted(schema),
                                Long.class))
                .containsExactly(9000001L, 9000002L);
        assertThat(
                        jdbc.queryForList(
                                ("SELECT fee_description FROM %s.fee"
                                                + " WHERE fee_reference = 'KEYTEST' ORDER BY fee_start_date")
                                        .formatted(schema),
                                String.class))
                .containsExactly("Updated", "Second");
    }
}
