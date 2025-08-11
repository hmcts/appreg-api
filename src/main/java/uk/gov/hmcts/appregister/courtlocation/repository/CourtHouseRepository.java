package uk.gov.hmcts.appregister.courtlocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.appregister.courtlocation.model.CourtHouse;

public interface CourtHouseRepository extends JpaRepository<CourtHouse, Long> {}
