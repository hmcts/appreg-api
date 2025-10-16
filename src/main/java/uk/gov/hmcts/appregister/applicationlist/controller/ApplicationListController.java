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

import uk.gov.hmcts.appregister.applicationentry.validator.ApplicationListEntrySortValidator;
import uk.gov.hmcts.appregister.applicationlist.service.ApplicationListService;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry_;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.generated.api.ApplicationListsApi;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;

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

    // Validator ensuring requested sort fields are valid for Court Locations.
    private final ApplicationListEntrySortValidator sortValidator;

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
     * Gets an Application List by id.
     *
     * <p>This endpoint returns both the list metadata and a paginated summary of its entries.
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
    public ResponseEntity<ApplicationListGetDetailDto> getApplicationList(
        UUID id, Integer page, Integer size, List<String> sort) {

        // Map OpenAPI paging params into a Spring Pageable with default sort by sequence number ascending
        Pageable pageable =
                pageableMapper.from(page, size, sort, ApplicationListEntry_.SEQUENCE_NUMBER, Sort.Direction.ASC);

        // Validate resolved sort properties to prevent invalid/unsafe sort fields
        pageable.getSort().forEach(o -> sortValidator.validate(o.getProperty()));

        ApplicationListGetDetailDto retrieved = service.get(id, pageable);

        return ResponseEntity.status(OK)
                .varyBy("Accept")
                .contentType(VND_JSON_V1)
                .body(retrieved);
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
