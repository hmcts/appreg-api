package uk.gov.hmcts.appregister.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.model.ApplicationCode;

import java.util.Optional;

@Repository
public interface ApplicationCodeRepository extends JpaRepository<ApplicationCode, Long> {
    Optional<ApplicationCode> findByApplicationCode(String applicationCode);
}
