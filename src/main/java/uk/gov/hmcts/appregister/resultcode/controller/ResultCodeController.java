package uk.gov.hmcts.appregister.resultcode.controller;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.appregister.common.api.SortableField;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode_;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.mapper.SortMapper;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.generated.api.ResultCodesApi;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ResultCodePage;
import uk.gov.hmcts.appregister.resultcode.api.ResultCodeSortFieldEnum;
import uk.gov.hmcts.appregister.resultcode.service.ResultCodeService;

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
@Slf4j
@RestController
@RequiredArgsConstructor
@PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
public class ResultCodeController implements ResultCodesApi {

    // Service layer providing Result Code business logic.
    private final ResultCodeService resultCodeService;

    // Mapper converting OpenAPI paging params to Spring Data {@link Pageable}.
    private final PageableMapper pageableMapper;

    // Maps and validates API sort parameters to entity field names.
    private final SortMapper sortMapper;

    /**
     * Retrieve a single Result Code by its code and a date where the Result Code is "Active".
     *
     * <p>Delegates to {@link ResultCodeService#findByCode(String, LocalDate)}. If no active court
     * is found, the service layer throws a domain-specific exception.
     *
     * @param code identifier for the Result Code (case-insensitive)
     * @param date ISO date (yyyy-MM-dd) on which the Result Code must be "Active"
     * @return HTTP 200 response containing a {@link ResultCodeGetDetailDto}
     */
    @Override
    public ResponseEntity<ResultCodeGetDetailDto> getResultCodeByCodeAndDate(
            String code, LocalDate date) {
        var dto = resultCodeService.findByCode(code, date);
        log.info("getResultCodes: code: {}, date: {}", code, date);
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
     * against allowed properties using {@link ResultCodeSortFieldEnum}.
     *
     * @param code optional filter for ResultCode code (partial, case-insensitive)
     * @param title optional filter for ResultCode title (partial, case-insensitive)
     * @param page zero-based page index
     * @param size page size
     * @param sort list of sort directives, e.g. {@code ["title,asc", "code,desc"]}
     * @return HTTP 200 response containing a {@link ResultCodePage} with metadata and content
     */
    @Override
    public ResponseEntity<ResultCodePage> getResultCodes(
            String code, String title, Integer page, Integer size, List<String> sort) {

        final List<String> entitySortFields = toEntitySort(sort);

        Pageable pageable =
                pageableMapper.from(
                        page,
                        size,
                        entitySortFields,
                        ResolutionCode_.RESULT_CODE,
                        Sort.Direction.ASC);

        var resultCodePage = resultCodeService.findAll(code, title, pageable);

        log.info(
                "getResultCodes: code: {}, title: {}, page: {}, size: {}", code, title, page, size);
        return ResponseEntity.ok().body(resultCodePage);
    }

    private List<String> toEntitySort(List<String> sort) {
        if (sort == null || sort.isEmpty()) {
            return List.of();
        }
        return sortMapper.map(
                SortableField.of(sort.toArray(new String[0])),
                ResultCodeSortFieldEnum::getEntityValue);
    }
}
