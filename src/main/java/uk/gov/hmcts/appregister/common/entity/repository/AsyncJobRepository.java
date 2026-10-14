package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.AsyncJob;

@Repository
public interface AsyncJobRepository extends JpaRepository<AsyncJob, Long> {

    // Select metadata only: loading the CSV here would make listing large reports expensive.
    @Query(
            value =
                    """
                SELECT aj_id, id, job_state, job_type, last_updated, failure_message, user_name
                FROM {h-schema}asynch_jobs
                WHERE user_name = :userId
                  AND job_state = 'COMPLETED'
                  AND csv_output IS NOT NULL AND csv_output <> ''
                  AND job_type IN ('ACTIVITY_AUDIT_REPORT', 'FEES_REPORT',
                      'LIST_MAINTENANCE_REPORT', 'SEARCH_WARRANTS_REPORT', 'WORKLOAD_REPORT',
                      'DURATION_REPORT', 'PRIVATE_PROSECUTORS_INDEX_REPORT')
                ORDER BY last_updated DESC, aj_id DESC
                """,
            nativeQuery = true)
    List<AsyncJob> findRecentReports(String userId, Pageable pageable);

    /**
     * finds an asynchronous job by job number and the user id.
     *
     * @param jobType The job type
     * @param userId The user id
     */
    @Query(
            """
            SELECT aj
            FROM AsyncJob aj
            WHERE aj.jobType = :jobType
            AND aj.userName = :userId
        """)
    AsyncJob findByJobTypeAndUser(String jobType, String userId);

    /**
     * finds an asynchronous job by job number and the user id.
     *
     * @param jobId The job id to return the job.
     * @param userId The user id of the current user to check ownership.
     */
    @Query(
            """
            SELECT aj
            FROM AsyncJob aj
            WHERE aj.uuid = :jobId
            AND aj.userName = :userId
        """)
    AsyncJob findByJobId(UUID jobId, String userId);
}
