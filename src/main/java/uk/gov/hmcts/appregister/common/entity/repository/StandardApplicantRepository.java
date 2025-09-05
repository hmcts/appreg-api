package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
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
     * Finds all ApplicationCode entities with IDs greater than or equal to the specified value.
     *
     * @param value the minimum ID value
     * @return a list of ApplicationCode entities with IDs >= value
     */
    List<ApplicationCode> findByIdGreaterThanEqual(Integer value);
}
