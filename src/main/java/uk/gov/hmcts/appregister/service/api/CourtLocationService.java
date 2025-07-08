package uk.gov.hmcts.appregister.service.api;

import java.util.List;
import uk.gov.hmcts.appregister.dto.read.CourtHouseDto;

public interface CourtLocationService {
    List<CourtHouseDto> findAll();

    CourtHouseDto findById(Long id);
}
