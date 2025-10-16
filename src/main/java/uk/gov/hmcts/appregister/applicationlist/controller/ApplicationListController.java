package uk.gov.hmcts.appregister.applicationlist.controller;

import static org.springframework.http.HttpStatus.CREATED;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.gov.hmcts.appregister.applicationlist.mapper.ApplicationListSortMapper;
import uk.gov.hmcts.appregister.applicationlist.service.ApplicationListService;
import uk.gov.hmcts.appregister.common.entity.ApplicationList_;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.generated.api.ApplicationListsApi;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;

/**
 * REST controller for managing Application Lists.
 *
 * <p>This controller provides endpoints for creating and retrieving application lists. It leverages
 * {@link ApplicationListService} for business logic and ensures request validation and
 * authorization via Spring Security annotations.
 *
 * <p>Responses are served in versioned JSON media type: {@code
 * application/vnd.hmcts.appreg.v1+json}. Annotations:
 *
 * <ul>
 *   <li>{@code @RestController} - Marks this as a REST controller.
 *   <li>{@code @Validated} - Enables validation on method parameters.
 *   <li>{@code @RequiredArgsConstructor} - Generates a constructor for final fields.
 *   <li>{@code @Slf4j} - Provides logging support.
 * </ul>
 */
@RestController
@Validated
@RequiredArgsConstructor
@Slf4j
public class ApplicationListController implements ApplicationListsApi {

    private static final MediaType VND_JSON_V1 =
            MediaType.parseMediaType("application/vnd.hmcts.appreg.v1+json");

    private final ApplicationListService service;

    // Mapper converting OpenAPI paging params to Spring Data {@link Pageable}.
    private final PageableMapper pageableMapper;

    // Mapper converting API sort params to internal db fields.
    private final ApplicationListSortMapper sortMapper;

    /**
     * Creates a new Application List.
     *
     * <p>This endpoint persists the provided {@link ApplicationListCreateDto} and returns the
     * created entity as {@link ApplicationListGetDetailDto}. The response includes a {@code
     * Location} header pointing to the newly created resource URI. Security:
     *
     * <ul>
     *   <li>Accessible only to users with USER or ADMIN roles (see {@link RoleNames}).
     * </ul>
     *
     * @param applicationListCreateDto the request payload containing application list details
     * @return {@link ResponseEntity} containing the created application list details
     */
    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<ApplicationListGetDetailDto> createApplicationList(
            @Valid @RequestBody ApplicationListCreateDto applicationListCreateDto) {

        ApplicationListGetDetailDto created = service.create(applicationListCreateDto);

        return ResponseEntity.status(CREATED)
                .varyBy("Accept")
                .contentType(VND_JSON_V1)
                .headers(h -> h.setLocation(locationOf(created.getId())))
                .body(created);
    }

    /**
     * Deletes a new Application List.
     *
     * <p>This endpoint deletes the provided id and returns a 204 response
     *
     * <ul>
     *   <li>Accessible only to users with USER or ADMIN roles (see {@link RoleNames}).
     * </ul>
     *
     * @param id The application list id to delete
     * @return {@link ResponseEntity} The 204 response
     */
    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<Void> deleteApplicationList(UUID id) {
        service.delete(id);
        log.info("Deleted Application List with id: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a paginated and optionally sorted list of application lists.
     *
     * <p>This endpoint allows clients to fetch existing {@link ApplicationListPage} resources using
     * pagination, filtering, and sorting options. It converts OpenAPI paging parameters into Spring
     * Data's {@link Pageable} and applies default sorting by {@code description} in ascending order
     * when no explicit sort is provided.
     *
     * <p>Security:
     *
     * <ul>
     *   <li>Accessible only to users with USER or ADMIN roles (see {@link RoleNames}).
     * </ul>
     *
     * @param filter an {@link ApplicationListGetFilterDto} containing optional filter criteria such
     *     as name, status, or other attributes
     * @param page the page number to retrieve (zero-based)
     * @param size the number of records per page
     * @param sort a list of sort parameters (e.g., {@code ["description,asc",
     *     "createdDate,desc"]}); validated and mapped by {@link ApplicationListSortMapper}
     * @return {@link ResponseEntity} containing the requested page of application lists wrapped in
     *     an {@link ApplicationListPage} object
     */
    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<ApplicationListPage> getApplicationLists(
            ApplicationListGetFilterDto filter, Integer page, Integer size, List<String> sort) {

        List<String> mappedSort = sortMapper.mapAndValidate(sort);
        Pageable pageInfo =
                pageableMapper.from(
                        page, size, mappedSort, ApplicationList_.DESCRIPTION, Sort.Direction.ASC);

        var applicationListPage = service.getPage(filter, pageInfo);
        return ResponseEntity.ok(applicationListPage);
    }

    /**
     * Builds the resource location URI for a given Application List ID.
     *
     * @param id the unique identifier of the Application List
     * @return a {@link URI} pointing to the resource location
     */
    private static URI locationOf(UUID id) {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }
}
