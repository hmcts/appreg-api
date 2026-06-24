package uk.gov.hmcts.appregister.common.entity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.NameAddress;

@Repository
public interface NameAddressRepository extends JpaRepository<NameAddress, Long> {
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
