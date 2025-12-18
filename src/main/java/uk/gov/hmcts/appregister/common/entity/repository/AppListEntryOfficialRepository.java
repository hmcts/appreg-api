package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;

public interface AppListEntryOfficialRepository extends JpaRepository<AppListEntryOfficial, Long> {
    /**
     * Finds all Official entry entities with an ID greater than or equal to the specified value.
     *
     * @param value the minimum ID value (inclusive)
     * @return a list of Official entry entities with IDs greater than or equal to the specified
     *     value
     */
    List<AppListEntryOfficial> findByIdGreaterThanEqual(Integer value);

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
}
