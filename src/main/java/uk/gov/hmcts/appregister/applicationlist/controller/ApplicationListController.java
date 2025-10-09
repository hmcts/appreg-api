package uk.gov.hmcts.appregister.applicationlist.controller;

import static org.springframework.http.HttpStatus.CREATED;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.gov.hmcts.appregister.applicationlist.service.ApplicationListService;
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
        return ResponseEntity.noContent().build();
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
