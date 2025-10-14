package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;

public interface AppListEntryResolutionRepository
        extends JpaRepository<AppListEntryResolution, Long> {
    @Query(
            """
        SELECT r FROM AppListEntryResolution r
        WHERE r.id = :resultId
        AND r.applicationList.id = :applicationId
        AND r.applicationList.applicationList.id = :listId
        AND r.applicationList.applicationList.createdUser = :userId
        """)
    Optional<AppListEntryResolution> findByIdWithApplicationAndListAndCreatedUser(
            Long resultId, Long applicationId, Long listId, String userId);

    /**
     * Finds all ApplicationCode entities with an ID greater than or equal to the specified value.
     *
     * @param value the minimum ID value (inclusive)
     * @return a list of ApplicationCode entities with IDs greater than or equal to the specified
     *     value
     */
    List<AppListEntryResolution> findByIdGreaterThanEqual(Integer value);
}
