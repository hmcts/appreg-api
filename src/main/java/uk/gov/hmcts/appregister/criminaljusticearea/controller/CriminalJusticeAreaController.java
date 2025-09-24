package uk.gov.hmcts.appregister.criminaljusticearea.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.criminaljusticearea.service.CriminalJusticeService;
import uk.gov.hmcts.appregister.generated.api.CriminalJusticeAreasApi;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;

/** The core controller for the criminal justice area. */
@RequiredArgsConstructor
@Controller
public class CriminalJusticeAreaController implements CriminalJusticeAreasApi {
    private final CriminalJusticeService criminalJusticeService;

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<CriminalJusticeAreaGetDto> getCriminalJusticeAreaByCode(String code) {
        CriminalJusticeAreaGetDto criminalJusticeAreaDto = criminalJusticeService.findByCode(code);
        return ResponseEntity.ok().body(criminalJusticeAreaDto);
    }
}
