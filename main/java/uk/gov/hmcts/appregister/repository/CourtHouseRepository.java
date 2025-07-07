package uk.gov.hmcts.appregister.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.appregister.model.CourtHouse;

public interface CourtHouseRepository extends JpaRepository<CourtHouse, Long> {}
