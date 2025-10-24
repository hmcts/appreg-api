package uk.gov.hmcts.appregister.common.entity.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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
     * Searches for application codes based on some filter criteria.
     *
     * @param code The code to find
     * @param title The title
     * @param pageable The pagaeable data to further the results
     * @return The list of application codes in page format
     */
    @Query(
            """
        SELECT c
        FROM ApplicationCode c
        WHERE (:code  IS NULL OR lower(c.code)  LIKE concat('%',
                    lower(cast(:code  as string)), '%'))
        AND (:title  IS NULL OR lower(c.title)  LIKE concat('%',
            lower(cast(:title  as string)), '%'))
        """)
    Page<ApplicationCode> search(
            @Param("code") String code, @Param("title") String title, Pageable pageable);
}
