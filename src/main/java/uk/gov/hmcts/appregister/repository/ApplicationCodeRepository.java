package uk.gov.hmcts.appregister.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.model.ApplicationCode;

@Repository
public interface ApplicationCodeRepository extends JpaRepository<ApplicationCode, Long> {
    Optional<ApplicationCode> findByApplicationCode(String applicationCode);
}
