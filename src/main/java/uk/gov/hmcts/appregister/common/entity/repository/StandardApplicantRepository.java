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
     * Finds the highest-ID date-valid version for an applicant code.
     *
     * @param code the applicant code to search for
     * @param date The date to check for active status
     * @return at most one matching StandardApplicant
     */
    @Query(
            """
        SELECT sa
        FROM StandardApplicant sa
        WHERE LOWER(sa.applicantCode) = LOWER(CAST(:code AS string))
        AND sa.id = (
            SELECT MAX(v.id) FROM StandardApplicant v
            WHERE LOWER(v.applicantCode) = LOWER(sa.applicantCode)
              AND v.applicantStartDate <= :date
              AND (v.applicantEndDate IS NULL OR v.applicantEndDate >= :date)
        )
        """)
    List<StandardApplicant> findStandardApplicantByCodeAndDate(
            @Param("code") String code, @Param("date") LocalDate date);

    /** Returns the highest-ID date-valid version per applicant code. */
    @Query(
            """
            SELECT sa
            FROM StandardApplicant sa
            WHERE sa.id = (
                SELECT MAX(v.id) FROM StandardApplicant v
                WHERE LOWER(v.applicantCode) = LOWER(sa.applicantCode)
                  AND v.applicantStartDate <= :date
                  AND (v.applicantEndDate IS NULL OR v.applicantEndDate >= :date)
            )
            ORDER BY LOWER(sa.applicantCode)
            """)
    List<StandardApplicant> findAllByDate(@Param("date") LocalDate date);

    /**
     * Finds the highest-ID version by applicant code, without date filtering.
     *
     * @param code the applicant code to search for
     * @return at most one matching StandardApplicant
     */
    @Query(
            """
        SELECT sa
        FROM StandardApplicant sa
        WHERE LOWER(sa.applicantCode) = LOWER(CAST(:code AS string))
        AND sa.id = (
            SELECT MAX(v.id) FROM StandardApplicant v
            WHERE LOWER(v.applicantCode) = LOWER(sa.applicantCode)
        )
        """)
    List<StandardApplicant> findStandardApplicantByCode(@Param("code") String code);

    /**
     * Retrieve a page of active Standard Applicant codes filtered by code/name (case-insensitive).
     *
     * <p>Active if: c.startDate <= :date AND (c.endDate IS NULL OR c.endDate >= :date)
     *
     * <p>Display and sorting use only the reference name, never personal name fields. Legacy search
     * filters remain supported; personal and contact details are not returned.
     *
     * @param code optional partial code filter (case-insensitive)
     * @param name optional partial applicant display-name filter (case-insensitive)
     * @param addressLine1 optional partial address line 1 filter (case-insensitive)
     * @param from optional start date overlap filter
     * @param to optional end date overlap filter
     * @param active date to evaluate "active" on when no date range is supplied
     * @param pageable paging/sorting
     * @return page of matching entities
     */
    @Query(
            """
        SELECT
                c AS standardApplicant,
                c.name AS effectiveName
        FROM StandardApplicant c
        WHERE (:code IS NULL OR LOWER(c.applicantCode) LIKE CONCAT('%', LOWER(CAST(:code AS string)), '%')  ESCAPE '\\')
          AND c.id = (
            SELECT MAX(v.id) FROM StandardApplicant v
            WHERE LOWER(v.applicantCode) = LOWER(c.applicantCode)
            AND (
              (
                  CAST(:from AS date) IS NULL
                  AND CAST(:to AS date) IS NULL
                  AND v.applicantStartDate <= :active
                  AND (v.applicantEndDate IS NULL OR v.applicantEndDate >= :active)
              )
              OR (
                  CAST(:from AS date) IS NOT NULL
                  AND CAST(:to AS date) IS NOT NULL
                  AND v.applicantStartDate <= :to
                  AND (v.applicantEndDate IS NULL OR v.applicantEndDate >= :from)
              )
              OR (
                  CAST(:from AS date) IS NOT NULL
                  AND CAST(:to AS date) IS NULL
                  AND (v.applicantEndDate IS NULL OR v.applicantEndDate >= :from)
              )
              OR (
                  CAST(:from AS date) IS NULL
                  AND CAST(:to AS date) IS NOT NULL
                  AND v.applicantStartDate <= :to
              )
            )
          )
          AND (
              :addressLine1 IS NULL
              OR LOWER(c.addressLine1) LIKE CONCAT(
                  '%',
                  LOWER(CAST(:addressLine1 AS string)),
                  '%'
              ) ESCAPE '\\'
          )
          AND (
              :name IS NULL
              OR (
                  c.name IS NOT NULL
                  AND LOWER(c.name) LIKE CONCAT('%', LOWER(CAST(:name AS string)), '%') ESCAPE '\\'
              )
              OR (
                  c.applicantForename2 IS NOT NULL
                  AND LOWER(c.applicantForename2)
                      LIKE CONCAT('%', LOWER(CAST(:name AS string)), '%') ESCAPE '\\'
              )
              OR (
                  c.applicantForename3 IS NOT NULL
                  AND LOWER(c.applicantForename3)
                      LIKE CONCAT('%', LOWER(CAST(:name AS string)), '%') ESCAPE '\\'
              )
              OR LOWER(FUNCTION('concat_ws', ' ', c.applicantForename1, c.applicantSurname))
                  LIKE CONCAT('%', LOWER(CAST(:name AS string)), '%') ESCAPE '\\'
          )
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
