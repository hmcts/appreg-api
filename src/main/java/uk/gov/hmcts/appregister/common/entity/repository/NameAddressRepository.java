package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.NameAddress;

@Repository
public interface NameAddressRepository extends JpaRepository<NameAddress, Long> {
    /**
     * Finds all named address entry entities with an ID greater than or equal to the specified
     * value.
     *
     * @param value the minimum ID value (inclusive)
     * @return a list of NameAddress entry entities with IDs greater than or equal to the specified
     *     value
     */
    List<NameAddress> findByIdGreaterThanEqual(Integer value);

    /**
     * deletes the name address.
     *
     * @param id The entry id that the officials map to
     */
    @Modifying
    @Query(
            """
        DELETE FROM NameAddress na WHERE na.id = :id
        """)
    void deleteForId(Long id);
}
