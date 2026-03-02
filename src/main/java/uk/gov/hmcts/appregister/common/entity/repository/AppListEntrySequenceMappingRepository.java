package uk.gov.hmcts.appregister.common.entity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.common.entity.AppListEntrySequenceMapping;

@Repository
public interface AppListEntrySequenceMappingRepository
        extends JpaRepository<AppListEntrySequenceMapping, Long> {}
