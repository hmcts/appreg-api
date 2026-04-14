package uk.gov.hmcts.appregister.common.entity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.DatabaseJob;

@Repository
public interface DatabaseJobRepository extends JpaRepository<DatabaseJob, Long> {
    @Query("SELECT j FROM DatabaseJob j WHERE j.name = :name")
    DatabaseJob findByName(String name);
}
