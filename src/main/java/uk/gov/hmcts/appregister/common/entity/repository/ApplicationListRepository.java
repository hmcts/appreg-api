package uk.gov.hmcts.appregister.common.entity.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    /**
     * Finds all entities with the given IDs.
     *
     * @param ids An id to look up
     * @return A single matching application entry
     */
    Optional<ApplicationList> findByUuid(UUID ids);

    /**
     * Retrieves a paginated list of {@link ApplicationList} entities filtered by the specified
     * criteria, including status, court code, criminal justice area (CJA), date, time, and
     * description fields. The query uses an {@link EntityGraph} to eagerly fetch the associated
     * {@link CriminalJusticeArea} to prevent N+1 select issues.
     *
     * <p>All filter parameters are optional; if a parameter is {@code null}, it will be ignored in
     * the filtering process.
     *
     * @param status the application list status to filter by, or {@code null} to include all
     *     statuses
     * @param courtCode the court code to filter by, or {@code null} to include all court codes
     * @param cja the criminal justice area to filter by, or {@code null} to include all areas
     * @param onDate the specific date to filter by, or {@code null} to include all dates
     * @param atTime the specific time to filter by, or {@code null} to include all times
     * @param description the description text to search within application descriptions, or {@code
     *     null} for no filter
     * @param otherDesc the text to search within the {@code otherLocation} field, or {@code null}
     *     for no filter
     * @param pageable the pagination and sorting information
     * @return a {@link Page} of {@link ApplicationList} entities matching the provided filter
     *     criteria
     */
    @EntityGraph(attributePaths = "cja")
    @Query(
            """
        SELECT al
        FROM ApplicationList al
        WHERE (:status IS NULL OR al.status = :status)
          AND (:courtCode IS NULL OR al.courtCode = :courtCode)
          AND (:cja IS NULL OR al.cja = :cja)
          AND (al.date = COALESCE(:onDate, al.date))
          AND (al.time = COALESCE(:atTime, al.time))
          AND (:description IS NULL OR lower(al.description) LIKE concat('%', lower(cast(:description AS string)), '%'))
          AND (:otherDesc IS NULL OR lower(al.otherLocation) LIKE concat('%', lower(cast(:otherDesc AS string)), '%'))
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
