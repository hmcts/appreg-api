package uk.gov.hmcts.appregister.common.entity.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.appregister.common.entity.AsyncJobsAppListEntry;

public interface AsyncJobAppListEntryRepository extends JpaRepository<AsyncJobsAppListEntry, Long> {
    List<AsyncJobsAppListEntry> findByAsyncJobId(UUID asyncJobId);

    /** Current applicable fees for non-deleted entries, regardless of payment status. */
    @Query(
            value =
                    """
            SELECT COALESCE(SUM(CASE WHEN f.is_offsite IS NOT TRUE
                                     THEN f.fee_value ELSE 0 END), 0) AS mainFeeTotal,
                   COALESCE(SUM(CASE WHEN f.is_offsite IS TRUE
                                     THEN f.fee_value ELSE 0 END), 0) AS offsiteFeeTotal
            FROM {h-schema}async_jobs_app_list_entry job_entry
            JOIN {h-schema}application_list_entries entry ON entry.id = job_entry.ale_id
            JOIN {h-schema}app_list_entry_fee_id entry_fee ON entry_fee.ale_ale_id = entry.ale_id
            JOIN {h-schema}fee f ON f.fee_id = entry_fee.fee_fee_id
            WHERE job_entry.aj_id = :jobId
              AND (entry.is_deleted = 'N' OR entry.is_deleted IS NULL)
            """,
            nativeQuery = true)
    FeeTotals getFeeTotals(UUID jobId);

    interface FeeTotals {
        BigDecimal getMainFeeTotal();

        BigDecimal getOffsiteFeeTotal();
    }
}
