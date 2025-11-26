package uk.gov.hmcts.appregister.common.entity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;

public interface AppListEntryOfficialRepository extends JpaRepository<AppListEntryOfficial, Long> {
}
