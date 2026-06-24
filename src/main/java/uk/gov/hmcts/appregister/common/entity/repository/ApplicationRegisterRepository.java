package uk.gov.hmcts.appregister.common.entity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.appregister.common.entity.ApplicationRegister;

/**
 * Repository interface for managing ApplicationRegister entities.
 */
public interface ApplicationRegisterRepository extends JpaRepository<ApplicationRegister, Long> {}
