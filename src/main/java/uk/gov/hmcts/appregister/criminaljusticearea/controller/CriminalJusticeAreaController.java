package uk.gov.hmcts.appregister.criminaljusticearea.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea_;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.criminaljusticearea.service.CriminalJusticeService;
import uk.gov.hmcts.appregister.criminaljusticearea.validator.CriminalJusticeAreaSortValidator;
import uk.gov.hmcts.appregister.generated.api.CriminalJusticeAreasApi;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaPage;

/**
 * The core controller for the criminal justice area.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class CriminalJusticeAreaController implements CriminalJusticeAreasApi {
    private final CriminalJusticeService criminalJusticeService;
    private final PageableMapper appRegPageable;
    private final CriminalJusticeAreaSortValidator sortValidator;

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<CriminalJusticeAreaGetDto> getCriminalJusticeAreaByCode(String code) {
        CriminalJusticeAreaGetDto criminalJusticeAreaDto = criminalJusticeService.findByCode(code);
        return ResponseEntity.ok().body(criminalJusticeAreaDto);
    }

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<CriminalJusticeAreaPage> getCriminalJusticeAreas(
            String code, String description, Integer page, Integer size, List<String> sort) {
        // parse the pageable, supplying a default sort order if
        org.springframework.data.domain.Pageable pageable =
                appRegPageable.from(
                        page, size, sort, CriminalJusticeArea_.CODE, Sort.Direction.ASC);

        // validate the sort parameters
        pageable.getSort().get().forEach(o -> sortValidator.validate(o.getProperty()));

        CriminalJusticeAreaPage criminalJusticeAreaPage =
                criminalJusticeService.findAll(code, description, pageable);
        return ResponseEntity.ok().body(criminalJusticeAreaPage);
    }
}
