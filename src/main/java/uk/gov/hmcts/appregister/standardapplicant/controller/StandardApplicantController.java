package uk.gov.hmcts.appregister.standardapplicant.controller;

import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.appregister.common.api.ApiConstants.MediaTypes.VND_JSON_V1;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.common.util.CsvUtil;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.api.StandardApplicantsApi;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPage;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPrintDto;
import uk.gov.hmcts.appregister.standardapplicant.api.StandardApplicantSortFieldEnum;
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
    private final PageableMapper pageableMapper;

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<StandardApplicantPage> getStandardApplicants(
            String code,
            String name,
            String addressLine1,
            LocalDate from,
            LocalDate to,
            Integer pageNumber,
            Integer pageSize,
            List<String> sort) {
        sort = sort == null || sort.isEmpty() ? List.of() : sort;

        // Map OpenAPI paging params into a Spring Pageable with default sort by name ascending
        PagingWrapper pageable =
                pageableMapper.from(
                        pageNumber,
                        pageSize,
                        sort,
                        StandardApplicantSortFieldEnum.CODE,
                        Sort.Direction.ASC,
                        StandardApplicantSortFieldEnum::getEntityValue);

        StandardApplicantPage standardApplicantPage =
                service.findAll(code, name, addressLine1, from, to, pageable);
        return ResponseEntity.ok().body(standardApplicantPage);
    }

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<StandardApplicantGetDetailDto> getStandardApplicantByCode(String code) {

        StandardApplicantGetDetailDto standardApplicantGetDetailDto = service.findByCode(code);

        return ResponseEntity.status(OK)
                .varyBy("Accept")
                .contentType(VND_JSON_V1)
                .body(standardApplicantGetDetailDto);
    }

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<StandardApplicantPrintDto> printStandardApplicants(
            String code,
            String name,
            LocalDate from,
            LocalDate to,
            String addressLine1,
            List<String> sort) {

        sort = sort == null || sort.isEmpty() ? List.of() : sort;

        PagingWrapper pageable =
                pageableMapper.from(
                        0,
                        1,
                        sort,
                        StandardApplicantSortFieldEnum.CODE,
                        Sort.Direction.ASC,
                        StandardApplicantSortFieldEnum::getEntityValue);

        return ResponseEntity.status(OK)
                .varyBy("Accept")
                .contentType(VND_JSON_V1)
                .body(service.print(code, name, addressLine1, from, to, pageable));
    }

    @Override
    public ResponseEntity<String> standardApplicantsExport(
            @Nullable String code, @Nullable String name) {
        return ResponseEntity.ok(CsvUtil.escapeCSV(service.generateCsv(code, name)));
    }
}
