package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;

public interface AppListEntryFeeStatusRepository
        extends JpaRepository<AppListEntryFeeStatus, Long> {
    /**
     * Finds all ApplicationCode entities with an ID greater than or equal to the specified value.
     *
     * @param value the minimum ID value (inclusive)
     * @return a list of ApplicationCode entities with IDs greater than or equal to the specified
     *     value
     */
    List<AppListEntryFeeStatus> findByIdGreaterThanEqual(Integer value);

    /**
     * Finds a single application list entries by list ID, ensuring it belongs to the specified
     * application list and that the list is owned by the given user.
     *
     * @param listId The ID of the application list the application is expected to belong to
     * @return The application, if found and accessible
     */
    List<AppListEntryFeeStatus> findByAppListEntryId(Long listId);

    /**
     * gets the fee status for an entry id.
     *
     * @param entryId the uuid of the entry
     * @return the official entry
     */
    @Query(
            """
        SELECT appStatus
        FROM AppListEntryFeeStatus appStatus
        WHERE appStatus.appListEntry.uuid = :entryId
        """)
    List<AppListEntryFeeStatus> getFeeStatusByEntryUuid(UUID entryId);
}
