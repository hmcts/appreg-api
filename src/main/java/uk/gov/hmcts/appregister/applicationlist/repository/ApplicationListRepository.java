package uk.gov.hmcts.appregister.applicationlist.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.appregister.applicationlist.model.ApplicationList;

public interface ApplicationListRepository extends JpaRepository<ApplicationList, Long> {
    List<ApplicationList> findAllByUserId(String userId);

    Optional<ApplicationList> findByIdAndUserId(Long id, String userId);

    boolean existsByIdAndUserId(Long id, String userId);
}
