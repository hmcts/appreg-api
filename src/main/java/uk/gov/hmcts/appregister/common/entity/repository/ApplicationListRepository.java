package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;

/**
 * Repository interface for managing ApplicationList entities.
 */
public interface ApplicationListRepository extends JpaRepository<ApplicationList, Long> {

    /**
     * Find all ApplicationList entities associated with a specific user.
     *
     * @param userId the ID of the user
     * @return a list of ApplicationList entities
     */
    List<ApplicationList> findAllByCreatedUser(String userId);

    /**
     * Find an ApplicationList entity by its ID and associated user.
     *
     * @param primaryKey the PK of the ApplicationList
     * @param userId the ID of the user
     * @return an Optional containing the ApplicationList if found, or empty if not found
     */
    Optional<ApplicationList> findByPkAndCreatedUser(Long primaryKey, String userId);

    /**
     * Check if an ApplicationList entity exists by its ID and associated user.
     *
     * @param primaryKey the PK of the ApplicationList
     * @param userId the ID of the user
     * @return true if the ApplicationList exists, false otherwise
     */
    boolean existsByPkAndCreatedUser(Long primaryKey, String userId);

    /**
     * Finds all ApplicationCode entities with an ID greater than or equal to the specified value.
     *
     * @param value the minimum ID value (inclusive)
     * @return a list of ApplicationCode entities with IDs greater than or equal to the specified
     *     value
     */
    List<ApplicationList> findByPkGreaterThanEqual(Integer value);

    /**
     * Finds all entries with the given IDs.
     *
     * @param ids An id to look up
     * @return A list of matching application entries
     */
    Optional<ApplicationList> findByUuid(UUID ids);
}
