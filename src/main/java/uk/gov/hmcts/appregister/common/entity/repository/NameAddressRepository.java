package uk.gov.hmcts.appregister.common.entity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.NameAddress;

import java.util.List;

@Repository
public interface NameAddressRepository extends JpaRepository<NameAddress, Long> {
    /**
     * Finds all named address entry entities with an ID greater than or equal to the specified value.
     *
     * @param value the minimum ID value (inclusive)
     * @return a list of NameAddress entry entities with IDs greater than or equal to the specified
     *     value
     */
    List<NameAddress> findByIdGreaterThanEqual(Integer value);
}
