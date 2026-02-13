package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryResolutionPrintProjection;

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

    /**
     * Retrieves all result wordings for a given application list.
     *
     * @param listUuid the UUID of the ApplicationList
     * @return a list of result wordings
     */
    @Query(
            """
            SELECT
                aler.applicationList.id AS entryId,
                aler.resolutionWording AS wording
            FROM AppListEntryResolution aler
            WHERE aler.applicationList.applicationList.uuid = :listUuid
            """)
    List<ApplicationListEntryResolutionPrintProjection> findByApplicationListUuidForPrinting(
            UUID listUuid);

    /**
     * Finds an AppListEntryResolution by its unique identifier and the UUID of the associated
     * application list entry.
     *
     * @param resolutionUuid the UUID of the AppListEntryResolution to find
     * @param entryUuid the UUID of the associated application list entry
     * @return an Optional containing the matching AppListEntryResolution if found
     */
    Optional<AppListEntryResolution> findByUuidAndApplicationList_Uuid(
            UUID resolutionUuid, UUID entryUuid);

    /**
     * Finds an AppListEntryResolution by the UUID of the associated application list entry.
     *
     * @param listUuid the UUID of the list
     * @return A list of AppListEntryResolution entities associated with the specified list UUID, or
     *     an empty * list if no matching entities are found.
     */
    List<AppListEntryResolution> findByApplicationListUuid(UUID listUuid);
}
