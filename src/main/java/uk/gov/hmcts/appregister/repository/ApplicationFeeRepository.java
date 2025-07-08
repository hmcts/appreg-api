package uk.gov.hmcts.appregister.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.appregister.model.ApplicationFee;

public interface ApplicationFeeRepository extends JpaRepository<ApplicationFee, Long> {
    List<ApplicationFee> findByReference(String reference);

    List<ApplicationFee> findByReferenceAndIsOffset(String reference, boolean isOffset);
}
