package uk.gov.hmcts.appregister.common.entity.generator;

import java.io.Serial;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.val;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.IdentifierGenerator;

/**
 * Allocates audit IDs in blocks without changing the shared database sequence increment. Every
 * value is reserved with {@code nextval}, so direct sequence consumers remain safe.
 */
public class DataAuditIdGenerator implements IdentifierGenerator {
    @Serial private static final long serialVersionUID = 1L;

    static final int PREFETCH_SIZE = 100;
    private static final String SEQUENCE_NAME = "add_dataaudit_event";
    private static final String FETCH_IDS_SQL =
            "select nextval(?::regclass) from generate_series(1, ?)";

    // Most allocations are lock-free; only an empty queue enters the synchronized refill path.
    private final ConcurrentLinkedQueue<Long> availableIds = new ConcurrentLinkedQueue<>();
    private final String qualifiedSequenceName;

    public DataAuditIdGenerator(
            DataAuditGeneratedId annotation, GeneratorCreationContext creationContext) {
        val sqlContext = creationContext.getSqlStringGenerationContext();
        val sequenceName =
                new QualifiedSequenceName(
                        sqlContext.getDefaultCatalog(),
                        sqlContext.getDefaultSchema(),
                        sqlContext.toIdentifier(SEQUENCE_NAME));
        qualifiedSequenceName = sqlContext.format(sequenceName);
    }

    DataAuditIdGenerator(String qualifiedSequenceName) {
        this.qualifiedSequenceName = qualifiedSequenceName;
    }

    @Override
    public Long generate(SharedSessionContractImplementor session, Object object) {
        val id = availableIds.poll();
        return id == null ? refill(session) : id;
    }

    private synchronized Long refill(SharedSessionContractImplementor session) {
        // Another worker may have refilled while this worker waited for the monitor.
        val id = availableIds.poll();
        if (id != null) {
            return id;
        }

        val fetchedIds = session.doReturningWork(this::fetchIds);
        val firstId = fetchedIds.getFirst();
        availableIds.addAll(fetchedIds.subList(1, fetchedIds.size()));
        return firstId;
    }

    private ArrayList<Long> fetchIds(Connection connection) throws SQLException {
        var ids = new ArrayList<Long>(PREFETCH_SIZE);
        try (val statement = connection.prepareStatement(FETCH_IDS_SQL)) {
            statement.setString(1, qualifiedSequenceName);
            statement.setInt(2, PREFETCH_SIZE);
            try (val resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getLong(1));
                }
            }
        }

        if (ids.size() != PREFETCH_SIZE) {
            throw new SQLException(
                    "Expected "
                            + PREFETCH_SIZE
                            + " audit sequence values but received "
                            + ids.size());
        }
        return ids;
    }
}
