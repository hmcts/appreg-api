package uk.gov.hmcts.appregister.common.entity.repository;

import java.time.LocalDate;
import java.util.List;
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
     * Finds an ApplicationCode entity by its application code.
     *
     * @param applicationCode the application code to search for
     * @return an Optional containing the found ApplicationCode, or empty if not found
     */
    @Query(
            """
            SELECT c
            FROM ApplicationCode c
            WHERE c.code = :applicationCode
              AND c.startDate <= :dateTime
              AND (c.endDate IS NULL OR c.endDate >= :dateTime)
            """)
    List<ApplicationCode> findByCodeAndDate(String applicationCode, LocalDate dateTime);

    /**
     * Finds all ApplicationCode entities with an ID greater than or equal to the specified value.
     *
     * @param value the minimum ID value (inclusive)
     * @return a list of ApplicationCode entities with IDs greater than or equal to the specified
     *     value
     */
    List<ApplicationCode> findByIdGreaterThanEqual(Integer value);

    /**
     * Retrieve a page of active Application Codes filtered by code/title (case-insensitive).
     *
     * <p>Active if: c.startDate < :date AND (c.endDate IS NULL OR c.endDate >= :date)
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
        WHERE (:code IS NULL OR LOWER(c.code)  LIKE CONCAT('%', LOWER( CAST(:code AS string)), '%'))
          AND (:title IS NULL OR LOWER(c.title) LIKE CONCAT('%', LOWER( CAST(:title AS string)), '%'))
          AND c.startDate < :date
          AND (c.endDate IS NULL OR c.endDate >= :date)
        """)
    Page<ApplicationCode> search(
            @Param("code") String code,
            @Param("title") String title,
            @Param("date") LocalDate date,
            Pageable pageable);
}
