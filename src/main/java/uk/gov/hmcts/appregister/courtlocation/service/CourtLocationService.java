package uk.gov.hmcts.appregister.courtlocation.service;

import java.util.List;
import uk.gov.hmcts.appregister.courtlocation.dto.CourtHouseDto;

public interface CourtLocationService {
    List<CourtHouseDto> findAll();

    CourtHouseDto findById(Long id);
}
