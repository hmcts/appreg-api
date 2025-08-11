package uk.gov.hmcts.appregister.applicationentry.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.appregister.applicationentry.model.Application;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    /**
     * Finds a single application by ID, ensuring it belongs to the specified application list and
     * that the list is owned by the given user.
     *
     * @param id The application ID
     * @param listId The ID of the application list the application is expected to belong to
     * @param userId The ID of the user who owns the list
     * @return The application, if found and accessible
     */
    Optional<Application> findByIdAndApplicationListIdAndApplicationListUserId(
            Long id, Long listId, String userId);

    /**
     * Finds all applications with the given IDs that are accessible to a specific user. Only
     * applications belonging to lists owned by that user will be returned.
     *
     * @param ids A list of application IDs to look up
     * @param userId The ID of the user who must own the associated lists
     * @return A list of matching applications that the user is authorized to access
     */
    List<Application> findByIdInAndApplicationListUserId(List<Long> ids, String userId);

    @Query(
            """
    SELECT e FROM Application e
    LEFT JOIN FETCH e.standardApplicant
    LEFT JOIN FETCH e.applicationCode
    WHERE e.applicationList.id = :listId
        """)
    List<Application> findByApplicationListIdWithJoins(Long listId);
}
