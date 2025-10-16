package uk.gov.hmcts.appregister.resultcode.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.hmcts.appregister.common.entity.ResolutionCode_;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.generated.api.ResultCodesApi;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ResultCodePage;
import uk.gov.hmcts.appregister.resultcode.service.ResultCodeService;
import uk.gov.hmcts.appregister.resultcode.validator.ResultCodeSortValidator;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for retrieving Result Codes.
 *
 * <p>Implements the operations defined in the OpenAPI-generated {@link ResultCodesApi}. Provides
 * endpoints to:
 *
 * <ul>
 *   <li>Retrieve a specific Result Code by code and effective date.
 *   <li>Retrieve a paginated list of active Court Locations of type CHOA with optional filters.
 * </ul>
 *
 * <p>All endpoints require the caller to have either user or admin role restrictions as enforced by
 * {@link RoleNames#USER_ROLE_OR_ADMIN_ROLE_RESTRICTION}.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
public class ResultCodeController implements ResultCodesApi {

    // Service layer providing Result Code business logic.
    private final ResultCodeService resultCodeService;

    // Mapper converting OpenAPI paging params to Spring Data {@link Pageable}.
    private final PageableMapper pageableMapper;

    // Validator ensuring requested sort fields are valid for Result Codes.
    private final ResultCodeSortValidator sortValidator;

    /**
     * Retrieve a single Result Code by its code and a date where the Result Code is "Active".
     *
     * <p>Delegates to {@link ResultCodeService#findByCodeAndDate(String, LocalDate)}. If no
     * active court is found, the service layer throws a domain-specific exception.
     *
     * @param code identifier for the Result Code (case-insensitive)
     * @param date ISO date (yyyy-MM-dd) on which the Result Code must be "Active"
     * @return HTTP 200 response containing a {@link ResultCodeGetDetailDto}
     */
    @Override
    public ResponseEntity<ResultCodeGetDetailDto> getResultCodeByCodeAndDate(String code, LocalDate date) {
        var dto = resultCodeService.findByCodeAndDate(code, date);
        return ResponseEntity.ok(dto);
    }

    /**
     * Retrieve a paginated list of active Result Codes.
     *
     * <p>Filters:
     *
     * <ul>
     *   <li>{@code code} — optional, case-insensitive partial match on code.
     *   <li>{@code title} — optional, case-insensitive partial match on title.
     * </ul>
     *
     * <p>Pagination and sorting parameters follow the OpenAPI contract. Sorting is validated
     * against allowed properties using {@link ResultCodeSortValidator}.
     *
     * @param code optional filter for ResultCode code (partial, case-insensitive)
     * @param title optional filter for ResultCode title (partial, case-insensitive)
     * @param page zero-based page index
     * @param size page size
     * @param sort list of sort directives, e.g. {@code ["title,asc", "code,desc"]}
     * @return HTTP 200 response containing a {@link ResultCodePage} with metadata and content
     */
    @Override
    public ResponseEntity<ResultCodePage> getResultCodes(String code, String title, Integer page, Integer size, List<String> sort) {

        // Map OpenAPI paging params into a Spring Pageable with default sort by name ascending
        Pageable pageable =
            pageableMapper.from(page, size, sort, ResolutionCode_.RESULT_CODE, Sort.Direction.ASC);

        // Validate resolved sort properties to prevent invalid/unsafe sort fields
        pageable.getSort().forEach(o -> sortValidator.validate(o.getProperty()));

        // Fetch paginated results from service layer
        var resultCodePage = resultCodeService.getPage(code, title, pageable);

        return ResponseEntity.ok(resultCodePage);
    }
}
