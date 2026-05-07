package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.aspect.LikeParam;

/**
 * Repository interface for managing ApplicationCode entities.
 */
@Repository
public interface CriminalJusticeAreaRepository extends JpaRepository<CriminalJusticeArea, Long> {
    /**
     * gets possibly many criminal justice areas by code.
     *
     * @param code The code to search for
     * @return one or many criminal justice areas
     */
    List<CriminalJusticeArea> findByCode(String code);

    /**
     * Searches for criminal justice area based on filter criteria.
     *
     * @param code The code to find
     * @param description The title
     */
    @Query(
            """
        SELECT c
        FROM CriminalJusticeArea c
        WHERE (:code IS NULL OR LOWER(c.code) = LOWER(cast(:code AS STRING)))
        AND (:description IS NULL OR LOWER(c.description) LIKE concat('%',
                LOWER(cast(:description AS STRING)), '%')  ESCAPE '\\')
        """)
    Page<CriminalJusticeArea> search(
            @Param("code") String code,
            @LikeParam @Param("description") String description,
            Pageable pageable);

    /**
     * Finds all CriminalJusticeArea entities with an ID greater than or equal to the specified
     * value.
     *
     * @param value the minimum ID value (inclusive)
     * @return a list of CriminalJusticeArea entities with IDs greater than or equal to the
     *     specified value
     */
    List<CriminalJusticeArea> findByIdGreaterThanEqual(Integer value);
}
