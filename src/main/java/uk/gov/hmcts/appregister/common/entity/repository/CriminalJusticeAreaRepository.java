package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;

/** Repository interface for managing ApplicationCode entities. */
@Repository
public interface CriminalJusticeAreaRepository extends JpaRepository<CriminalJusticeArea, Long> {
    /**
     * gets possibly many criminal justice areas by code.
     *
     * @param code The code to search for
     * @return one or many criminal justice areas
     */
    List<CriminalJusticeArea> findByCjaCode(String code);

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
