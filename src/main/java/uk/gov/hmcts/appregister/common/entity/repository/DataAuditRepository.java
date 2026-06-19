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
     * finds a unique record for a data audit record with an old value.
     *
     * @param table The table
     * @param column The column
     * @param oldValue The old value
     */
    @Query(
            "SELECT da FROM DataAudit da "
                    + "WHERE da.tableName = :table "
                    + "AND da.columnName = :column "
                    + "AND da.oldValue = :oldValue")
    Optional<DataAudit> findDataAuditForTableAndColumnAndOldValue(
            String table, String column, String oldValue);

    /**
     * finds a unique record for a data audit record with a new value.
     *
     * @param table The table
     * @param column The column
     * @param newValue The new value
     */
    @Query(
            "SELECT da FROM DataAudit da "
                    + "WHERE da.tableName = :table "
                    + "AND da.columnName = :column "
                    + "AND da.newValue = :newValue")
    List<DataAudit> findDataAuditListForTableAndColumnAndNewValue(
            String table, String column, String newValue);

    /**
     * finds a unique record for a data audit record with a new value.
     *
     * @param table The table
     * @param column The column
     * @param newValue The new value
     */
    @Query(
            "SELECT da FROM DataAudit da "
                    + "WHERE da.tableName = :table "
                    + "AND da.columnName = :column "
                    + "AND da.newValue = :newValue")
    Optional<DataAudit> findDataAuditForTableAndColumnAndNewValue(
            String table, String column, String newValue);

    /**
     * finds a unique record for a data audit record.
     *
     * @param table The table
     * @param column The column
     * @param oldValue The old value
     * @param newValue The new value
     */
    @Query(
            "SELECT da FROM DataAudit da "
                    + "WHERE da.tableName = :table "
                    + "AND da.columnName = :column "
                    + "AND da.oldValue = :oldValue "
                    + "AND da.newValue = :newValue")
    Optional<DataAudit> findDataAuditForTableAndColumnAndOldValueAndNewValue(
            String table, String column, String oldValue, String newValue);
}
