package uk.gov.hmcts.appregister.applicationentryresult.controller;

import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.gov.hmcts.appregister.applicationentryresult.model.ListEntryResultDeleteArgs;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForCreateEntryResult;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForUpdateEntryResult;
import uk.gov.hmcts.appregister.applicationentryresult.service.ApplicationEntryResultService;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.generated.api.ApplicationListEntryResultsApi;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;
import uk.gov.hmcts.appregister.generated.model.ResultGetDto;
import uk.gov.hmcts.appregister.generated.model.ResultUpdateDto;

/**
 * REST controller for managing Application List Entry Results.
 */
@RestController
@Validated
@RequiredArgsConstructor
@Slf4j
public class ApplicationEntryResultController implements ApplicationListEntryResultsApi {

    private final ApplicationEntryResultService service;

    public static final MediaType VND_JSON_V1 =
            MediaType.parseMediaType("application/vnd.hmcts.appreg.v1+json");

    /**
     * Deletes an Application List Entry Result.
     *
     * <p>This endpoint deletes the provided result id and returns a 204 response
     *
     * <ul>
     *   <li>Accessible only to users with USER or ADMIN roles (see {@link RoleNames}).
     * </ul>
     *
     * @param listId Public identifier of the Application List. (required)
     * @param entryId Public identifier of the Application List Entry. (required)
     * @param resultId Public identifier of the Application List Entry Result. (required)
     * @return {@link ResponseEntity} The 204 response
     */
    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<Void> deleteApplicationListEntryResult(
            UUID listId, UUID entryId, UUID resultId) {
        ListEntryResultDeleteArgs args = new ListEntryResultDeleteArgs(listId, entryId, resultId);
        service.delete(args);
        log.info("Deleted Application List Entry Result with id: {}", resultId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Creates an Application List Entry Result.
     *
     * <p>This endpoint creates and stores a new Application List Entry Result linked to an existing
     * Application List Entry
     *
     * <ul>
     *   <li>Accessible only to users with USER or ADMIN roles (see {@link RoleNames}).
     * </ul>
     *
     * @param listId Public identifier of the Application List. (required)
     * @param entryId Public identifier of the Application List Entry. (required)
     * @param resultCreateDto (required)
     * @return Returns the created Application List Entry Result (status code 201)
     */
    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<ResultGetDto> createApplicationListEntryResult(
            UUID listId, UUID entryId, ResultCreateDto resultCreateDto) {
        // create the entry result
        MatchResponse<ResultGetDto> resultGetDto =
                service.create(
                        PayloadForCreateEntryResult.<ResultCreateDto>builder()
                                .listId(listId)
                                .entryId(entryId)
                                .data(resultCreateDto)
                                .build());

        return ResponseEntity.created(locationOf(resultGetDto.getPayload().getId()))
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(VND_JSON_V1)
                .eTag(resultGetDto.getEtag())
                .body(resultGetDto.getPayload());
    }

    /**
     * Updates an Application List Entry Result.
     *
     * <ul>
     *   <li>Accessible only to users with USER or ADMIN roles (see {@link RoleNames}).
     * </ul>
     *
     * @param listId Public identifier of the Application List. (required)
     * @param entryId Public identifier of the Application List Entry. (required)
     * @param resultId Public identifier of the Application List Entry Result. (required)
     * @param resultUpdateDto (required)
     * @return Returns the updated Application List Entry Result (status code 200)
     */
    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<ResultGetDto> updateApplicationListEntryResult(
            UUID listId, UUID entryId, UUID resultId, ResultUpdateDto resultUpdateDto) {
        PayloadForUpdateEntryResult payloadForUpdateEntryResult =
                new PayloadForUpdateEntryResult(resultUpdateDto, listId, entryId, resultId);

        // update the entry result
        MatchResponse<ResultGetDto> resultGetDto = service.update(payloadForUpdateEntryResult);
        log.info(
                "Successfully updated Application List Entry Result with id:{}",
                resultGetDto.getPayload().getId());

        return ResponseEntity.ok()
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(VND_JSON_V1)
                .headers(h -> h.setLocation(locationOf(resultGetDto.getPayload().getId())))
                .eTag(resultGetDto.getEtag())
                .body(resultGetDto.getPayload());
    }

    /**
     * Builds the resource location URI for a given Application List Entry Result UUID.
     *
     * @param resultId the unique UUID for the entry
     * @return a {@link URI} pointing to the resource location
     */
    private static URI locationOf(UUID resultId) {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{resultId}")
                .buildAndExpand(resultId)
                .toUri();
    }
}
