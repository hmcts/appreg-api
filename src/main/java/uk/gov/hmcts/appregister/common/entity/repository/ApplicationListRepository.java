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
import uk.gov.hmcts.appregister.common.entity.aspect.LikeParam;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.projection.ApplicationListSummaryProjection;

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
    Optional<ApplicationList> findByIdAndCreatedUser(Long primaryKey, String userId);

    /**
     * Check if an ApplicationList entity exists by its ID and associated user.
     *
     * @param primaryKey the PK of the ApplicationList
     * @param userId the ID of the user
     * @return true if the ApplicationList exists, false otherwise
     */
    boolean existsByIdAndCreatedUser(Long primaryKey, String userId);

    /**
     * Finds all ApplicationCode entities with an ID greater than or equal to the specified value.
     *
     * @param value the minimum ID value (inclusive)
     * @return a list of ApplicationCode entities with IDs greater than or equal to the specified
     *     value
     */
    List<ApplicationList> findByIdGreaterThanEqual(Integer value);

    /**
     * Finds a non-soft deleted application list by its UUID.
     *
     * @param id An id to look up
     * @return A single matching application entry
     */
    @Query(
            """
        SELECT al
        FROM ApplicationList al
        WHERE al.uuid = :id
          AND (al.deleted IS NULL OR al.deleted <> 'Y')
        """)
    Optional<ApplicationList> findByUuid(UUID id);

    /**
     * Finds a non-soft deleted application list by its UUID. Does not exclude deleted entries.
     *
     * @param id An id to look up
     * @return A single matching application entry
     */
    @Query(
            """
        SELECT al
        FROM ApplicationList al
        WHERE al.uuid = :id
        """)
    Optional<ApplicationList> findByUuidIncludingDelete(UUID id);

    /**
     * Retrieves a paginated list of {@link ApplicationList} entities filtered by the specified
     * criteria, including status, court code, criminal justice area (CJA), date, time, and
     * description fields. The query uses an {@link EntityGraph} to eagerly fetch the associated
     * {@link CriminalJusticeArea} to prevent N+1 select issues.
     *
     * <p>All filter parameters are optional; if a parameter is {@code null}, it will be ignored in
     * the filtering process.
     *
     * <p>Can filter by minute while ignoring seconds. match times >= start and < end. Special case:
     * if end is midnight, the service passes a flag to indicate that only the >= start condition
     * should be applied because the < end condition would return nothing.
     *
     * @param status the application list status to filter by, or {@code null} to include all
     *     statuses
     * @param courtCode the court code to filter by, or {@code null} to include all court codes
     * @param cja the criminal justice area to filter by, or {@code null} to include all areas
     * @param onDate the specific date to filter by, or {@code null} to include all dates
     * @param start the start of the minute-based time range, or {@code null} for no time filter
     * @param end the exclusive upper bound of the minute range, or {@code null} for no time filter
     * @param wrapsMidnight a flag indicating whether the computed minute range crosses midnight
     *     (e.g., 23:59 -> 00:00).
     * @param description the description text to search within application descriptions, or {@code
     *     null} for no filter
     * @param otherDesc the text to search within the {@code otherLocation} field, or {@code null}
     *     for no filter
     * @param pageable the pagination and sorting information
     * @return a {@link Page} of {@link ApplicationList} entities matching the provided filter
     *     criteria
     */
    @Query(
            """
        SELECT
          al.uuid AS uuid,
          al.time AS time,
          al.date AS date,
          al.courtName AS courtName,
          al.description AS description,
          cja.description AS cjaDescription,
          al.otherLocation AS otherLocation,
          al.status AS status,
            (
            select count(ale2.id)
            from ApplicationListEntry ale2
            where ale2.applicationList = al
              and (ale2.deleted is null or ale2.deleted <> 'Y')
          ) as entryCount,
          LOWER(TRIM(COALESCE(al.courtName, cja.description, al.otherLocation))) AS effectiveLocation
        FROM ApplicationList al
        LEFT JOIN al.cja cja
        WHERE (:status IS NULL OR al.status = :status)
          AND (:courtCode IS NULL OR LOWER(al.courtCode) = LOWER(cast(:courtCode AS STRING)))
          AND (:cja IS NULL OR al.cja = :cja)
          AND (al.date = COALESCE(:onDate, al.date))
          AND (
               COALESCE(:start, NULL) IS NULL
                OR (
                     (:wrapsMidnight = TRUE  AND al.time >= :start)
                  OR (:wrapsMidnight = FALSE AND al.time >= :start AND al.time < :end)
                )
              )
          AND (:description IS NULL OR lower(al.description)
                  LIKE concat('%', lower(cast(:description AS string)), '%') ESCAPE '\\')
          AND (:otherDesc IS NULL OR lower(al.otherLocation)
                  LIKE concat('%', lower(cast(:otherDesc AS string)), '%') ESCAPE '\\')
          AND (al.deleted IS NULL OR al.deleted <> 'Y')
        """)
    Page<ApplicationListSummaryProjection> findAllByFilter(
            @Param("status") Status status,
            @Param("courtCode") String courtCode,
            @Param("cja") CriminalJusticeArea cja,
            @Param("onDate") LocalDate onDate,
            @Param("start") LocalTime start,
            @Param("end") LocalTime end,
            @Param("wrapsMidnight") boolean wrapsMidnight,
            @LikeParam @Param("description") String description,
            @LikeParam @Param("otherDesc") String otherDesc,
            Pageable pageable);
}
