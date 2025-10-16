package uk.gov.hmcts.appregister.common.entity.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;

/**
 * Repository interface for {@link ResolutionCode} entities.
 *
 * <p>This repository abstracts the persistence layer using Spring Data:
 *
 * <ul>
 *   <li>{@link PagingAndSortingRepository} – provides CRUD plus paging and sorting (e.g. {@code
 *       findAll(Pageable)}, {@code findAll(Sort)}).
 *   <li>{@link JpaSpecificationExecutor} – enables dynamic, typesafe filtering with JPA
 *       Specifications.
 * </ul>
 *
 * <p>No implementation is required—Spring Data generates a proxy at runtime. You can mix:
 *
 * <ul>
 *   <li>Derived query methods (e.g. {@code findByResultCode}).
 *   <li>Annotated JPQL queries (see {@link #search(String, String, LocalDate, LocalDate, LocalDate,
 *       LocalDate, Pageable)}).
 *   <li>Specification-based queries via {@link JpaSpecificationExecutor}.
 * </ul>
 */
public interface ResolutionCodeRepository
        extends PagingAndSortingRepository<ResolutionCode, Long>,
                JpaSpecificationExecutor<ResolutionCode> {

    /**
     * Find a single record by its business code (e.g., "RC123").
     *
     * @param code the exact code to look up (column {@code resolution_code})
     * @return an {@link Optional} containing the entity when found, otherwise empty
     */
    Optional<ResolutionCode> findByResultCode(String code);

    /**
     * Search for result codes using optional, case-insensitive filters with pagination.
     *
     * <p>Semantics:
     *
     * <ul>
     *   <li>{@code code}: partial, case-insensitive match on {@code resolution_code} (ILIKE).
     *   <li>{@code title}: partial, case-insensitive match on {@code resolution_code_title}
     *       (ILIKE).
     *   <li>{@code startFrom}/{@code startTo}: inclusive bounds on {@code
     *       resolution_code_start_date}.
     *   <li>{@code endFrom}: include records where {@code resolution_code_end_date} is
     *       <em>null</em> (treated as ongoing) <strong>or</strong> {@code >= endFrom}.
     *   <li>{@code endTo}: inclusive upper bound on {@code resolution_code_end_date} (nulls do not
     *       match this predicate).
     *   <li>Any null parameter disables that particular filter.
     * </ul>
     *
     * @param code optional partial code filter (case-insensitive)
     * @param title optional partial title filter (case-insensitive)
     * @param startFrom optional lower bound (inclusive) for start date
     * @param startTo optional upper bound (inclusive) for start date
     * @param endFrom optional lower bound (inclusive) for end date; also includes null end dates
     * @param endTo optional upper bound (inclusive) for end date
     * @param pageable standard Spring Data pagination/sorting descriptor
     * @return paginated result set matching the applied filters
     */
    @Query(
            """
            SELECT r
            FROM ResolutionCode r
            WHERE (:code  IS NULL OR lower(r.resultCode)  LIKE concat('%',
                    lower(cast(:code  as string)), '%'))
              AND (:title IS NULL OR lower(r.title) LIKE concat('%',
                       lower(cast(:title as string)), '%'))
              AND (:startFrom IS NULL OR r.startDate >= :startFrom)
              AND (:startTo   IS NULL OR r.startDate <= :startTo)
              AND (:endFrom  IS NULL OR r.endDate IS NULL OR r.endDate >= :endFrom)
              AND (:endTo    IS NULL OR r.endDate <= :endTo)
            """)
    Page<ResolutionCode> search(
            @Param("code") String code,
            @Param("title") String title,
            @Param("startFrom") LocalDate startFrom,
            @Param("startTo") LocalDate startTo,
            @Param("endFrom") LocalDate endFrom,
            @Param("endTo") LocalDate endTo,
            Pageable pageable);

    /**
     * Find a single record by its primary key identifier.
     *
     * <p>This method is inherited from {@link org.springframework.data.repository.CrudRepository},
     * but is redeclared here for clarity and to provide documentation alongside other custom
     * queries. Returns an {@link Optional} so that callers must handle the "not found" case
     * explicitly.
     *
     * <p><strong>Usage example:</strong>
     *
     * <pre>{@code
     * Optional<ResolutionCode> maybeCode = resolutionCodeRepository.findById(123L);
     * ResolutionCode code = maybeCode.orElseThrow(() ->
     * new ResponseStatusException(HttpStatus.NOT_FOUND));
     * }</pre>
     *
     * @param id the {@code rc_id} primary key value
     * @return an {@link Optional} containing the entity if found, or empty if not
     */
    Optional<ResolutionCode> findById(Long id);

    List<ResolutionCode> findByIdGreaterThanEqual(Integer value);

    /**
     * Find active Resolution Codes by code on a given date window.
     *
     * <p>Active if: {@code rc.startDate < :tomorrowStart} and
     * ({@code rc.endDate IS NULL} or {@code rc.endDate >= :todayStart}).
     * Code match is case-insensitive equality on {@code rc.resultCode}.
     *
     * @param code case-insensitive business code (e.g. "ABC123")
     * @param todayStart inclusive window start (local day start)
     * @param tomorrowStart exclusive window end (next day start)
     * @return zero, one, or many rows; service layer enforces uniqueness
     */
    @Query("""
          SELECT rc
          FROM ResolutionCode rc
          WHERE LOWER(rc.resultCode) = LOWER(CAST(:code AS string))
          AND rc.startDate < :tomorrowStart
          AND (rc.endDate IS NULL OR rc.endDate >= :todayStart)
          """)
    List<ResolutionCode> findActiveResolutionCodesOnDateByCode(
        @Param("code")      String code,
        @Param("todayStart") LocalDateTime todayStart,
        @Param("tomorrowStart")   LocalDateTime tomorrowStart);

    /**
     * Retrieve a page of active Resolution Codes filtered by code/title (case-insensitive).
     *
     * <p>Filters (applied when non-null):
     * <ul>
     *   <li>{@code code}: partial match on {@code rc.resultCode}</li>
     *   <li>{@code title}: partial match on {@code rc.title}</li>
     * </ul>
     *
     * <p>Active if: {@code rc.startDate < :tomorrowStart} and
     * ({@code rc.endDate IS NULL} or {@code rc.endDate >= :todayStart}).
     *
     * @param code optional partial code filter (case-insensitive)
     * @param title optional partial title filter (case-insensitive)
     * @param todayStart inclusive window start (local day start)
     * @param tomorrowStart exclusive window end (next day start)
     * @param pageable paging/sorting
     * @return page of matching entities
     */
    @Query("""
         SELECT rc
         FROM ResolutionCode rc
         WHERE (:code IS NULL OR LOWER(rc.resultCode) LIKE CONCAT('%', LOWER(CAST(:code AS string)), '%'))
         AND (:title IS NULL OR LOWER(rc.title) LIKE CONCAT('%', LOWER(CAST(:title as string)), '%'))
         AND rc.startDate < :tomorrowStart
         AND (rc.endDate IS NULL OR rc.endDate >= :todayStart)
         """)
    Page<ResolutionCode> findActiveOnDate(
        @Param("code")  String code,
        @Param("title") String title,
        @Param("todayStart") LocalDateTime todayStart,
        @Param("tomorrowStart") LocalDateTime tomorrowStart,
        Pageable pageable);
}
