package uk.gov.hmcts.appregister.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.appregister.model.ApplicationList;

import java.util.List;
import java.util.Optional;

public interface ApplicationListRepository extends JpaRepository<ApplicationList, Long> {
    List<ApplicationList> findAllByUserId(String userId);
    Optional<ApplicationList> findByIdAndUserId(Long id, String userId);
    boolean existsByIdAndUserId(Long id, String userId);
}
