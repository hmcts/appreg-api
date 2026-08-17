package uk.gov.hmcts.appregister.controller.applicationentry;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.restassured.response.Response;
import jakarta.persistence.EntityManagerFactory;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.instancio.junit.InstancioExtension;
import org.instancio.junit.Seed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.audit.service.DataAuditPersistenceProperties;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.testutils.util.HeaderUtil;
import uk.gov.hmcts.appregister.util.CreateEntryDtoUtil;

/**
 * Records endpoint-level database and audit activity as a regression diagnostic. Hibernate's
 * prepared-statement count excludes direct JDBC work such as audit sequence prefetch queries.
 */
@Slf4j
@ExtendWith(InstancioExtension.class)
class ApplicationEndpointDatabaseUsageTest extends AbstractApplicationEntryCrudTest {
    private static final String AUDIT_ROWS_METRIC = "appreg.audit.persistence.rows.submitted";
    private static final long CREATE_ENTRY_STATEMENT_BUDGET = 123;
    private static final long UPDATE_ENTRY_STATEMENT_BUDGET = 41;
    private static final long ENTRY_DETAIL_STATEMENT_BUDGET = 16;
    private static final long LIST_DETAIL_STATEMENT_BUDGET = 6;

    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private MeterRegistry meterRegistry;
    @Autowired private DataAuditRepository dataAuditRepository;
    @Autowired private DataAuditPersistenceProperties auditPersistenceProperties;

    @Test
    @Seed(1719)
    void givenCreateEntry_whenDatabaseUsageIsMeasured_thenStayWithinStatementBudget()
            throws Exception {
        val token = createAdminToken().fetchTokenForRole();
        val listId = getOpenApplicationListId();
        val request = CreateEntryDtoUtil.getCorrectCreateEntryDto();
        val url = getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + listId + "/entries");

        val usage =
                measure(
                        "create entry",
                        201,
                        () -> restAssuredClient.executePostRequest(url, token, request));

