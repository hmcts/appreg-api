package uk.gov.hmcts.appregister.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.appregister.model.ApplicationResult;

import java.util.Optional;

public interface ApplicationResultRepository extends JpaRepository<ApplicationResult, Long> {
    Optional<ApplicationResult> findByApplicationId(Long applicationId);
    void deleteByIdAndApplicationId(Long resultId, Long applicationId);

    @Query("""
    SELECT r FROM ApplicationResult r
    WHERE r.id = :resultId
    AND r.application.id = :applicationId
    AND r.application.applicationList.id = :listId
    AND r.application.applicationList.userId = :userId
    """)
    Optional<ApplicationResult> findByIdWithApplicationAndListAndUser(Long resultId, Long applicationId, Long listId, String userId);
}
