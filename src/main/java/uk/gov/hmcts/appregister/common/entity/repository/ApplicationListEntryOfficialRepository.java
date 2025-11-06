package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.Collection;
import java.util.List;
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
     * Retrieves list of officials for a given application list entry.
     *
     * @param id the ID of the ApplicationListEntry
     * @return officials
     */
    @Query(
            """
            SELECT
                aleo.officialType AS type,
                aleo.title AS title,
                aleo.forename AS forename,
                aleo.surname AS surname
            FROM AppListEntryOfficial aleo
            WHERE aleo.appListEntry.id = :id
            AND aleo.officialType IN :codes
            """)
    List<ApplicationListOfficialPrintProjection> findByIdForPrinting(
            Long id, Collection<String> codes);
}
