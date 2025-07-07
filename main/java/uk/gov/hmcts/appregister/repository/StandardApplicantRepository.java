package uk.gov.hmcts.appregister.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.model.StandardApplicant;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Repository
public interface StandardApplicantRepository extends JpaRepository<StandardApplicant, Long> {
    Optional<StandardApplicant> findByApplicantCode(String applicantCode);
}
