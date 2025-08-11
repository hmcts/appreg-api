package uk.gov.hmcts.appregister.standardapplicant.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.standardapplicant.model.StandardApplicant;

@Repository
public interface StandardApplicantRepository extends JpaRepository<StandardApplicant, Long> {
    Optional<StandardApplicant> findByApplicantCode(String applicantCode);
}
