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
 * Repository interface for managing ApplicationCode entities.
 */
@Repository
public interface ApplicationCodeRepository extends JpaRepository<ApplicationCode, Long> {

    /**
     * Finds the highest-ID version of a code valid on the supplied date.
     *
     * @param applicationCode the application code to search for
     * @return the selected version, or empty if no version is valid
     */
    @Query(
            """
            SELECT c
            FROM ApplicationCode c
            WHERE LOWER(c.code) = LOWER(CAST(:applicationCode AS string))
              AND c.id = (
                  SELECT MAX(v.id) FROM ApplicationCode v
                  WHERE LOWER(v.code) = LOWER(c.code)
                    AND v.startDate <= :dateTime
                    AND (v.endDate IS NULL OR v.endDate >= :dateTime)
              )
            """)
    Optional<ApplicationCode> findByCodeAndDate(String applicationCode, LocalDate dateTime);

    /** Returns the highest-ID date-valid version of each code, ordered by code. */
    @Query(
            """
            SELECT c
            FROM ApplicationCode c
            WHERE c.id = (
                SELECT MAX(v.id) FROM ApplicationCode v
                WHERE LOWER(v.code) = LOWER(c.code)
                  AND v.startDate <= :date
                  AND (v.endDate IS NULL OR v.endDate >= :date)
            )
            ORDER BY LOWER(c.code)
            """)
    List<ApplicationCode> findAllByDate(@Param("date") LocalDate date);

    /**
     * Retrieve a page of the highest-ID date-valid versions, filtered by code/title. Filtering and
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
                AND v.startDate <= :date
                AND (v.endDate IS NULL OR v.endDate >= :date)
          )
        """)
    Page<ApplicationCode> search(
            @Param("code") String code,
            @Param("title") String title,
            @Param("date") LocalDate date,
            Pageable pageable);
}
