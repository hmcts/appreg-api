package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryOfficialPrintProjection;

/**
 * Repository interface for managing AppListEntryOfficial entities.
 */
@Repository
public interface ApplicationListEntryOfficialRepository
        extends JpaRepository<AppListEntryOfficial, Long> {

    /**
     * Retrieves all officials for a given application list.
     *
     * @param listUuid the UUID of the ApplicationList
     * @param codes printable official types
     * @return a list of officials
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
            WHERE aleo.appListEntry.applicationList.uuid = :listUuid
            AND aleo.officialType in :codes
            """)
    List<ApplicationListEntryOfficialPrintProjection> findByApplicationListUuidForPrinting(
            UUID listUuid, Collection<String> codes);
}
