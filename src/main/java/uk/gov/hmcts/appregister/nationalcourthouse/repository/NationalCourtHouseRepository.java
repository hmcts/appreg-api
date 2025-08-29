package uk.gov.hmcts.appregister.nationalcourthouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uk.gov.hmcts.appregister.nationalcourthouse.model.NationalCourtHouse;

/**
 * Repository interface for accessing {@link NationalCourtHouse} entities.
 *
 * <p>This interface combines:
 *
 * <ul>
 *   <li>{@link JpaRepository} – provides basic CRUD operations and pagination methods for {@code
 *       CourtLocation} entities using their {@code Long} ID.
 *   <li>{@link JpaSpecificationExecutor} – enables execution of dynamic JPA {@link
 *       org.springframework.data.jpa.domain.Specification} queries, allowing advanced filtering
 *       (e.g. by name, type, date ranges).
 * </ul>
 *
 * <p>Because it extends Spring Data interfaces, no implementation is required: Spring generates a
 * proxy at runtime. You can add custom query methods here if more complex lookups are needed (e.g.
 * {@code findByNameContainingIgnoreCase}).
 *
 * <p>Typical usage:
 *
 * <pre>
 *   List&lt;CourtLocation&gt; results = courtLocationRepository.findAll();
 *   Page&lt;CourtLocation&gt; page = courtLocationRepository.findAll(spec, pageable);
 * </pre>
 */
public interface NationalCourtHouseRepository
        extends JpaRepository<NationalCourtHouse, Long>, JpaSpecificationExecutor<NationalCourtHouse> {
    // No additional methods required yet – Spring Data generates CRUD and spec-based methods.
}
