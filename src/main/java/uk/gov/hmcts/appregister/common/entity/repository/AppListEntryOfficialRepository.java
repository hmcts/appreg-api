package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;

public interface AppListEntryOfficialRepository extends JpaRepository<AppListEntryOfficial, Long> {
    /**
     * gets the official record for an entry id.
     *
     * @param entryId the uuid of the entry
     * @return the official entry
     */
    @Query(
            """
            SELECT off
            FROM AppListEntryOfficial off
            WHERE off.appListEntry.uuid = :entryId
        """)
    List<AppListEntryOfficial> getOfficialByEntryUuid(UUID entryId);

    /**
     * Finds all official records for the supplied application list entry UUIDs.
     *
     * @param entryUuids the application list entry UUIDs
     * @return the matching official records
     */
    List<AppListEntryOfficial> findByAppListEntry_UuidIn(List<UUID> entryUuids);

    /**
     * deletes the official.
     *
     * @param entryId The entry id that the officials map to
     */
    @Modifying
    @Query(
            """
        DELETE
        FROM AppListEntryOfficial off
        WHERE off.appListEntry.id = :entryId
        """)
    void deleteAllForEntryId(Long entryId);
}
