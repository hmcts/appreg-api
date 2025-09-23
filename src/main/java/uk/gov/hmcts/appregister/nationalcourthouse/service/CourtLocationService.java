package uk.gov.hmcts.appregister.nationalcourthouse.service;

import uk.gov.hmcts.appregister.generated.model.CourtLocationDto;

import java.time.LocalDate;

public interface CourtLocationService {
    CourtLocationDto getByCodeAndDate(String code, LocalDate date);
}
