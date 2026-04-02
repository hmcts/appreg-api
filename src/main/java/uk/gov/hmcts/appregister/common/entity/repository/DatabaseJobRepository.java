package uk.gov.hmcts.appregister.common.entity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.DatabaseJob;

@Repository
public interface DatabaseJobRepository extends JpaRepository<DatabaseJob, Long> {
    DatabaseJob findByName(String name);
}
