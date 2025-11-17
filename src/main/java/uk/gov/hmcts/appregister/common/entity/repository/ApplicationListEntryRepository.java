package uk.gov.hmcts.appregister.common.entity.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.base.EntryCount;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryGetSummaryProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntrySummaryProjection;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;

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
                ale.uuid AS uuid,
                ale.sequenceNumber AS sequenceNumber,
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
            LEFT JOIN ale.applicationCode ac
            LEFT JOIN AppListEntryResolution aler ON aler.applicationList = ale
                AND aler.changedDate = (
                    SELECT MAX(sub.changedDate)
                    FROM AppListEntryResolution sub
                    WHERE sub.applicationList = ale
                )
            LEFT JOIN aler.resolutionCode rc
            WHERE ale.applicationList.uuid = :id
            """)
    Page<ApplicationListEntrySummaryProjection> findSummariesById(UUID id, Pageable pageable);

    @Query(
            """
        select ale.applicationList.uuid as primaryKey, count(ale) as count
        from ApplicationListEntry ale
        where ale.applicationList.uuid in :uuids
        group by ale.applicationList.uuid
        """)
    List<EntryCount> countByApplicationListUuids(@Param("uuids") List<UUID> uuids);

    /** Retrieves the paginated results */
    @Query(
            """
        SELECT
                ale.uuid AS uuid,
                ale.applicationList.date AS hearingDate,
                ale.applicationList.courtCode  AS courtCode,
                ac.legislation as legislation,
                CASE WHEN ac.feeDue = "1" THEN true ELSE false END AS feeRequired,
                aler.resolutionCode AS result,
                ale.applicationList.cja.code AS cjaCode,
                ale.applicationList.otherLocation AS otherLocationDescription,
                ale.anamedaddress.name AS appOrganisation,
                ale.anamedaddress.surname AS appSurname,
                ale.anamedaddress as anamedaddress,
                ale.standardApplicant.applicantCode AS standardApplicantCode,
                ale.rnameaddress.name AS respondentOrganisation,
                ale.rnameaddress.surname AS respondentSurname,
                ale.rnameaddress.postcode AS respondentPostcode,
                ale.rnameaddress as rnameaddress,
                ale.caseReference AS accountReference,
                ale.applicationCode.title as title,
                status AS status
            from ApplicationListEntry ale
            LEFT JOIN ale.anamedaddress ana ON ale.applicationList = ale.anamedaddress
            LEFT JOIN ale.standardApplicant sa ON ale.applicationList = ale.standardApplicant
            LEFT JOIN ale.rnameaddress rna ON ale.applicationList = ale.rnameaddress
            LEFT JOIN ale.applicationCode ac  ON ale.applicationList = ale.applicationCode
            LEFT JOIN AppListEntryResolution aler ON aler.applicationList = ale
                AND aler.changedDate = (
                    SELECT MAX(sub.changedDate)
                    FROM AppListEntryResolution sub
                    WHERE sub.applicationList = ale
                )
            LEFT JOIN ApplicationList al ON ale.applicationList = ale.applicationList
            LEFT JOIN AppListEntryFeeStatus appstatus ON appstatus.appListEntry = ale
                AND appstatus.changedDate = (
                    SELECT MAX(sub.changedDate)
                    FROM AppListEntryFeeStatus sub
                    WHERE sub.appListEntry = ale
                )
        where :dte IS NULL OR :hearingDate=ale.applicationList.date
                AND :otherLocationDescription IS NULL OR :otherLocationDescription=ale.applicationList.otherLocation
                AND :courtCode IS NULL OR :courtCode LIKE '%' + ale.applicationList.courtCode + '%'
                AND :cjaCode IS NULL OR :cjaCode=ale.applicationList.cja.code
                AND :applicantOrganisation IS NULL OR :appOrganisation LIKE '%' + ale.anamedaddress.name + '%' AND ale.anamedaddress.code='AP'
                AND :applicantSurname IS NULL OR :appSurname LIKE '%' + ale.anamedaddress.surname + '%' AND ale.anamedaddress.code='AP'
                AND :standardApplicantCode IS NULL OR :appCode LIKE '%' + ale.standardApplicant.applicantCode + '%'
                AND :status IS NULL OR :status=ale.applicationList.status
                AND :respondentOrganisation IS NULL OR :respOrganisation=ale.rnameaddress.name AND ale.rnameaddress.code='RE'
                AND :respondentSurname IS NULL OR :respSurname=ale.rnameaddress.surname AND ale.rnameaddress.code='RE'
                AND :respondentPostcode IS NULL OR :respPostcode=ale.rnameaddress.postcode AND ale.rnameaddress.code='RE'
                AND :accountReference IS NULL OR :accountReference LIKE '%' + ale.caseReference + '%'
        """)
    Page<ApplicationListEntryGetSummaryProjection> findApplicationList(
            LocalDate hearingDate,
            String courtCode,
            String otherLocationDescription,
            String cjaCode,
            String applicantOrganisation,
            String applicantSurname,
            String standardApplicantCode,
            Status status,
            String respondentOrganisation,
            String respondentSurname,
            String respondentPostcode,
            String accountReference,
            Pageable pageable);
}
