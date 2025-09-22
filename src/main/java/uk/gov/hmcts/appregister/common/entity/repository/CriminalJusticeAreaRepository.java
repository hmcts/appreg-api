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
}
