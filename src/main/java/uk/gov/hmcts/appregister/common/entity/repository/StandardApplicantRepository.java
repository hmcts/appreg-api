package uk.gov.hmcts.appregister.common.entity.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;

/** Repository for StandardApplicant entities. */
@Repository
public interface StandardApplicantRepository extends JpaRepository<StandardApplicant, Long> {

    /**
     * Finds a StandardApplicant by its applicant code.
     *
     * @param applicantCode the applicant code to search for
     * @return an Optional containing the found StandardApplicant, or empty if not found
     */
    Optional<StandardApplicant> findByApplicantCode(String applicantCode);

    /**
     * Finds the ids that are greater than this value.
     *
     * @param value the minimum ID value
     * @return a list of ApplicationCode entities with IDs >= value
     */
    List<StandardApplicant> findByIdGreaterThanEqual(Integer value);
}
