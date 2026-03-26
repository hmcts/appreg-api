package uk.gov.hmcts.appregister.common.entity.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;

/**
 * Spring Data JPA repository for {@link NationalCourtHouse} entities.
 *
 * <p>Provides access to National Court Houses (NCH) stored in the {@code national_court_houses}
 * table. Includes custom queries for retrieving active court locations by code/date and paginated
 * searches with optional filters.
 */
public interface NationalCourtHouseRepository extends JpaRepository<NationalCourtHouse, Long> {

    /**
     * Find an active Court Location of type CHOA by code and date.
     *
     * <p>Matches records where:
     *
     * <ul>
     *   <li>{@code courtType} = CHOA
     *   <li>{@code courtLocationCode} equals {@code code}, case-insensitive
     *   <li>{@code startDate} is on or before {@code date}
     *   <li>{@code endDate} is {@code null} or on/after {@code date}
     * </ul>
     *
     * <p>This query may return zero, one, or multiple results. Service layer is responsible for
     * enforcing uniqueness.
     *
     * @param code business identifier for the Court Location
     * @param date date on which the Court Location must be valid
     * @return list of matching active courts
     */
    @Query(
            """
        SELECT nch
        FROM NationalCourtHouse nch
        WHERE nch.courtType = 'CHOA'
          AND LOWER(nch.courtLocationCode) = LOWER(CAST(:code AS string))
          AND nch.startDate <= :date
          AND (nch.endDate IS NULL OR nch.endDate >= :date)
        ORDER BY CASE WHEN nch.endDate IS NULL THEN 0 ELSE 1 END,
                 nch.endDate DESC,
                 nch.startDate DESC,
                 nch.id DESC
        """)
    List<NationalCourtHouse> findActiveCourtsWithDate(
            @Param("code") String code, @Param("date") LocalDate date);

    /**
     * Find an active Court Location of type CHOA by code.
     *
     * <p>Matches records where:
     *
     * <ul>
     *   <li>{@code courtType} = CHOA
     *   <li>{@code courtLocationCode} equals {@code code}, case-insensitive
     *   <li>{@code startDate} is on or before today
     *   <li>{@code endDate} is {@code null} or on/after today
     * </ul>
     *
     * <p>This query may return zero, one, or multiple results. Service layer is responsible for
     * enforcing uniqueness.
     *
     * @param code business identifier for the Court Location
     * @param date active date to evaluate against
     * @return list of matching active courts
     */
    @Query(
            """
        SELECT nch
        FROM NationalCourtHouse nch
        WHERE nch.courtType = 'CHOA'
          AND LOWER(nch.courtLocationCode) = LOWER(CAST(:code AS string))
          AND nch.startDate <= :date
          AND (nch.endDate IS NULL OR nch.endDate >= :date)
        ORDER BY CASE WHEN nch.endDate IS NULL THEN 0 ELSE 1 END,
                 nch.endDate DESC,
                 nch.startDate DESC,
                 nch.id DESC
        """)
    List<NationalCourtHouse> findActiveCourts(
            @Param("code") String code, @Param("date") LocalDate date);

    /**
     * Retrieve a paginated list of active Court Locations of type CHOA.
     *
     * <p>Filters applied if non-null:
     *
     * <ul>
     *   <li>{@code code} — case-insensitive partial match on courtLocationCode
     *   <li>{@code name} — case-insensitive partial match on courthouse name
     * </ul>
     *
     * @param code optional filter for court location code
     * @param name optional filter for courthouse name
     * @param date active date to evaluate against
     * @param pageable Spring Data paging and sorting configuration
     * @return page of matching Court Locations
     */
    @Query(
            """
        SELECT nch
        FROM NationalCourtHouse nch
        WHERE nch.courtType = 'CHOA'
          AND nch.startDate <= :date
          AND (nch.endDate IS NULL OR nch.endDate >= :date)
          AND (:code IS NULL OR LOWER(nch.courtLocationCode) LIKE CONCAT('%', LOWER(CAST(:code AS string)), '%'))
          AND (:name IS NULL OR LOWER(nch.name) LIKE CONCAT('%', LOWER(CAST(:name AS string)), '%'))
        """)
    Page<NationalCourtHouse> findAllActiveCourts(
            @Param("code") String code,
            @Param("name") String name,
            @Param("date") LocalDate date,
            Pageable pageable);

    Optional<NationalCourtHouse> findById(Long id);

    List<NationalCourtHouse> findByIdGreaterThanEqual(Integer value);
}
