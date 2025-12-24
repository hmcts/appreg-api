package uk.gov.hmcts.appregister.applicationlist.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

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
import uk.gov.hmcts.appregister.applicationlist.api.ApplicationListEntriesSummarySortFieldEnum;
import uk.gov.hmcts.appregister.applicationlist.api.ApplicationListSortFieldEnum;
import uk.gov.hmcts.appregister.applicationlist.service.ApplicationListService;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForUpdate;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.api.ApplicationListsApi;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;

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

        MatchResponse<ApplicationListGetDetailDto> created =
                service.create(applicationListCreateDto);

        ResponseEntity<ApplicationListGetDetailDto> response =
                ResponseEntity.status(CREATED)
                        .varyBy("Accept")
                        .contentType(VND_JSON_V1)
                        .headers(h -> h.setLocation(locationOf(created.getPayload().getId())))
                        .eTag(created.getEtag())
                        .body(created.getPayload());

        log.info(
                "Create successful for Application List with id: {}", created.getPayload().getId());
        return response;
    }

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<ApplicationListGetDetailDto> updateApplicationList(
            UUID id, ApplicationListUpdateDto applicationListUpdateDto) {
        MatchResponse<ApplicationListGetDetailDto> updated =
                service.update(
                        PayloadForUpdate.<ApplicationListUpdateDto>builder()
                                .id(id)
                                .data(applicationListUpdateDto)
                                .build());

        ResponseEntity<ApplicationListGetDetailDto> response =
                ResponseEntity.status(OK)
                        .varyBy("Accept")
                        .contentType(VND_JSON_V1)
                        .eTag(updated.getEtag())
                        .body(updated.getPayload());

        log.info(
                "Update successful for Application List with id: {}", updated.getPayload().getId());
        return response;
    }

    /**
     * Gets an Application List by id.
     *
     * <p>This endpoint returns both the list metadata and a paginated summary of its entries.
     *
     * <ul>
     *   <li>Accessible only to users with USER or ADMIN roles (see {@link RoleNames}).
     * </ul>
     *
     * @param id the unique identifier of the application list
     * @param page the page number to retrieve (zero-based)
     * @param size the number of records per page
     * @param sort a list of sort parameters (e.g., {@code ["sequenceNumber,asc"]}); validated and
     *     mapped by {@link PageableMapper}
     * @return {@link ResponseEntity} containing the application list details
     */
    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<ApplicationListGetDetailDto> getApplicationList(
            UUID id, Integer page, Integer size, List<String> sort) {

        // Map OpenAPI paging params into a Spring Pageable with default sort by sequence number
        // ascending
        PagingWrapper pageable =
                pageableMapper.from(
                        page,
                        size,
                        sort,
                        ApplicationListEntriesSummarySortFieldEnum.SEQUENCE_NUMBER,
                        Sort.Direction.ASC,
                        ApplicationListEntriesSummarySortFieldEnum::getEntityValue);

        ApplicationListGetDetailDto retrieved = service.get(id, pageable);

        return ResponseEntity.status(OK).varyBy("Accept").contentType(VND_JSON_V1).body(retrieved);
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

        PagingWrapper pageInfo =
                pageableMapper.from(
                        page,
                        size,
                        sort,
                        ApplicationListSortFieldEnum.DESCRIPTION,
                        Sort.Direction.ASC,
                        ApplicationListSortFieldEnum::getEntityValue);

        var applicationListPage = service.getPage(filter, pageInfo);
        log.info("Retrieved Application Lists");
        return ResponseEntity.ok(applicationListPage);
    }

    /**
     * Gets an Application List by id with all its Application List Entries.
     *
     * <p>This endpoint returns both the list metadata and its entries.
     *
     * <ul>
     *   <li>Accessible only to users with USER or ADMIN roles (see {@link RoleNames}).
     * </ul>
     *
     * @param id the unique identifier of the application list
     * @return {@link ResponseEntity} containing the application list details
     */
    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<ApplicationListGetPrintDto> printApplicationList(UUID id) {

        ApplicationListGetPrintDto retrieved = service.print(id);

        log.info("Successfully retrieved ApplicationList with id: {}", id);

        return ResponseEntity.status(OK).varyBy("Accept").contentType(VND_JSON_V1).body(retrieved);
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
