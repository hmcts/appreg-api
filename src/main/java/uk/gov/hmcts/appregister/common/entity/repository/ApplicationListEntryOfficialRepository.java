package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.projection.ApplicationListOfficialPrintProjection;

/**
 * Repository interface for managing AppListEntryOfficial entities.
 */
@Repository
public interface ApplicationListEntryOfficialRepository
        extends JpaRepository<AppListEntryOfficial, Long> {

    /**
     * Retrieves officials for all entries in a given application list (bulk).
     *
     * @param listUuid UUID of the ApplicationList (parent of the entries)
     * @param codes printable official types
     * @return rows containing entryId and official fields, ordered for stable rendering
     */
    @Query(
            """
            SELECT
               aleo.appListEntry.id as entryId,
               aleo.officialType as type,
               aleo.title as title,
               aleo.forename as forename,
               aleo.surname as surname
            FROM AppListEntryOfficial aleo
            JOIN aleo.appListEntry ale
            WHERE aleo.appListEntry.applicationList.uuid = :listUuid
            AND aleo.officialType in :codes
            ORDER BY ale.id, aleo.id
            """)
    List<ApplicationListOfficialPrintProjection> findOfficialsForPrinting(
            UUID listUuid, Collection<String> codes);
}
