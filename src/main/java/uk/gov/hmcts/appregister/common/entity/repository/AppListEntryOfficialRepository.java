package uk.gov.hmcts.appregister.common.entity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;

import java.util.List;

public interface AppListEntryOfficialRepository extends JpaRepository<AppListEntryOfficial, Long> {
    /**
     * Finds all Official entry entities with an ID greater than or equal to the specified value.
     *
     * @param value the minimum ID value (inclusive)
     * @return a list of Official entry entities with IDs greater than or equal to the specified
     *     value
     */
    List<AppListEntryOfficial> findByIdGreaterThanEqual(Integer value);

}
