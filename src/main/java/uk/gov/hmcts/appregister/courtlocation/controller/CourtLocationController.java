package uk.gov.hmcts.appregister.courtlocation.controller;

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
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse_;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.mapper.SortMapper;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.courtlocation.api.CourtLocationSortFieldMapper;
import uk.gov.hmcts.appregister.courtlocation.service.CourtLocationService;
import uk.gov.hmcts.appregister.generated.api.CourtLocationsApi;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationPage;

/**
 * REST controller for managing Court Locations.
 *
 * <p>Implements the operations defined in the OpenAPI-generated {@link CourtLocationsApi}. Provides
 * endpoints to:
 *
 * <ul>
 *   <li>Retrieve a specific Court Location by code and effective date.
 *   <li>Retrieve a paginated list of active Court Locations of type CHOA with optional filters.
 * </ul>
 *
 * <p>All endpoints require the caller to have either user or admin role restrictions as enforced by
 * {@link RoleNames#USER_ROLE_OR_ADMIN_ROLE_RESTRICTION}.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CourtLocationController implements CourtLocationsApi {

    // Service layer providing Court Location business logic.
    private final CourtLocationService service;

    // Mapper converting OpenAPI paging params to Spring Data {@link Pageable}.
    private final PageableMapper pageableMapper;

    // Maps and validates API sort parameters to entity field names.
    private final SortMapper sortMapper;

    /**
     * Retrieve a single Court Location by its business code and an effective date.
     *
     * <p>Delegates to {@link CourtLocationService#findByCodeAndDate(String, LocalDate)}. If no
     * active court is found, the service layer throws a domain-specific exception.
     *
     * @param code identifier for the Court Location (case-insensitive)
     * @param date ISO date (yyyy-MM-dd) on which the Court Location must be valid
     * @return HTTP 200 response containing a {@link CourtLocationGetDetailDto}
     */
    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<CourtLocationGetDetailDto> getCourtLocationByCodeAndDate(
            String code, LocalDate date) {
        var courtLocationGetDetailDto = service.findByCodeAndDate(code, date);
        log.info("getCourtLocationByCodeAndDate: code: {}, date: {}", code, date);
        return ResponseEntity.ok().body(courtLocationGetDetailDto);
    }

    /**
     * Retrieve a paginated list of active CHOA Court Locations.
     *
     * <p>Filters:
     *
     * <ul>
     *   <li>{@code name} — optional, case-insensitive partial match on court name.
     *   <li>{@code code} — optional, case-insensitive partial match on court code.
     * </ul>
     *
     * <p>Pagination and sorting parameters follow the OpenAPI contract. Sorting is validated
     * against allowed properties using {@link SortMapper}.
     *
     * @param name optional filter for court name (partial, case-insensitive)
     * @param code optional filter for court location code (partial, case-insensitive)
     * @param page zero-based page index
     * @param size page size
     * @param sort list of sort directives, e.g. {@code ["name,asc", "code,desc"]}
     * @return HTTP 200 response containing a {@link CourtLocationPage} with metadata and content
     */
    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<CourtLocationPage> getCourtLocations(
            String name, String code, Integer page, Integer size, List<String> sort) {

        final List<String> entitySortFields = toEntitySort(sort);

        Pageable pageable =
                pageableMapper.from(
                        page, size, entitySortFields, NationalCourtHouse_.NAME, Sort.Direction.ASC);

        log.info(
                "getCourtLocations: code: {}, name: {}, page: {}, size: {}",
                code,
                name,
                page,
                size);
        return ResponseEntity.ok().body(service.getPage(name, code, pageable));
    }

    private List<String> toEntitySort(List<String> sort) {
        if (sort == null || sort.isEmpty()) {
            return List.of();
        }
        return sortMapper.map(
                SortableField.of(sort.toArray(new String[0])),
                CourtLocationSortFieldMapper::getEntityValue);
    }
}
