package uk.gov.hmcts.appregister.common.entity.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;

/**
 * Spring Data JPA repository for {@link ResolutionCode} entities.
 *
 * <p>Provides access to Resolution Codes stored in the {@code result_codes} table. Includes custom
 * queries for retrieving active result codes by code/date and paginated searches with optional
 * filters.
 */
public interface ResolutionCodeRepository extends JpaRepository<ResolutionCode, Long> {

    List<ResolutionCode> findByIdGreaterThanEqual(Integer value);

    /**
     * Find active Resolution Codes by code on a given date window.
     *
     * <p>Active if: {@code rc.startDate < :tomorrowStart} and ({@code rc.endDate IS NULL} or {@code
     * rc.endDate >= :todayStart}). Code match is case-insensitive equality on {@code
     * rc.resultCode}.
     *
     * @param code case-insensitive business code (e.g. "ABC123")
     * @param todayStart inclusive window start (local day start)
     * @param tomorrowStart exclusive window end (next day start)
     * @return zero, one, or many rows; service layer enforces uniqueness
     */
    @Query(
            """
          SELECT rc
          FROM ResolutionCode rc
          WHERE LOWER(rc.resultCode) = LOWER(CAST(:code AS string))
          AND rc.startDate < :tomorrowStart
          AND (rc.endDate IS NULL OR rc.endDate >= :todayStart)
          """)
    List<ResolutionCode> findActiveResolutionCodesOnDateByCode(
            @Param("code") String code,
            @Param("todayStart") LocalDateTime todayStart,
            @Param("tomorrowStart") LocalDateTime tomorrowStart);

    /**
     * Retrieve a page of active Resolution Codes filtered by code/title (case-insensitive).
     *
     * <p>Filters (applied when non-null):
     *
     * <ul>
     *   <li>{@code code}: partial match on {@code rc.resultCode}
     *   <li>{@code title}: partial match on {@code rc.title}
     * </ul>
     *
     * <p>Active if: {@code rc.startDate < :tomorrowStart} and ({@code rc.endDate IS NULL} or {@code
     * rc.endDate >= :todayStart}).
     *
     * @param code optional partial code filter (case-insensitive)
     * @param title optional partial title filter (case-insensitive)
     * @param todayStart inclusive window start (local day start)
     * @param tomorrowStart exclusive window end (next day start)
     * @param pageable paging/sorting
     * @return page of matching entities
     */
    @Query(
            """
         SELECT rc
         FROM ResolutionCode rc
         WHERE (:code IS NULL OR LOWER(rc.resultCode) LIKE CONCAT('%', LOWER(CAST(:code AS string)), '%'))
         AND (:title IS NULL OR LOWER(rc.title) LIKE CONCAT('%', LOWER(CAST(:title as string)), '%'))
         AND rc.startDate < :tomorrowStart
         AND (rc.endDate IS NULL OR rc.endDate >= :todayStart)
         """)
    Page<ResolutionCode> findActiveOnDate(
            @Param("code") String code,
            @Param("title") String title,
            @Param("todayStart") LocalDateTime todayStart,
            @Param("tomorrowStart") LocalDateTime tomorrowStart,
            Pageable pageable);
}
