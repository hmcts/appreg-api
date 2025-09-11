package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;

public interface AppListEntryFeeStatusRepository
        extends JpaRepository<AppListEntryFeeStatus, Long> {
    /**
     * Finds all ApplicationCode entities with an ID greater than or equal to the specified value.
     *
     * @param value the minimum ID value (inclusive)
     * @return a list of ApplicationCode entities with IDs greater than or equal to the specified
     *     value
     */
    List<AppListEntryFeeStatus> findByIdGreaterThanEqual(Integer value);
}
