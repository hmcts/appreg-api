package uk.gov.hmcts.appregister.common.entity.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @EntityGraph(attributePaths = "cja")
    @Query(
            """
        SELECT al
        FROM ApplicationList al
        WHERE (:status     IS NULL OR al.status        = :status)
          AND (:courtCode  IS NULL OR al.courtCode     = :courtCode)
          AND (:cja        IS NULL OR al.cja           = :cja)
          AND (:onDate     IS NULL OR al.date          = :onDate)
          AND (:atTime     IS NULL OR al.time          = :atTime)
          AND (:description IS NULL OR lower(al.description)   LIKE concat('%', lower(:description), '%'))
          AND (:otherDesc   IS NULL OR lower(al.otherLocation) LIKE concat('%', lower(:otherDesc), '%'))
        """)
    Page<ApplicationList> findAllByFilter(
            @Param("status") ApplicationListStatus status,
            @Param("courtCode") String courtCode,
            @Param("cja") CriminalJusticeArea cja,
            @Param("onDate") LocalDate onDate,
            @Param("atTime") LocalTime atTime,
            @Param("description") String description,
            @Param("otherDesc") String otherDesc,
            Pageable pageable);
}
