package uk.gov.hmcts.appregister.applicationentryresult.controller;

import static uk.gov.hmcts.appregister.common.api.ApiConstants.MediaTypes.VND_JSON_V1;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.gov.hmcts.appregister.applicationentryresult.api.ApplicationEntryResultSortFieldEnum;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForCreateEntryResult;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForCreateResults;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForUpdateEntryResult;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadGetEntryResultInList;
import uk.gov.hmcts.appregister.applicationentryresult.service.ApplicationEntryResultService;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.api.ApplicationListEntryResultsApi;
import uk.gov.hmcts.appregister.generated.model.BulkDeleteResultsDto;
import uk.gov.hmcts.appregister.generated.model.BulkResultDto;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;
import uk.gov.hmcts.appregister.generated.model.ResultGetDto;
import uk.gov.hmcts.appregister.generated.model.ResultPage;
import uk.gov.hmcts.appregister.generated.model.ResultUpdateDto;

/**
 * REST controller for managing Application List Entry Results.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ApplicationEntryResultController implements ApplicationListEntryResultsApi {

    private final ApplicationEntryResultService service;

    // Mapper converting OpenAPI paging params to Spring Data {@link Pageable}.
    private final PageableMapper pageableMapper;

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<List<ResultGetDto>> bulkResultApplicationListEntries(
            UUID listId, BulkResultDto bulkResultDto) {
        // call the service to process the bulk result for the list of entries
        List<ResultGetDto> createdResults =
                service.bulkCreate(
                        PayloadForCreateResults.<BulkResultDto>builder()
                                .payload(bulkResultDto)
                                .listId(listId)
                                .build());

        return ResponseEntity.ok()
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(VND_JSON_V1)
                .body(createdResults);
    }

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<List<ResultGetDto>> bulkResultEntries(BulkResultDto bulkResultDto) {
        // call the service to process the bulk result for the list of entries
        List<ResultGetDto> createdResults =
                service.bulkCreate(
                        PayloadForCreateResults.<BulkResultDto>builder()
                                .payload(bulkResultDto)
                                .build());

        return ResponseEntity.ok()
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(VND_JSON_V1)
                .body(createdResults);
    }

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<Void> bulkDeleteResultEntries(BulkDeleteResultsDto bulkDeleteResultsDto) {
        service.bulkDelete(bulkDeleteResultsDto);
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

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<ResultPage> getApplicationListEntryResults(
            UUID listId, UUID entryId, Integer pageNumber, Integer pageSize) {
        PagingWrapper pagingWrapper =
                pageableMapper.from(
                        pageNumber,
                        pageSize,
                        List.of(),
                        ApplicationEntryResultSortFieldEnum.CODE,
                        Sort.Direction.ASC,
                        ApplicationEntryResultSortFieldEnum::getEntityValue);
        ResultPage resultPage =
                service.search(
                        PayloadGetEntryResultInList.builder()
                                .listId(listId)
                                .entryId(entryId)
                                .build(),
                        pagingWrapper);
        return ResponseEntity.ok().body(resultPage);
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
