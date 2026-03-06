package uk.gov.hmcts.appregister.standardapplicant.controller;

import static org.springframework.http.HttpStatus.OK;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.api.StandardApplicantsApi;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPage;
import uk.gov.hmcts.appregister.standardapplicant.api.StandardApplicantSortFieldEnum;
import uk.gov.hmcts.appregister.standardapplicant.service.StandardApplicantService;

/**
 * Controller for managing standard applicants.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class StandardApplicantController implements StandardApplicantsApi {
    private static final MediaType VND_JSON_V1 =
            MediaType.parseMediaType("application/vnd.hmcts.appreg.v1+json");

    private final StandardApplicantService service;

    // Maps and validates API sort parameters to entity field names.
    private final PageableMapper pageableMapper;

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<StandardApplicantPage> getStandardApplicants(
            String code, String title, Integer page, Integer size, List<String> sort) {
        sort = sort == null || sort.isEmpty() ? List.of() : sort;

        // Map OpenAPI paging params into a Spring Pageable with default sort by name ascending
        PagingWrapper pageable =
                pageableMapper.from(
                        page,
                        size,
                        sort,
                        StandardApplicantSortFieldEnum.CODE,
                        Sort.Direction.ASC,
                        StandardApplicantSortFieldEnum::getEntityValue);

        StandardApplicantPage standardApplicantPage = service.findAll(code, title, pageable);
        return ResponseEntity.ok().body(service.findAll(code, title, pageable));
    }

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<StandardApplicantGetDetailDto> getStandardApplicantByCodeAndDate(
            String code, LocalDate date) {

        StandardApplicantGetDetailDto standardApplicantGetDetailDto =
                service.findByCode(code, date);

        return ResponseEntity.status(OK)
                .varyBy("Accept")
                .contentType(VND_JSON_V1)
                .body(standardApplicantGetDetailDto);
    }
}