        assertWithinStatementBudget(usage, CREATE_ENTRY_STATEMENT_BUDGET);
    }

    @Test
    @Seed(1719)
    void givenUpdateEntry_whenDatabaseUsageIsMeasured_thenStayWithinStatementBudget()
            throws Exception {
        val createdEntry = createListEntryWithAllData();
        awaitDataAudits();

        val token = createAdminToken().fetchTokenForRole();
        val request = getCorrectUpdateDataDto();
        request.setNumberOfRespondents(null);
        val url = HeaderUtil.getLocation(createdEntry);

        val usage =
                measure(
                        "update entry",
                        200,
                        () -> restAssuredClient.executePutRequest(url, token, request));

        assertWithinStatementBudget(usage, UPDATE_ENTRY_STATEMENT_BUDGET);
    }

    @Test
    void givenEntryDetailRequest_whenDatabaseUsageIsMeasured_thenStayWithinStatementBudget()
            throws Exception {
        val token = createAdminToken().fetchTokenForRole();
        val ids = getValidEntryForList(VALID_ENTRY_PK);
        val url = getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + ids[0] + "/entries/" + ids[1]);

        val usage =
                measure(
                        "get entry detail",
                        200,
                        () -> restAssuredClient.executeGetRequest(url, token));

        assertWithinStatementBudget(usage, ENTRY_DETAIL_STATEMENT_BUDGET);
    }

    @Test
    void
            givenApplicationListDetailRequest_whenDatabaseUsageIsMeasured_thenStayWithinStatementBudget()
                    throws Exception {
        val token = createAdminToken().fetchTokenForRole();
        val listId = getOpenApplicationListId();
        val url = getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + listId);

        val usage =
                measure(
                        "get application list detail",
                        200,
                        () -> restAssuredClient.executeGetRequest(url, token));

        assertWithinStatementBudget(usage, LIST_DETAIL_STATEMENT_BUDGET);
    }

    private DatabaseUsage measure(String operation, int expectedStatus, DatabaseOperation request)
            throws Exception {
        awaitDataAudits();
        final Set<Long> existingAuditIds =
                dataAuditRepository.findAll().stream()
                        .map(DataAudit::getId)
                        .collect(Collectors.toSet());
        final double initialAuditRows = submittedAuditRows();
        val statistics = getHibernateStatistics();
        statistics.clear();

        val response = request.execute();
        response.then().statusCode(expectedStatus);
        awaitDataAudits();

        val auditRows = Math.round(submittedAuditRows() - initialAuditRows);
        var usage =
                DatabaseUsage.from(
                        statistics, auditRows, auditPersistenceProperties.getBatchSize());
        val persistedAudits =
                dataAuditRepository.findAll().stream()
                        .filter(audit -> !existingAuditIds.contains(audit.getId()))
                        .toList();
        val auditOperations =
                persistedAudits.stream()
                        .collect(
                                Collectors.groupingBy(
                                        DataAudit::getUpdateType,
                                        () -> new EnumMap<>(CrudEnum.class),
                                        Collectors.counting()));
        val auditedTables =
                persistedAudits.stream()
                        .collect(
                                Collectors.groupingBy(
                                        DataAudit::getTableName,
                                        TreeMap::new,
                                        Collectors.counting()));
        usage = usage.withAuditDetails(auditOperations, auditedTables);

        assertThat(persistedAudits)
                .as("Every submitted audit row should be persisted during the integration test")
                .hasSize(Math.toIntExact(auditRows));
        log.info("Database usage for {}: {}", operation, usage);
        return usage;
    }

    private double submittedAuditRows() {
        return meterRegistry.get(AUDIT_ROWS_METRIC).counter().count();
    }

    private Statistics getHibernateStatistics() {
        val statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        return statistics;
    }

    private void assertWithinStatementBudget(DatabaseUsage usage, long statementBudget) {
        assertThat(usage.preparedStatements())
                .as("Prepared statement budget; measured usage: %s", usage)
                .isLessThanOrEqualTo(statementBudget);
    }

    @FunctionalInterface
    private interface DatabaseOperation {
        Response execute() throws Exception;
    }

    private record DatabaseUsage(
            long preparedStatements,
            long connections,
            long transactions,
            long entityLoads,
            long entityInserts,
            long entityUpdates,
            long entityDeletes,
            long collectionFetches,
            long auditRows,
            int configuredAuditBatchSize,
            long estimatedAuditJdbcBatches,
            Map<CrudEnum, Long> auditOperations,
            Map<String, Long> auditedTables) {
        private static DatabaseUsage from(
                Statistics statistics, long auditRows, int configuredAuditBatchSize) {
            return new DatabaseUsage(
                    statistics.getPrepareStatementCount(),
                    statistics.getConnectCount(),
                    statistics.getTransactionCount(),
                    statistics.getEntityLoadCount(),
                    statistics.getEntityInsertCount(),
                    statistics.getEntityUpdateCount(),
                    statistics.getEntityDeleteCount(),
                    statistics.getCollectionFetchCount(),
                    auditRows,
                    configuredAuditBatchSize,
                    divideRoundingUp(auditRows, configuredAuditBatchSize),
                    Map.of(),
                    Map.of());
        }

        private DatabaseUsage withAuditDetails(
                Map<CrudEnum, Long> auditOperations, Map<String, Long> auditedTables) {
            return new DatabaseUsage(
                    preparedStatements,
                    connections,
                    transactions,
                    entityLoads,
                    entityInserts,
                    entityUpdates,
                    entityDeletes,
                    collectionFetches,
                    auditRows,
                    configuredAuditBatchSize,
                    estimatedAuditJdbcBatches,
                    auditOperations,
                    auditedTables);
        }

        private static long divideRoundingUp(long dividend, int divisor) {
            return (dividend + divisor - 1) / divisor;
        }
    }
}
