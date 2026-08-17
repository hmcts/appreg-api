package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId;
import uk.gov.hmcts.appregister.common.entity.Fee;

public interface AppListEntryFeeRepository extends JpaRepository<AppListEntryFeeId, Long> {
    /**
     * gets all fees for an entry id.
     *
     * @return The fees for an entry
     */
    @Query(
            """
        SELECT f FROM AppListEntryFeeId entryFee
        LEFT JOIN Fee f ON entryFee.feeId = f.id
        WHERE entryFee.appListEntryId = :id
        """)
    List<Fee> getFeeForEntryId(Long id);

    /**
     * gets the entries for an entry id.
     *
     * @return The entry fees for an entry
     */
    @Query(
            """
        SELECT fee FROM AppListEntryFeeId fee
        WHERE fee.appListEntryId = :id
        """)
    List<AppListEntryFeeId> getEntryFeesForEntry(Long id);

    /**
     * Gets offsite fee mappings for an application list entry.
     *
     * @param entryId the application list entry database id
     * @return offsite fee mappings for the entry
     */
    @Query(
            """
        SELECT entryFee
        FROM AppListEntryFeeId entryFee
        JOIN Fee fee ON entryFee.feeId = fee.id
        WHERE entryFee.appListEntryId = :entryId
        AND fee.isOffsite = true
        """)
    List<AppListEntryFeeId> getOffsiteEntryFeesForEntry(Long entryId);

    /**
     * Gets offsite fee mappings for multiple application list entries.
     *
     * @param entryIds the application list entry database ids
     * @return offsite fee mappings for the entries
     */
    @Query(
            """
        SELECT entryFee
        FROM AppListEntryFeeId entryFee
        JOIN Fee fee ON entryFee.feeId = fee.id
        WHERE entryFee.appListEntryId IN :entryIds
        AND fee.isOffsite = true
        """)
    List<AppListEntryFeeId> getOffsiteEntryFeesForEntries(Collection<Long> entryIds);
}
