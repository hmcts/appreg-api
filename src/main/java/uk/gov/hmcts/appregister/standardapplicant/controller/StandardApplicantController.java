package uk.gov.hmcts.appregister.standardapplicant.controller;

import static org.springframework.http.HttpStatus.OK;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.appregister.common.api.SortableField;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant_;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.mapper.SortMapper;
import uk.gov.hmcts.appregister.common.security.RoleNames;
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
    private final SortMapper sortMapper;

    // Maps and validates API sort parameters to entity field names.
    private final PageableMapper pageableMapper;

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<StandardApplicantPage> getStandardApplicants(
            String code, String title, Integer page, Integer size, List<String> sort) {
        sort = sort == null || sort.isEmpty() ? List.of() : sort;

        // map the sort parameters from OpenAPI to entity fields
        sort =
                sortMapper.map(
                        SortableField.of(sort.toArray(new String[0])),
                        StandardApplicantSortFieldEnum::getEntityValue);

        // Map OpenAPI paging params into a Spring Pageable with default sort by name ascending
        Pageable pageable =
                pageableMapper.from(
                        page, size, sort, StandardApplicant_.APPLICANT_CODE, Sort.Direction.ASC);

        log.info(
                "getStandardApplicants: code: {}, title: {}, page: {}, size: {}",
                code,
                title,
                page,
                size);
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
