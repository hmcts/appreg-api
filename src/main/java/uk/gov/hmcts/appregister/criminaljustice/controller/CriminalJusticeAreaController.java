package uk.gov.hmcts.appregister.criminaljustice.controller;

import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.appregister.generated.api.CriminalJusticeAreasApi;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaDto;

public class CriminalJusticeAreaController implements CriminalJusticeAreasApi {

    @Override
    public ResponseEntity<CriminalJusticeAreaDto> getCriminalJusticeAreaByCode(String code) {
        return CriminalJusticeAreasApi.super.getCriminalJusticeAreaByCode(code);
    }
}
