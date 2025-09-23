package uk.gov.hmcts.appregister.nationalcourthouse.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.generated.api.CourtLocationsApi;
import uk.gov.hmcts.appregister.generated.model.CourtLocationDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationPage;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Controller
public class CourtLocationController implements CourtLocationsApi {
    private final CourtLocationService courtLocationService;

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<CourtLocationPage> getCourtLocations(String name, String code, LocalDate date, Integer page, Integer size, List<String> sort) {
        return CourtLocationsApi.super.getCourtLocations(name, code, date, page, size, sort);
    }

    @Override
    public ResponseEntity<CourtLocationDto> getCourtLocationByCodeAndDate(String code, LocalDate date) {
        var courtLocationDto = courtLocationService.findByCodeAndDate(code, date);
    }
}
