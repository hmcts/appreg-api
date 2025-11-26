package uk.gov.hmcts.appregister.common.entity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId;

public interface AppListEntryFeeRepository extends JpaRepository<AppListEntryFeeId, Long> {
}
