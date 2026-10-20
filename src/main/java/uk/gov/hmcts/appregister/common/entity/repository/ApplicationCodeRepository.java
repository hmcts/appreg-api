package uk.gov.hmcts.appregister.common.entity.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;

/**
 * Selects the latest effective start-date group, then its highest-ID version. An expired winner
 * never revives a superseded version or an earlier group.
 */
@Repository
public interface ApplicationCodeRepository extends JpaRepository<ApplicationCode, Long> {

    /**
     * Finds the latest version of the effective period covering the supplied date.
     *
     * @param applicationCode the application code to search for
     * @return the selected version, or empty if the selected period has expired or not yet started
     */
    @Query(
            """
            SELECT c
            FROM ApplicationCode c
            WHERE LOWER(c.code) = LOWER(CAST(:applicationCode AS string))
              AND c.id = (
                  SELECT MAX(v.id) FROM ApplicationCode v
                  WHERE LOWER(v.code) = LOWER(c.code)
                    AND v.startDate = c.startDate
              )
              AND c.startDate = (
                  SELECT MAX(v.startDate) FROM ApplicationCode v
                  WHERE LOWER(v.code) = LOWER(c.code)
                    AND v.startDate <= :dateTime
              )
              AND (c.endDate IS NULL OR c.endDate >= :dateTime)
            """)
    Optional<ApplicationCode> findByCodeAndDate(String applicationCode, LocalDate dateTime);

    /** Returns eligible effective-period winners for each code, ordered by code. */
    @Query(
            """
            SELECT c
            FROM ApplicationCode c
            WHERE c.id = (
                SELECT MAX(v.id) FROM ApplicationCode v
                WHERE LOWER(v.code) = LOWER(c.code)
                  AND v.startDate = c.startDate
            )
            AND c.startDate = (
                SELECT MAX(v.startDate) FROM ApplicationCode v
                WHERE LOWER(v.code) = LOWER(c.code)
                  AND v.startDate <= :date
            )
            AND (c.endDate IS NULL OR c.endDate >= :date)
            ORDER BY LOWER(c.code)
            """)
    List<ApplicationCode> findAllByDate(@Param("date") LocalDate date);

    /**
     * Retrieve a page of eligible effective-period winners, filtered by code/title. Filtering and
     * pagination apply to selected versions, not superseded history.
     *
     * <p>Active if: c.startDate <= :date AND (c.endDate IS NULL OR c.endDate >= :date)
     *
     * @param code optional partial code filter (case-insensitive)
     * @param title optional partial title filter (case-insensitive)
     * @param date date to evaluate "active" on
     * @param pageable paging/sorting
     * @return page of matching entities
     */
    @Query(
            """
        SELECT c
        FROM ApplicationCode c
        WHERE (:code IS NULL OR LOWER(c.code)  LIKE CONCAT('%', LOWER( CAST(:code AS string)), '%')  ESCAPE '\\')
          AND (:title IS NULL OR LOWER(c.title) LIKE CONCAT('%', LOWER( CAST(:title AS string)), '%')  ESCAPE '\\')
          AND c.id = (
              SELECT MAX(v.id) FROM ApplicationCode v
              WHERE LOWER(v.code) = LOWER(c.code)
                AND v.startDate = c.startDate
          )
          AND c.startDate = (
              SELECT MAX(v.startDate) FROM ApplicationCode v
              WHERE LOWER(v.code) = LOWER(c.code)
                AND v.startDate <= :date
          )
          AND (c.endDate IS NULL OR c.endDate >= :date)
        """)
    Page<ApplicationCode> search(
            @Param("code") String code,
            @Param("title") String title,
            @Param("date") LocalDate date,
            Pageable pageable);
}
