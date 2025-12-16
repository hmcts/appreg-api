package uk.gov.hmcts.appregister.common.entity.repository;

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
}
