package uk.gov.hmcts.appregister.common.entity.repository;

import java.time.LocalDate;
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
     * Find active Resolution Codes by code on a given date.
     *
     * <p>Active if: {@code rc.startDate <= :date}
     * and ({@code rc.endDate IS NULL} or {@code rc.endDate >= :date}).
     * Code match is case-insensitive equality on {@code rc.resultCode}.
     *
     * @param code case-insensitive business code (e.g. "ABC123")
     * @param date local date to check for active codes
     * @return zero, one, or many rows; service layer enforces uniqueness
     */
    @Query("""
      SELECT rc
      FROM ResolutionCode rc
      WHERE LOWER(rc.resultCode) = LOWER(CAST(:code AS string))
      AND rc.startDate <= :date
      AND (rc.endDate IS NULL OR rc.endDate >= :date)
      """)
    List<ResolutionCode> findActiveResolutionCodesByCodeAndDate(
        @Param("code") String code,
        @Param("date") LocalDate date);

    /**
     * Retrieve a page of active Resolution Codes filtered by code/title (case-insensitive).
     *
     * Active if: rc.startDate < :asOfDate AND (rc.endDate IS NULL OR rc.endDate >= :asOfDate)
     *
     * @param code optional partial code filter (case-insensitive)
     * @param title optional partial title filter (case-insensitive)
     * @param date date to evaluate "active" on
     * @param pageable paging/sorting
     * @return page of matching entities
     */
    @Query("""
    SELECT rc
    FROM ResolutionCode rc
    WHERE (:code IS NULL OR LOWER(rc.resultCode) LIKE CONCAT('%', LOWER(:code), '%'))
      AND (:title IS NULL OR LOWER(rc.title) LIKE CONCAT('%', LOWER(:title), '%'))
      AND rc.startDate < :date
      AND (rc.endDate IS NULL OR rc.endDate >= :date)
    """)
    Page<ResolutionCode> findActiveOnDate(
        @Param("code") String code,
        @Param("title") String title,
        @Param("date") LocalDate date,
        Pageable pageable
    );
}
