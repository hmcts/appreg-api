package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;

/** Repository interface for managing ApplicationList entities. */
public interface ApplicationListRepository extends JpaRepository<ApplicationList, Long> {

    /**
     * Find all ApplicationList entities associated with a specific user.
     *
     * @param userId the ID of the user
     * @return a list of ApplicationList entities
     */
    List<ApplicationList> findAllByUserName(String userId);

    /**
     * Find an ApplicationList entity by its ID and associated user.
     *
     * @param id the ID of the ApplicationList
     * @param userId the ID of the user
     * @return an Optional containing the ApplicationList if found, or empty if not found
     */
    Optional<ApplicationList> findByIdAndUserName(Long id, String userId);
}
