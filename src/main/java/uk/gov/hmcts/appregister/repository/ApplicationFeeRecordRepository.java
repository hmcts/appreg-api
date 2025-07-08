package uk.gov.hmcts.appregister.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.appregister.model.ApplicationFeeRecord;

public interface ApplicationFeeRecordRepository extends JpaRepository<ApplicationFeeRecord, Long> {}
