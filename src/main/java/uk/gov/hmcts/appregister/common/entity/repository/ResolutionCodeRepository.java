package uk.gov.hmcts.appregister.common.entity.repository;

import java.time.LocalDate;
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
     * Find active Resolution Codes by code on a given date.
     *
     * <p>Active if: {@code rc.startDate <= :date} and ({@code rc.endDate IS NULL} or {@code
     * rc.endDate >= :date}). Code match is case-insensitive equality on {@code rc.resultCode}.
     *
     * @param code case-insensitive business code (e.g. "ABC123")
     * @param date local date to check for active codes
     * @return zero, one, or many rows; service layer enforces uniqueness
     */
    @Query(
            """
        SELECT rc
        FROM ResolutionCode rc
        WHERE LOWER(rc.resultCode) = LOWER(CAST(:code AS string))
        AND rc.startDate <= :date
        AND (rc.endDate IS NULL OR rc.endDate >= :date)
        ORDER BY CASE WHEN rc.endDate IS NULL THEN 0 ELSE 1 END,
                 rc.endDate DESC,
                 rc.startDate DESC,
                 rc.id DESC
        """)
    List<ResolutionCode> findActiveResolutionCodesByCodeAndDate(
            @Param("code") String code, @Param("date") LocalDate date);

    /**
     * Retrieve a page of active Resolution Codes filtered by code/title (case-insensitive).
     *
     * <p>Active if: rc.startDate <= :asOfDate AND (rc.endDate IS NULL OR rc.endDate >= :asOfDate)
     *
     * @param code optional partial code filter (case-insensitive)
     * @param title optional partial title filter (case-insensitive)
     * @param date date to evaluate "active" on
     * @param pageable paging/sorting
     * @return page of matching entities
     */
    @Query(
            """
        SELECT rc
        FROM ResolutionCode rc
        WHERE (:code IS NULL OR LOWER(rc.resultCode) LIKE CONCAT('%', LOWER( CAST(:code as string)), '%'))
        AND (:title IS NULL OR LOWER(rc.title) LIKE CONCAT('%', LOWER( CAST(:title as string)), '%'))
        AND rc.startDate <= :date
        AND (rc.endDate IS NULL OR rc.endDate >= :date)
        """)
    Page<ResolutionCode> findActiveOnDate(
            @Param("code") String code,
            @Param("title") String title,
            @Param("date") LocalDate date,
            Pageable pageable);

    /**
     * Finds active {@link ResolutionCode} records for the given result code (case-insensitive),
     * prioritising open-ended rows where {@code endDate IS NULL}.
     *
     * @param resultCode the result code to match (case-insensitive)
     * @return a list of active resolution codes ordered with open-ended rows first
     */
    default List<ResolutionCode> findPrioritisingNullEndDate(String resultCode, LocalDate date) {
        return findActiveByResultCodeIgnoreCaseOrdered(resultCode, date);
    }

    /**
     * Finds active {@link ResolutionCode} records for the given result code (case-insensitive).
     *
     * <p>An active resolution code is defined as:
     *
     * <ul>
     *   <li>{@code startDate <= :date}
     *   <li>{@code endDate IS NULL OR endDate >= :date}
     * </ul>
     *
     * @param resultCode the result code to match (case-insensitive)
     * @param date active date to evaluate against
     * @param pageable paging/sorting
     * @return a list of active resolution codes matching the given result code
     */
    @Query(
            """
        select rc
        from ResolutionCode rc
        where lower(rc.resultCode) = lower(:resultCode)
        and rc.startDate <= :date
        and (rc.endDate is null or rc.endDate >= :date)
        """)
    List<ResolutionCode> findActiveByResultCodeIgnoreCase(
            @Param("resultCode") String resultCode,
            @Param("date") LocalDate date,
            Pageable pageable);

    @Query(
            """
        select rc
        from ResolutionCode rc
        where lower(rc.resultCode) = lower(:resultCode)
        and rc.startDate <= :date
        and (rc.endDate is null or rc.endDate >= :date)
        order by case when rc.endDate is null then 0 else 1 end,
                 rc.endDate desc,
                 rc.startDate desc,
                 rc.id desc
        """)
    List<ResolutionCode> findActiveByResultCodeIgnoreCaseOrdered(
            @Param("resultCode") String resultCode, @Param("date") LocalDate date);
}
