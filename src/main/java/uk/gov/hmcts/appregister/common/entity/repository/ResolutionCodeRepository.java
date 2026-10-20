package uk.gov.hmcts.appregister.common.entity.repository;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;

/**
 * Selects the latest effective start-date group, then its highest-ID version, without fallback.
 */
public interface ResolutionCodeRepository extends JpaRepository<ResolutionCode, Long> {
    /** Returns the effective-period winner only if it has not expired. */
    @Query(
            """
        SELECT rc
        FROM ResolutionCode rc
        WHERE LOWER(rc.resultCode) = LOWER(CAST(:code AS string))
        AND rc.id = (
            SELECT MAX(v.id) FROM ResolutionCode v
            WHERE LOWER(v.resultCode) = LOWER(rc.resultCode)
              AND v.startDate = rc.startDate
        )
        AND rc.startDate = (
            SELECT MAX(v.startDate) FROM ResolutionCode v
            WHERE LOWER(v.resultCode) = LOWER(rc.resultCode)
              AND v.startDate <= :date
        )
        AND (rc.endDate IS NULL OR rc.endDate >= :date)
        """)
    Optional<ResolutionCode> findActiveResolutionCodesByCodeAndDate(
            @Param("code") String code, @Param("date") LocalDate date);

    /** Filters and paginates eligible effective-period winners. */
    @Query(
            """
        SELECT rc
        FROM ResolutionCode rc
        WHERE (:code IS NULL OR LOWER(rc.resultCode) LIKE CONCAT('%', LOWER(CAST(:code AS string)), '%'))
        AND (:title IS NULL OR LOWER(rc.title) LIKE CONCAT('%', LOWER(CAST(:title AS string)), '%'))
        AND rc.id = (
            SELECT MAX(v.id) FROM ResolutionCode v
            WHERE LOWER(v.resultCode) = LOWER(rc.resultCode)
              AND v.startDate = rc.startDate
        )
        AND rc.startDate = (
            SELECT MAX(v.startDate) FROM ResolutionCode v
            WHERE LOWER(v.resultCode) = LOWER(rc.resultCode)
              AND v.startDate <= :date
        )
        AND (rc.endDate IS NULL OR rc.endDate >= :date)
        """)
    Page<ResolutionCode> findActiveOnDate(
            @Param("code") String code,
            @Param("title") String title,
            @Param("date") LocalDate date,
            Pageable pageable);
}
