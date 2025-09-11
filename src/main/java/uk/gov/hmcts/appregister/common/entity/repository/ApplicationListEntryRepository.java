package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;

public interface ApplicationListEntryRepository extends JpaRepository<ApplicationListEntry, Long> {
    /**
     * Find an ApplicationList entity by its ID and associated user.
     *
     * @param id the ID of the ApplicationList
     * @param userId the ID of the user
     * @return an Optional containing the ApplicationList if found, or empty if not found
     */
    Optional<ApplicationListEntry> findByIdAndCreatedUser(Long id, String userId);

    /**
     * Finds a single application by ID, ensuring it belongs to the specified application list and
     * that the list is owned by the given user.
     *
     * @param id The application ID
     * @param listId The ID of the application list the application is expected to belong to
     * @param userId The ID of the user who owns the list
     * @return The application, if found and accessible
     */
    Optional<ApplicationListEntry> findByIdAndApplicationListIdAndCreatedUser(
            Long id, Long listId, String userId);

    /**
     * Finds all applications with the given IDs that are accessible to a specific user. Only
     * applications belonging to lists owned by that user will be returned.
     *
     * @param ids A list of application IDs to look up
     * @param userId The ID of the user who must own the associated lists
     * @return A list of matching applications that the user is authorized to access
     */
    List<ApplicationListEntry> findByIdInAndCreatedUser(List<Long> ids, String userId);

    /**
     * Finds all applications with the given IDs that are accessible to a specific user. Only
     * applications belonging to lists owned by that user will be returned.
     *
     * @param ids A list of application IDs to look up
     * @param userId The ID of the user who must own the associated lists
     * @return A list of matching applications that the user is authorized to access
     */
    List<ApplicationListEntry> findByApplicationListIdAndCreatedUser(Long ids, String userId);

    /**
     * Finds all ApplicationCode entities with an ID greater than or equal to the specified value.
     *
     * @param value the minimum ID value (inclusive)
     * @return a list of ApplicationCode entities with IDs greater than or equal to the specified
     *     value
     */
    List<ApplicationListEntry> findByIdGreaterThanEqual(Integer value);
}
