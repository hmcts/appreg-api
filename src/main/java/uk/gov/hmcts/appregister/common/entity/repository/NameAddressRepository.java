package uk.gov.hmcts.appregister.common.entity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.NameAddress;

@Repository
public interface NameAddressRepository extends JpaRepository<NameAddress, Long> {}
