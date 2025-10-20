package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntrySummaryProjection;
import uk.gov.hmcts.appregister.common.entity.base.EntryCount;

public interface ApplicationListEntryRepository extends JpaRepository<ApplicationListEntry, Long> {
    /**
     * Find an ApplicationList entity by its ID and associated user.
     *
     * @param id the ID of the ApplicationList
     * @param userId the ID of the user
     * @return an Optional containing the ApplicationList if found, or empty if not found
     */
    Optional<ApplicationListEntry> findByIdAndCreatedUser(Long id, String userId);

    /**
     * Finds a single application by ID, ensuring it belongs to the specified application list and
     * that the list is owned by the given user.
     *
     * @param id The application ID
     * @param listId The ID of the application list the application is expected to belong to
     * @param userId The ID of the user who owns the list
     * @return The application, if found and accessible
     */
    Optional<ApplicationListEntry> findByIdAndApplicationListPkAndCreatedUser(
            Long id, Long listId, String userId);

    /**
     * Finds all applications with the given IDs that are accessible to a specific user. Only
     * applications belonging to lists owned by that user will be returned.
     *
     * @param ids A list of application IDs to look up
     * @param userId The ID of the user who must own the associated lists
     * @return A list of matching applications that the user is authorized to access
     */
    List<ApplicationListEntry> findByIdInAndCreatedUser(List<Long> ids, String userId);

    /**
     * Finds all applications with the given IDs that are accessible to a specific user. Only
     * applications belonging to lists owned by that user will be returned.
     *
     * @param ids A list of application IDs to look up
     * @param userId The ID of the user who must own the associated lists
     * @return A list of matching applications that the user is authorized to access
     */
    List<ApplicationListEntry> findByApplicationListPkAndCreatedUser(Long ids, String userId);

    /**
     * Finds all ApplicationCode entities with an ID greater than or equal to the specified value.
     *
     * @param value the minimum ID value (inclusive)
     * @return a list of ApplicationCode entities with IDs greater than or equal to the specified
     *     value
     */
    List<ApplicationListEntry> findByIdGreaterThanEqual(Integer value);

    /**
     * Retrieves paginated list of entry summaries for a given application list.
     *
     * @param id the ID of the ApplicationList
     * @param pageable Spring Data paging and sorting configuration
     * @return a page of summary projections
     */
    @Query(
            """
            SELECT
                ale.sequenceNumber,
                ale.accountNumber AS accountNumber,
                COALESCE(ana.name, sa.name) AS applicant,
                rna.name AS respondent,
                rna.postcode AS postCode,
                ac.title AS applicationTitle,
                CASE WHEN ac.feeDue = "1" THEN true ELSE false END AS feeRequired,
                rc.resultCode AS result
            FROM ApplicationListEntry ale
            LEFT JOIN ale.anamedaddress ana
            LEFT JOIN ale.standardApplicant sa
            LEFT JOIN ale.rnameaddress rna
            INNER JOIN ale.applicationCode ac
            LEFT JOIN AppListEntryResolution aler ON aler.applicationList = ale
                AND aler.changedBy = (
                    SELECT MAX(sub.changedBy)
                    FROM AppListEntryResolution sub
                    WHERE sub.applicationList = ale
                )
            JOIN aler.resolutionCode rc
            WHERE ale.applicationList.uuid = :id
            """)
    Page<ApplicationListEntrySummaryProjection> findSummariesById(UUID id, Pageable pageable);

    @Query(
            """
        select ale.applicationList.uuid as pk, count(ale) as cnt
        from ApplicationListEntry ale
        where ale.applicationList.uuid in :uuids
        group by ale.applicationList.uuid
        """)
    List<EntryCount> countByApplicationListUuids(@Param("uuids") List<UUID> uuids);
}
