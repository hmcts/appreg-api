package uk.gov.hmcts.appregister.applicationentry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.appregister.applicationentry.model.ApplicationFeeRecord;

public interface ApplicationFeeRecordRepository extends JpaRepository<ApplicationFeeRecord, Long> {}
