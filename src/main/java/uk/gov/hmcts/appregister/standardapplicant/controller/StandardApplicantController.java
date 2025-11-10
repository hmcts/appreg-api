package uk.gov.hmcts.appregister.standardapplicant.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.appregister.common.api.SortableField;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant_;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.mapper.SortMapper;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.generated.api.StandardApplicantsApi;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPage;
import uk.gov.hmcts.appregister.standardapplicant.api.StandardApplicantSortFieldEnum;
import uk.gov.hmcts.appregister.standardapplicant.dto.StandardApplicantDto;
import uk.gov.hmcts.appregister.standardapplicant.service.StandardApplicantService;

/**
 * Controller for managing standard applicants.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class StandardApplicantController implements StandardApplicantsApi {
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

    @Operation(summary = "Get a standard applicant by ID", operationId = "getStandardApplicantById")
    @ApiResponse(responseCode = "200", description = "Standard applicant found")
    @ApiResponse(responseCode = "404", description = "Standard applicant not found")
    @GetMapping("/standard-applicants/{id}")
    @Deprecated
    public ResponseEntity<StandardApplicantDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }
}
