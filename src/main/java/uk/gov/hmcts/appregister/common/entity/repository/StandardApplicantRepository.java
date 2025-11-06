package uk.gov.hmcts.appregister.common.entity.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;

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
}
