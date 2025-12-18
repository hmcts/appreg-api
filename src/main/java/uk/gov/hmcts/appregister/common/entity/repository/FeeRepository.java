package uk.gov.hmcts.appregister.common.entity.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.appregister.common.entity.Fee;

/**
 * Repository interface for managing Application Fees.
 */
public interface FeeRepository extends JpaRepository<Fee, Long> {

    /**
     * Finds a list of Fee entities by their reference.
     *
     * @param reference the reference to search for
     * @return a list of Fee entities with the given reference
     */
    List<Fee> findByReference(String reference);

    /**
     * Finds a list of Fee entities by their reference and offset status.
     *
     * @param reference the reference to search for
     * @return fee entities matching the reference and offset status
     */
    @Query(
            """
        SELECT f
        FROM Fee f
        WHERE (f.reference = :reference) AND
          ((f.endDate IS NULL OR  f.endDate >= :dateTime)
                  AND f.startDate <= :dateTime)
        """)
    List<Fee> findByReferenceBetweenDate(String reference, LocalDate dateTime);

    /**
     * Finds a list of Fee entities by their reference and offset status.
     *
     * @param reference the reference to search for
     * @return fee entities matching the reference and offset status
     */
    @Query(
            """
        SELECT f
        FROM Fee f
        WHERE (f.reference = :reference) AND
        ((f.endDate IS NULL OR  f.endDate >= :dateTime)
        AND f.startDate <= :dateTime) AND f.isOffsite = :offsiteStatus
        """)
    List<Fee> findByReferenceBetweenDateWithOffsite(
            String reference, LocalDate dateTime, boolean offsiteStatus);

    /**
     * Finds ApplicationCode entities with IDs greater than or equal to the specified value.
     *
     * @param value the minimum ID value
     * @return a list of ApplicationCode entities with IDs >= value
     */
    List<Fee> findByIdGreaterThanEqual(Integer value);
}
