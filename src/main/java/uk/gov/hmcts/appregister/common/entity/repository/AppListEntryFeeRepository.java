package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.compositeid.AppListEntryFeeCompositeId;

public interface AppListEntryFeeRepository
        extends JpaRepository<AppListEntryFeeId, AppListEntryFeeCompositeId> {

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
     * Gets the entry to fee mapping for an entry id and fee id.
     *
     * @return The entry fee mapping
     */
    @Query(
            """
        SELECT fee FROM AppListEntryFeeId fee
        WHERE fee.appListEntryId = :id AND fee.feeId = :fee
        """)
    Optional<AppListEntryFeeId> getEntryFeesForFee(Long id, Long fee);
}
