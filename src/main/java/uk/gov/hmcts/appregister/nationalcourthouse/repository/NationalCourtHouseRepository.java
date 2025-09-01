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
 * Spring Data repository for {@link NationalCourtHouse} entities.
 *
 * <p><strong>Base interfaces:</strong>
 *
 * <ul>
 *   <li>{@link PagingAndSortingRepository} – provides basic CRUD operations, plus {@code
 *       findAll(Pageable)} and {@code findAll(Sort)} for pagination and sorting.
 *   <li>{@link JpaSpecificationExecutor} – enables execution of dynamic JPA {@link
 *       org.springframework.data.jpa.domain.Specification}-based queries for advanced filtering
 *       scenarios.
 * </ul>
 *
 * <p><strong>Runtime behavior:</strong> Spring generates the implementation automatically at
 * runtime, so you only define the contract here. Typical usage is from the service layer, which
 * delegates persistence and filtering logic to this repository.
 *
 * <p><strong>Custom queries:</strong>
 *
 * <ul>
 *   <li>{@link #search(String, String, LocalDate, LocalDate, LocalDate, LocalDate, Pageable)} – a
 *       JPQL query that applies multiple optional filters (name, court type, start date range, end
 *       date range) in one go. Each filter is ignored if the corresponding parameter is {@code
 *       null}.
 *   <li>{@link #findById(Long)} – overridden here explicitly for clarity, but already inherited
 *       from {@code CrudRepository}. Returns an {@link Optional} so that callers handle the “not
 *       found” case explicitly.
 * </ul>
 *
 * <p><strong>Example usage:</strong>
 *
 * <pre>{@code
 * Page<NationalCourtHouse> page = repository.search(
 *     "cardiff", "CROWN",
 *     LocalDate.of(2020, 1, 1),
 *     null,
 *     null,
 *     null,
 *     PageRequest.of(0, 10, Sort.by("name").ascending())
 * );
 *
 * Optional<NationalCourtHouse> maybeCourt = repository.findById(123L);
 * }</pre>
 */
public interface NationalCourtHouseRepository
        extends PagingAndSortingRepository<NationalCourtHouse, Long>,
                JpaSpecificationExecutor<NationalCourtHouse> {

    /**
     * Search for court houses with optional filters and paging.
     *
     * <p>Filters:
     *
     * <ul>
     *   <li>{@code name} – case-insensitive substring match.
     *   <li>{@code courtType} – exact match.
     *   <li>{@code startFrom}/{@code startTo} – inclusive range filter on {@code startDate}.
     *   <li>{@code endFrom}/{@code endTo} – inclusive range filter on {@code endDate}. Records with
     *       {@code endDate IS NULL} are treated as ongoing and match when an {@code endFrom} bound
     *       is supplied.
     * </ul>
     *
     * @param name optional case-insensitive substring filter
     * @param courtType optional exact-match court type filter
     * @param startFrom optional lower bound for start date (inclusive)
     * @param startTo optional upper bound for start date (inclusive)
     * @param endFrom optional lower bound for end date (inclusive)
     * @param endTo optional upper bound for end date (inclusive)
     * @param pageable Spring Data {@link Pageable} for pagination and sorting
     * @return a page of matching {@link NationalCourtHouse} entities
     */
    @Query(
            """
        select n
          from NationalCourtHouse n
         where (:name is null or lower(n.name) like lower(concat('%', :name, '%')))
           and (:courtType is null or n.courtType = :courtType)
           and (:startFrom is null or n.startDate >= :startFrom)
           and (:startTo   is null or n.startDate <= :startTo)
           and (:endFrom   is null or (n.endDate is null or n.endDate >= :endFrom))
           and (:endTo     is null or n.endDate <= :endTo)
        """)
    Page<NationalCourtHouse> search(
            @Param("name") String name,
            @Param("courtType") String courtType,
            @Param("startFrom") LocalDate startFrom,
            @Param("startTo") LocalDate startTo,
            @Param("endFrom") LocalDate endFrom,
            @Param("endTo") LocalDate endTo,
            Pageable pageable);

    /**
     * Find a single courthouse by its ID.
     *
     * @param id the primary key identifier
     * @return an {@link Optional} containing the entity if found, otherwise empty
     */
    Optional<NationalCourtHouse> findById(Long id);
}
