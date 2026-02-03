package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.DataAudit;

@Repository
public interface DataAuditRepository extends JpaRepository<DataAudit, Long> {
    /**
     * Finds all ApplicationCode entities with an ID greater than or equal to the specified value.
     *
     * @param value the minimum ID value (inclusive)
     * @return a list of ApplicationCode entities with IDs greater than or equal to the specified
     *     value
     */
    List<DataAudit> findByIdGreaterThanEqual(Integer value);

    /**
     * finds a unique record for a data audit record with an old value
     * @param table The table
     * @param column The column
     * @param oldValue The old value
     */
    @Query("SELECT da FROM DataAudit da " +
        "WHERE da.tableName = :table " +
        "AND da.columnName = :column " +
        "AND da.oldValue = :oldValue")
    Optional<DataAudit> findDataAuditForTableAndColumnAndOldValue(String table,
                                                                  String column,
                                                                  String oldValue);

    /**
     * finds a unique record for a data audit record with a new value
     * @param table The table
     * @param column The column
     * @param oldValue The new value
     */
    @Query("SELECT da FROM DataAudit da " +
        "WHERE da.tableName = :table " +
        "AND da.columnName = :column " +
        "AND da.newValue = :oldValue")
    Optional<DataAudit> findDataAuditForTableAndColumnAndNewValue(String table,
                                                                  String column,
                                                                  String oldValue);

    /**
     * finds a unique record for a data audit record
     * @param table The table
     * @param column The column
     * @param oldValue The old value
     * @param newValue The new value
     */
    @Query("SELECT da FROM DataAudit da " +
        "WHERE da.tableName = :table " +
        "AND da.columnName = :column " +
        "AND da.oldValue = :oldValue " +
        "AND da.newValue = :newValue")
    Optional<DataAudit> findDataAuditForTableAndColumnAndOldValueAndNewValue(String table,
                                                                  String column,
                                                                  String oldValue,
                                                                  String newValue);
}
