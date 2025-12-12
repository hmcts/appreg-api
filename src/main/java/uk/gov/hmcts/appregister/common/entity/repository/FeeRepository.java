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
     * Finds Fee entities with IDs greater than or equal to the specified value.
     *
     * @param value the minimum ID value
     * @return a list of Fee entities with IDs >= value
     */
    List<Fee> findByIdGreaterThanEqual(Integer value);

    /**
     * find the fee associated fees with the given ids that is within the window of the date.
     *
     * @param ids The fee ids
     * @param date The date that the fee should be valid for
     * @return The fee objects if found
     */
    @Query(
            """
        SELECT f
        FROM Fee f
        WHERE (f.id IN :ids) AND
        ((f.endDate IS NULL OR  f.endDate >= :date)
        AND f.startDate <= :date)
        """)
    List<Fee> findByIdsBetweenDate(List<Long> ids, LocalDate date);
}
