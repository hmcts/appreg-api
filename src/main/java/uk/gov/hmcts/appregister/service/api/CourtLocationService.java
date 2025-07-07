package uk.gov.hmcts.appregister.service.api;

import uk.gov.hmcts.appregister.dto.read.CourtHouseDto;

import java.util.List;

public interface CourtLocationService {
    List<CourtHouseDto> findAll();
    CourtHouseDto findById(Long id);
}
