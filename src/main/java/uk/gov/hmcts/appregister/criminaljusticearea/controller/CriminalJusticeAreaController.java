package uk.gov.hmcts.appregister.criminaljusticearea.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.criminaljusticearea.api.CriminalJusticeSortFieldEnum;
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
        PagingWrapper pageable =
                appRegPageable.from(
                        page,
                        size,
                        sort,
                        CriminalJusticeSortFieldEnum.CODE,
                        Sort.Direction.ASC,
                        CriminalJusticeSortFieldEnum::getEntityValue);

        CriminalJusticeAreaPage criminalJusticeAreaPage =
                criminalJusticeService.findAll(code, description, pageable);
        return ResponseEntity.ok().body(criminalJusticeAreaPage);
    }
}
