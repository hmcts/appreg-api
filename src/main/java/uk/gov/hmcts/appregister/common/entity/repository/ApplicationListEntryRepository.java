package uk.gov.hmcts.appregister.common.entity.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.aspect.LikeParam;
import uk.gov.hmcts.appregister.common.entity.base.EntryCount;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryGetSummaryProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryPrintProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntrySummaryProjection;

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
    Optional<ApplicationListEntry> findByIdAndApplicationListIdAndCreatedUser(
            Long id, Long listId, String userId);

    /**
     * Finds a single application list entries by list ID, ensuring it belongs to the specified
     * application list and that the list is owned by the given user.
     *
     * @param listId The ID of the application list the application is expected to belong to
     * @return The application, if found and accessible
     */
    List<ApplicationListEntry> findByApplicationListId(Long listId);

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
    List<ApplicationListEntry> findByApplicationListIdAndCreatedUser(Long ids, String userId);

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
            ana AS applicant,
            sa AS standardApplicant,
            rna AS respondent,
            rna.postcode AS postCode,
            ac.title AS applicationTitle,
            CASE WHEN ac.feeDue = "Y" THEN true ELSE false END AS feeRequired,
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
        AND (ale.deleted IS NULL OR ale.deleted <> 'Y')
        """)
    Page<ApplicationListEntrySummaryProjection> findSummariesById(UUID id, Pageable pageable);

    @Query(
            """
        select ale.applicationList.uuid as primaryKey, count(ale) as count
        from ApplicationListEntry ale
        where ale.applicationList.uuid in :uuids
        and (ale.deleted IS NULL OR ale.deleted <> 'Y')
        group by ale.applicationList.uuid
        """)
    List<EntryCount> countByApplicationListUuids(@Param("uuids") List<UUID> uuids);

    /**
     * Retrieves the paginated summary results.
     *
     * @param applicationListId The application list id to filter by (optional)
     * @param hasHearingDate Whether to filter by hearing date
     * @param hearingDate The hearing date to use for filtering if hasHearingDate is true
     * @param courtCode The court code to filter by.
     * @param otherLocationDescription The other location description to filter by. Partial matches
     *     allowed
     * @param cjaCode The criminal justice area code to filter by.
     * @param applicantOrganisation The applicant organisation to filter by. Partial matches allowed
     * @param applicantSurname The applicant surname to filter by. Partial matches allowed
     * @param standardApplicantCode The standard applicant code to filter by. Partial matches
     *     allowed
     * @param status The status to filter by
     * @param respondentOrganisation The respondent organisation to filter by. Partial matches
     *     allowed
     * @param respondentSurname The respondent surname to filter by. Partial matches allowed
     * @param respondentPostcode The respondent postcode to filter by. Partial matches allowed
     * @param accountReference The account reference to filter by. Partial matches allowed
     * @param pageable The pagination information
     * @return A page of ApplicationListEntryGetSummaryProjection matching the criteria
     */
    @Query(
            """
             SELECT
                    al.date  AS date,
                    ale.uuid AS uuid,
                    ale.id AS id,
                    al.courtCode  AS courtCode,
                    ac.legislation as legislation,
                    ac.feeDue feeRequired,
                    aler.id as result,
                    cja.code AS cjaCode,
                    al.otherLocation AS otherLocationDescription,
                    ana as anameAddress,
                    sa.applicantCode AS standardApplicantCode,
                    rna as rnameAddress,
                    ac.title as title,
                    al.status AS status,
                    al.date as dateOfAl,
                    ana.name as applicationorganisation,
                    ana.surname as applicantSurname,
                    CASE WHEN ana.surname IS NOT NULL AND ana.forename1 IS NOT NULL AND ana.title IS NOT NULL THEN
                        CONCAT(ana.surname, ',', ana.forename1, ',', ana.title)
                    WHEN ana.surname IS NOT NULL AND ana.forename1 IS NOT NULL AND ana.title IS NULL THEN
                        CONCAT(ana.surname, ',', ana.forename1)
                    END as applicantName,
                    CASE WHEN rna.surname IS NOT NULL AND rna.forename1 IS NOT NULL AND rna.title IS NOT NULL THEN
                        CONCAT(rna.surname, ',', rna.forename1, ',', rna.title)
                    WHEN rna.surname IS NOT NULL AND rna.forename1 IS NOT NULL AND rna.title IS NULL THEN
                        CONCAT(rna.surname, ',', rna.forename1)
                    END as respondentName,
                    rna.name as respondentOrganisation,
                    rna.surname as respondentSurname,
                    rna.postcode as respondentPostcode,
                    ale.accountNumber as  accountReference,
                    sa as standardApplicant,
                    al.uuid as listId,
                    ac.title AS applicationTitle,
                    ale.sequenceNumber as sequenceNumber,
                    rc as resolutionCode
                from ApplicationListEntry ale
                LEFT JOIN ale.anamedaddress ana
                LEFT JOIN ale.standardApplicant sa
                LEFT JOIN ale.rnameaddress rna
                LEFT JOIN ale.applicationCode ac
                LEFT JOIN ale.applicationList al
                LEFT JOIN CriminalJusticeArea cja ON al.cja = cja
                LEFT JOIN AppListEntryResolution aler ON aler.applicationList
                            = ale AND aler.id = (SELECT MAX(sub.id)
                              FROM AppListEntryResolution sub
                              WHERE sub.applicationList = ale)
                LEFT JOIN aler.resolutionCode rc on rc.id = aler.resolutionCode.id
            WHERE  (:hasHearingDate = false OR :hasHearingDate IS NULL OR al.date = :hearingDate)
                    AND (:applicationListId IS NULL OR al.uuid = :applicationListId)
                    AND (:otherLocationDescription IS NULL OR LOWER(al.otherLocation)
                            LIKE CONCAT('%', LOWER(cast(:otherLocationDescription AS string)), '%') ESCAPE '\\')
                    AND (:courtCode IS NULL OR LOWER(al.courtCode) = LOWER(cast(:courtCode AS string )))
                    AND (:cjaCode IS NULL OR LOWER(cja.code)=LOWER(cast(:cjaCode AS STRING )))
                    AND (:applicantName IS NULL OR LOWER(CONCAT(COALESCE(ana.surname, ' '),
                                        COALESCE(ana.name, ' '), COALESCE(ana.title, ' ')))
                            LIKE CONCAT('%', LOWER(cast(:applicantName AS string)) , '%') ESCAPE '\\'
                            AND ana.code='NA')
                    AND (:applicantOrganisation IS NULL OR LOWER(ana.name)
                            LIKE CONCAT('%',LOWER(cast(:applicantOrganisation AS string)), '%') ESCAPE '\\'
                            AND ana.code='NA')
                    AND (:applicantSurname IS NULL OR LOWER(ana.surname)
                             LIKE CONCAT('%', LOWER(cast(:applicantSurname AS string)) , '%')  ESCAPE '\\'
                            AND ana.code='NA')
                    AND (:standardApplicantCode IS NULL OR LOWER(sa.applicantCode)
                            LIKE CONCAT('%', LOWER(cast(:standardApplicantCode AS string)), '%')  ESCAPE '\\')
                    AND (:status IS NULL OR :status=ale.applicationList.status)
                    AND (:respondentName IS NULL OR LOWER( CONCAT(COALESCE(rna.surname, ' '), COALESCE(rna.name, ' '),
                            COALESCE(rna.title, ' ')))  LIKE CONCAT('%',
                            LOWER(cast(:respondentName AS string )), '%')  ESCAPE '\\' AND rna.code='RE')
                    AND (:respondentOrganisation IS NULL OR LOWER(rna.name) LIKE CONCAT('%',
                            LOWER(cast(:respondentOrganisation AS string)), '%')  ESCAPE '\\' AND rna.code='RE')
                    AND (:respondentSurname IS NULL OR LOWER(rna.surname) LIKE CONCAT('%',
                            LOWER(cast(:respondentSurname AS string)), '%')  ESCAPE '\\' AND rna.code='RE')
                    AND (:accountReference IS NULL OR  LOWER(ale.accountNumber)
                            LIKE CONCAT('%', LOWER(cast(:accountReference AS string)), '%')
                                         ESCAPE '\\')
                    AND (:respondentPostcode IS NULL OR LOWER(rna.postcode) LIKE
                              CONCAT('%', LOWER(cast(:respondentPostcode AS string)), '%')
                                          ESCAPE '\\' AND rna.code='RE')
                    AND (:applicationTitle IS NULL OR LOWER(ac.title) LIKE
                                CONCAT('%', LOWER(cast(:applicationTitle AS string)), '%')  ESCAPE '\\')
                    AND (:feeRequired IS NULL OR ac.feeDue = CASE WHEN :feeRequired = true THEN 'Y' ELSE 'N' END)
                    AND (:sequenceNumber IS NULL OR ale.sequenceNumber = :sequenceNumber)
                    AND (al.deleted IS NULL OR al.deleted <> 'Y')
                    AND (ale.deleted IS NULL OR ale.deleted <> 'Y')
            """)
    Page<ApplicationListEntryGetSummaryProjection> searchForGetSummary(
            @Param("applicationListId") UUID applicationListId,
            Boolean hasHearingDate,
            @Param("hearingDate") LocalDate hearingDate,
            @Param("courtCode") String courtCode,
            @LikeParam @Param("otherLocationDescription") String otherLocationDescription,
            @Param("cjaCode") String cjaCode,
            @LikeParam @Param("applicantOrganisation") String applicantOrganisation,
            @LikeParam @Param("applicantSurname") String applicantSurname,
            @LikeParam @Param("applicantName") String applicantName,
            @LikeParam @Param("standardApplicantCode") String standardApplicantCode,
            @Param("status") Status status,
            @LikeParam @Param("respondentOrganisation") String respondentOrganisation,
            @LikeParam @Param("respondentSurname") String respondentSurname,
            @LikeParam @Param("respondentName") String respondentName,
            @LikeParam @Param("respondentPostcode") String respondentPostcode,
            @LikeParam @Param("accountReference") String accountReference,
            @LikeParam @Param("applicationTitle") String applicationTitle,
            @Param("feeRequired") Boolean feeRequired,
            @Param("sequenceNumber") Integer sequenceNumber,
            Pageable pageable);

    /**
     * Retrieves list of entries for a given application list.
     *
     * @param id the ID of the ApplicationList
     * @return application list entry projections
     */
    @Query(
            """
        SELECT
            ale.id AS id,
            ale.uuid AS uuid,
            ale.sequenceNumber AS sequenceNumber,
            COALESCE(ana.title, sa.applicantTitle) AS applicantTitle,
            COALESCE(ana.surname, sa.applicantSurname) AS applicantSurname,
            COALESCE(ana.forename1, sa.applicantForename1) AS applicantForename1,
            COALESCE(ana.forename2, sa.applicantForename2) AS applicantForename2,
            COALESCE(ana.forename3, sa.applicantForename3) AS applicantForename3,
            COALESCE(ana.address1, sa.addressLine1) AS applicantAddressLine1,
            COALESCE(ana.address2, sa.addressLine2) AS applicantAddressLine2,
            COALESCE(ana.address3, sa.addressLine3) AS applicantAddressLine3,
            COALESCE(ana.address4, sa.addressLine4) AS applicantAddressLine4,
            COALESCE(ana.address5, sa.addressLine5) AS applicantAddressLine5,
            COALESCE(ana.postcode, sa.postcode) AS applicantPostcode,
            COALESCE(ana.telephoneNumber, sa.telephoneNumber) AS applicantPhone,
            COALESCE(ana.mobileNumber, sa.mobileNumber) AS applicantMobile,
            COALESCE(ana.emailAddress, sa.emailAddress) AS applicantEmail,
            COALESCE(ana.name, sa.name) AS applicantName,
            rna.title AS respondentTitle,
            rna.surname AS respondentSurname,
            rna.forename1 AS respondentForename1,
            rna.forename2 AS respondentForename2,
            rna.forename3 AS respondentForename3,
            rna.address1 AS respondentAddressLine1,
            rna.address2 AS respondentAddressLine2,
            rna.address3 AS respondentAddressLine3,
            rna.address4 AS respondentAddressLine4,
            rna.address5 AS respondentAddressLine5,
            rna.postcode AS respondentPostcode,
            rna.telephoneNumber AS respondentPhone,
            rna.mobileNumber AS respondentMobile,
            rna.emailAddress AS respondentEmail,
            rna.dateOfBirth AS respondentDateOfBirth,
            rna.name AS respondentName,
            ac.code AS applicationCode,
            ac.title AS applicationTitle,
            ale.applicationListEntryWording AS applicationWording,
            ale.caseReference AS caseReference,
            ale.accountNumber AS accountReference,
            ale.notes AS notes
        FROM ApplicationListEntry ale
        LEFT JOIN ale.anamedaddress ana
        LEFT JOIN ale.standardApplicant sa
        LEFT JOIN ale.rnameaddress rna
        LEFT JOIN ale.applicationCode ac
        WHERE ale.applicationList.uuid = :id
        AND (ale.deleted IS NULL OR ale.deleted <> 'Y')
        ORDER BY ale.sequenceNumber
        """)
    List<ApplicationListEntryPrintProjection> findByIdForPrinting(UUID id);

    /**
     * Finds an entry for Uuid.
     *
     * @param entryId The entry id
     * @return A single matching application entry
     */
    @Query(
            """
        SELECT ale
        FROM ApplicationListEntry ale
        WHERE ale.uuid = :entryId AND (ale.deleted IS NULL OR ale.deleted <> 'Y')
        """)
    Optional<ApplicationListEntry> findByUuid(UUID entryId);

    /**
     * Finds all entities with the given IDs, within the associated list.
     *
     * @param entryId The entry id
     * @param listId The list that the entry resides in
     * @return A single matching application entry
     */
    @Query(
            """
        SELECT ale
        FROM ApplicationListEntry ale
        WHERE ale.applicationList.uuid = :listId AND ale.uuid = :entryId
                AND (ale.deleted IS NULL OR ale.deleted <> 'Y')
        """)
    Optional<ApplicationListEntry> findByEntryUuidWithinListUuid(UUID listId, UUID entryId);

    /**
     * Bulk-move entries to a new application list using a single JPQL UPDATE. Returns number of
     * rows updated.
     *
     * @param entryUuids the set of entry UUIDs to move; only entries matching these UUIDs and
     *     belonging to the sourceListUuid will be updated
     * @param targetList the ApplicationList entity representing the new target list to which the
     *     entries will be reassigned; this value is written to the applicationList field of all
     *     matching entries
     * @param sourceListUuid the UUID of the source ApplicationList; only entries currently
     *     associated with this list will be updated
     * @return the number of rows updated; may be less than the number of provided UUIDs if some
     *     entries are not found in the source list
     */
    @Modifying
    @Query(
            """
        UPDATE ApplicationListEntry ale
        SET ale.applicationList = :targetList
        WHERE ale.uuid IN :entryUuids
        AND ale.applicationList.uuid = :sourceListUuid
        AND (ale.deleted IS NULL OR ale.deleted <> 'Y')
        """)
    int bulkMoveByUuidAndSourceList(
            Set<UUID> entryUuids, ApplicationList targetList, UUID sourceListUuid);

    /**
     * Retrieves an application list entry by its UUID and the UUID of the application list it
     * belongs to.
     *
     * @param entryUuid the UUID of the application list entry
     * @param listUuid the UUID of the parent application list
     * @return an Optional containing the entry if found, otherwise empty
     */
    @Query(
            """
        SELECT ale
        FROM ApplicationListEntry ale
        WHERE ale.uuid = :entryUuid
          AND ale.applicationList.uuid = :listUuid
          AND (ale.deleted IS NULL OR ale.deleted <> 'Y')
        """)
    Optional<ApplicationListEntry> findActiveByUuidAndApplicationListUuid(
            @Param("entryUuid") UUID entryUuid, @Param("listUuid") UUID listUuid);

    /**
     * Soft-deletes an application list entry by UUID.
     *
     * @param entryUuid the UUID of the application list entry to delete
     * @return number of rows updated (0 or 1)
     */
    @Modifying
    @Transactional
    @Query(
            """
        UPDATE ApplicationListEntry ale
        SET ale.deleted = 'Y'
        WHERE ale.uuid = :entryUuid
        """)
    int softDeleteByUuid(@Param("entryUuid") UUID entryUuid);

    /**
     * Finds entries for a list, excluding deleted entries.
     *
     * @param appLstId The application list
     * @param pageable The page criteria
     * @return A page of matching application entries
     */
    @Query(
            """
        SELECT ale
        FROM ApplicationListEntry ale
        WHERE ale.applicationList.uuid = :appLstId
            AND (ale.deleted IS NULL OR ale.deleted <> 'Y')
        """)
    Page<ApplicationListEntry> findForApplicationList(UUID appLstId, Pageable pageable);

    /**
     * Retrieves the subset of Application List Entry UUIDs that exist in the given source list.
     *
     * @param sourceListId the UUID of the source ApplicationList
     * @param requestedIds the set of Application List Entry UUIDs requested for movement
     * @return a Set of UUIDs representing entries that exist in the source list
     */
    @Query(
            """
        SELECT ale.uuid
        FROM ApplicationListEntry ale
        WHERE ale.applicationList.uuid = :sourceListId
        AND ale.uuid in :requestedIds
        AND (ale.deleted IS NULL OR ale.deleted <> 'Y')
        """)
    Set<UUID> findExistingEntryIdsInSourceList(UUID sourceListId, Set<UUID> requestedIds);
}
