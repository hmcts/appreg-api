package uk.gov.hmcts.appregister.audit.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serial;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import lombok.val;
import org.hibernate.SessionEventListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.data.DataAuditTestData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;

/**
 * Proves sequence prefetch and JDBC insert batching against PostgreSQL.
 */
@TestPropertySource(
        properties =
                "spring.jpa.properties.hibernate.session.events.auto="
                        + "uk.gov.hmcts.appregister.audit.service."
                        + "DataAuditPersistenceBatchingIntegrationTest$BatchExecutionCounter")
class DataAuditPersistenceBatchingIntegrationTest extends BaseRepositoryTest {
    private static final int AUDIT_COUNT = 205;

    @Autowired private DataAuditPersistenceService persistenceService;
    @Autowired private DataAuditRepository dataAuditRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String databaseSchema;

    @Test
    void givenMoreThanTwoAuditBatches_whenPersisted_thenExecuteThreeJdbcBatches() {
        val audits =
                IntStream.range(0, AUDIT_COUNT)
                        .mapToObj(index -> new DataAuditTestData().someMinimal().build())
                        .toList();
        final long initialAuditCount = dataAuditRepository.count();
        // Anchor expected IDs without relying on pg_sequences, which reports cache boundaries.
        final long initialSequenceValue = nextSequenceValue();
        BatchExecutionCounter.reset();

        persistenceService.persist(audits);

        assertThat(BatchExecutionCounter.count()).isEqualTo(3);
        assertThat(dataAuditRepository.count() - initialAuditCount).isEqualTo(AUDIT_COUNT);
        assertThat(audits)
                .extracting(DataAudit::getId)
                .containsExactlyElementsOf(
                        LongStream.rangeClosed(
                                        initialSequenceValue + 1,
                                        initialSequenceValue + AUDIT_COUNT)
                                .boxed()
                                .toList());
    }

    private long nextSequenceValue() {
        return jdbcTemplate.queryForObject(
                "select nextval(?::regclass)", Long.class, databaseSchema + ".add_dataaudit_event");
    }

    public static class BatchExecutionCounter implements SessionEventListener {
        @Serial private static final long serialVersionUID = 1L;

        private static final AtomicInteger BATCH_EXECUTIONS = new AtomicInteger();

        @Override
        public void jdbcExecuteBatchStart() {
            // Hibernate invokes this immediately before PreparedStatement.executeBatch().
            BATCH_EXECUTIONS.incrementAndGet();
        }

        static void reset() {
            BATCH_EXECUTIONS.set(0);
        }

        static int count() {
            return BATCH_EXECUTIONS.get();
        }
    }
}
