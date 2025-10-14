package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.ApplicationRegister;

@Repository
public interface ApplicationRegisterRepository extends JpaRepository<ApplicationRegister, Long> {
    /**
     * Finds all CriminalJusticeArea entities with an ID greater than or equal to the specified
     * value.
     *
     * @param value the minimum ID value (inclusive)
     * @return a list of CriminalJusticeArea entities with IDs greater than or equal to the
     *     specified value
     */
    List<ApplicationRegister> findByIdGreaterThanEqual(Integer value);
}
