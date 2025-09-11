package uk.gov.hmcts.appregister.common.entity.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;

/** Repository interface for managing ApplicationCode entities. */
@Repository
public interface ApplicationCodeRepository extends JpaRepository<ApplicationCode, Long> {

    /**
     * Finds an ApplicationCode entity by its application code.
     *
     * @param applicationCode the application code to search for
     * @return an Optional containing the found ApplicationCode, or empty if not found
     */
    Optional<ApplicationCode> findByApplicationCode(String applicationCode);

    /**
     * Finds all ApplicationCode entities with an ID greater than or equal to the specified value.
     *
     * @param value the minimum ID value (inclusive)
     * @return a list of ApplicationCode entities with IDs greater than or equal to the specified
     *     value
     */
    List<ApplicationCode> findByIdGreaterThanEqual(Integer value);
}
