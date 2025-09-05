package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.Fee;

/** Repository interface for managing Application Fees. */
public interface FeeRepository extends JpaRepository<Fee, Long> {

    /**
     * Finds a list of Fee entities by their reference.
     *
     * @param reference the reference to search for
     * @return a list of Fee entities with the given reference
     */
    List<Fee> findByReference(String reference);

    // TODO:No offset in schema
    // List<Fee> findByReferenceAndIsOffset(String reference, boolean isOffset);

    /**
     * Finds ApplicationCode entities with IDs greater than or equal to the specified value.
     *
     * @param value the minimum ID value
     * @return a list of ApplicationCode entities with IDs >= value
     */
    List<ApplicationCode> findByIdGreaterThanEqual(Integer value);
}
