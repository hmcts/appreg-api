package uk.gov.hmcts.appregister.common.entity.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.appregister.common.entity.AsyncJobsAppListEntry;

public interface AsyncJobAppListEntryRepository extends JpaRepository<AsyncJobsAppListEntry, Long> {
    List<AsyncJobsAppListEntry> findByAsyncJobId(UUID asyncJobId);
}
