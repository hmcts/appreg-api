package uk.gov.hmcts.appregister.nationalcourthouse.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.appregister.nationalcourthouse.model.NationalCourtHouse;

/**
 * Spring Data repository for {@link NationalCourtHouse}.
 *
 * <p>Features:
 * <ul>
 *   <li>Paging &amp; sorting via {@link PagingAndSortingRepository}</li>
 *   <li>Specification support via {@link JpaSpecificationExecutor}</li>
 *   <li>A custom JPQL search that supports optional filters</li>
 * </ul>
 *
 * <p><b>Notes on the custom search:</b>
 * <ul>
 *   <li>All parameters are optional; when a parameter is {@code null}, its predicate is ignored.</li>
 *   <li>Name filtering is a case-insensitive {@code contains} match using {@code lower(...)}.</li>
 *   <li>Date range semantics:
 *     <ul>
 *       <li>{@code startFrom}/{@code startTo} bound the entity's {@code startDate} inclusively.</li>
 *       <li>{@code endFrom} treats {@code endDate = null} as "still active" (included) or a date
 *           on/after {@code endFrom}.</li>
 *       <li>{@code endTo} includes rows with {@code endDate} on/before {@code endTo}.</li>
 *     </ul>
 *   </li>
 *   <li>If you ever see a Postgres error like <i>"function lower(bytea) does not exist"</i>,
 *       ensure the bound parameter is a string/varchar. An alternative is to pre-build a
 *       {@code %pattern%} on the Java side and use
 *       {@code LOWER(n.name) LIKE :namePattern} (with {@code namePattern} already lower-cased).</li>
 *   <li>Make sure {@code n.name} matches the actual entity property (e.g. if your field is
 *       {@code courthouseName}, update the query accordingly).</li>
 * </ul>
 */
public interface NationalCourtHouseRepository
    extends PagingAndSortingRepository<NationalCourtHouse, Long>,
    JpaSpecificationExecutor<NationalCourtHouse> {

    /**
     * Searches {@link NationalCourtHouse} rows applying the provided (nullable) filters.
     *
     * <p>Sorting is provided by the {@link Pageable} argument unless you hardcode an
     * {@code ORDER BY} in the JPQL.</p>
     *
     * @param name      optional courthouse name fragment (case-insensitive contains)
     * @param courtType optional exact court type match
     * @param startFrom optional lower bound (inclusive) for {@code startDate}
     * @param startTo   optional upper bound (inclusive) for {@code startDate}
     * @param endFrom   optional lower bound (inclusive) for {@code endDate}; {@code null} endDate is treated as active
     * @param endTo     optional upper bound (inclusive) for {@code endDate}
     * @param pageable  page + size (+ optional sort)
     * @return a page of matching {@link NationalCourtHouse} entities
     */
    @Query("""
        SELECT n
        FROM NationalCourtHouse n
        WHERE (:courtType IS NULL OR n.courtType = :courtType)
          AND (:name IS NULL OR lower(n.name) LIKE concat('%', lower(cast(:name as string)), '%'))
          AND (:startFrom IS NULL OR n.startDate >= :startFrom)
          AND (:startTo   IS NULL OR n.startDate <= :startTo)
          AND (:endFrom  IS NULL OR n.endDate IS NULL OR n.endDate >= :endFrom)
          AND (:endTo    IS NULL OR n.endDate <= :endTo)
        """)
    Page<NationalCourtHouse> search(
        @Param("name") String name,
        @Param("courtType") String courtType,
        @Param("startFrom") LocalDate startFrom,
        @Param("startTo") LocalDate startTo,
        @Param("endFrom") LocalDate endFrom,
        @Param("endTo") LocalDate endTo,
        Pageable pageable
    );

    /**
     * Convenience method for single-row lookup.
     */
    Optional<NationalCourtHouse> findById(Long id);
}
