package uk.gov.hmcts.appregister.common.entity.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.aspect.LikeParam;
import uk.gov.hmcts.appregister.common.projection.StandardApplicantEnrichedProjection;

/**
 * Repository for StandardApplicant entities.
 */
@Repository
public interface StandardApplicantRepository extends JpaRepository<StandardApplicant, Long> {

    /**
     * Finds a StandardApplicant by its applicant code.
     *
     * @param code the applicant code to search for
     * @param date The date to check for active status
     * @return an Optional containing the found StandardApplicant, or empty if not found
     */
    @Query(
            """
        SELECT sa
        FROM StandardApplicant sa
        WHERE LOWER(sa.applicantCode) = LOWER(CAST(:code AS string))
        AND sa.applicantStartDate <= :date
        AND (sa.applicantEndDate IS NULL OR sa.applicantEndDate >= :date)
        ORDER BY CASE WHEN sa.applicantEndDate IS NULL THEN 0 ELSE 1 END,
                 sa.applicantEndDate DESC,
                 sa.applicantStartDate DESC,
                 sa.id DESC
        """)
    List<StandardApplicant> findStandardApplicantByCodeAndDate(
            @Param("code") String code, @Param("date") LocalDate date);

    /**
     * Finds the ids that are greater than this value.
     *
     * @param value the minimum ID value
     * @return a list of ApplicationCode entities with IDs >= value
     */
    List<StandardApplicant> findByIdGreaterThanEqual(Integer value);

    /**
     * Retrieve a page of active Standrd Applicant Codes filtered by code/name (case-insensitive).
     *
     * <p>Active if: c.startDate <= :date AND (c.endDate IS NULL OR c.endDate >= :date)
     *
     * <p>Name can represent the name title or the forename_1 or the surname. If name is not null we
     * use name as this is an organisation. The expectations is that the forename and surname will
     * be null in this case.
     *
     * <p>If the name is not null then we search matching results on the name, forename_1 and
     * surname fields.
     *
     * @param code optional partial code filter (case-insensitive)
     * @param name optional partial title filter (case-insensitive)
     * @param addressLine1 optional partial address line 1 filter (case-insensitive)
     * @param from optional start date range filter
     * @param to optional end date range filter
     * @param active date to evaluate "active" on
     * @param pageable paging/sorting
     * @return page of matching entities
     */
    @Query(
            """
        SELECT
                c AS standardApplicant,
                COALESCE(
                    c.name,
                    NULLIF(
                        TRIM(
                            FUNCTION('concat_ws', ' ',
                                c.applicantTitle,
                                c.applicantForename1,
                                c.applicantSurname
                            )
                        ),
                        ''
                    )
                ) AS effectiveName
        FROM StandardApplicant c
        WHERE (:code IS NULL OR LOWER(c.applicantCode) LIKE CONCAT('%', LOWER(CAST(:code AS string)), '%')  ESCAPE '\\')
          AND (c.applicantStartDate <= :active)
          AND (c.applicantEndDate IS NULL OR c.applicantEndDate >= :active)
          AND (CAST(:from AS date) IS NULL OR c.applicantStartDate >= :from)
          AND (CAST(:to AS date) IS NULL OR c.applicantEndDate <= :to)
          AND (
              :addressLine1 IS NULL
              OR LOWER(c.addressLine1) LIKE CONCAT(
                  '%',
                  LOWER(CAST(:addressLine1 AS string)),
                  '%'
              ) ESCAPE '\\'
          )
          AND (:name IS NULL
                  OR (((c.name IS NOT NULL AND LOWER(c.name) LIKE CONCAT('%',
                          LOWER(CAST(:name AS string)), '%')  ESCAPE '\\')
                  OR (c.applicantForename1 IS NOT NULL AND LOWER(c.applicantForename1)
                          ILIKE CONCAT('%', LOWER(CAST(:name AS string)), '%')  ESCAPE '\\'))
                  OR (c.applicantSurname IS NOT NULL
                          AND LOWER(c.applicantSurname) LIKE CONCAT('%',
                                   LOWER(CAST(:name AS string)), '%')  ESCAPE '\\')))
        """)
    Page<StandardApplicantEnrichedProjection> search(
            @LikeParam @Param("code") String code,
            @LikeParam @Param("name") String name,
            @LikeParam @Param("addressLine1") String addressLine1,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("active") LocalDate active,
            Pageable pageable);
}
