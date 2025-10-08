package uk.gov.hmcts.appregister.common.entity.repository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;

/**
 * Repository interface for managing ApplicationList entities.
 */
public interface ApplicationListRepository extends JpaRepository<ApplicationList, Long> {

    /**
     * Find all ApplicationList entities associated with a specific user.
     *
     * @param userId the ID of the user
     * @return a list of ApplicationList entities
     */
    List<ApplicationList> findAllByCreatedUser(String userId);

    /**
     * Find an ApplicationList entity by its ID and associated user.
     *
     * @param primaryKey the PK of the ApplicationList
     * @param userId the ID of the user
     * @return an Optional containing the ApplicationList if found, or empty if not found
     */
    Optional<ApplicationList> findByPkAndCreatedUser(Long primaryKey, String userId);

    /**
     * Check if an ApplicationList entity exists by its ID and associated user.
     *
     * @param primaryKey the PK of the ApplicationList
     * @param userId the ID of the user
     * @return true if the ApplicationList exists, false otherwise
     */
    boolean existsByPkAndCreatedUser(Long primaryKey, String userId);

    /**
     * Finds all ApplicationCode entities with an ID greater than or equal to the specified value.
     *
     * @param value the minimum ID value (inclusive)
     * @return a list of ApplicationCode entities with IDs greater than or equal to the specified
     *     value
     */
    List<ApplicationList> findByPkGreaterThanEqual(Integer value);

    @Query("""
    SELECT al
    FROM ApplicationList al
    WHERE (:status IS NULL OR al.status = :status)
      AND (:courtCode IS NULL OR al.courtCode = :courtCode)
      AND (:cja IS NULL OR al.cja = :cja)
      AND (:dateMidnight IS NULL OR al.date = :dateMidnight)
      AND (:time IS NULL OR (hour(al.time) = hour(:time) AND minute(al.time) = minute(:time)))
      AND (:description IS NULL OR LOWER(al.description) LIKE CONCAT('%', LOWER(CAST(:description AS string)), '%'))
      AND (:otherDesc IS NULL OR LOWER(al.otherLocation) LIKE CONCAT('%', LOWER(CAST(:otherDesc AS string)), '%'))
        """)
    Page<ApplicationList> findAllByFilter(
        @Param("status") ApplicationListStatus status,
        @Param("courtCode") String courtLocationCode,
        @Param("cja") CriminalJusticeArea cja,
        @Param("dateMidnight") LocalDateTime date,   // pass date at 00:00:00
        @Param("time") LocalDateTime time,                 // HH:mm (no seconds)
        @Param("description") String descriptionContains,
        @Param("otherDesc") String otherLocationDescriptionContains,
        Pageable pageable);
}
