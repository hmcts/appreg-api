package uk.gov.hmcts.appregister.applicationentry.controller;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.gov.hmcts.appregister.applicationentry.api.ApplicationEntrySortFieldEnum;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForUpdateEntry;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadGetEntryInList;
import uk.gov.hmcts.appregister.applicationentry.service.ApplicationEntryService;
import uk.gov.hmcts.appregister.common.api.SortableField;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.mapper.SortMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.generated.api.ApplicationListEntriesApi;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.EntryUpdateDto;

@PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
@Controller
@RequiredArgsConstructor
@Slf4j
public class ApplicationEntryController implements ApplicationListEntriesApi {
    private final ApplicationEntryService applicationEntryService;

    private final SortMapper sortMapper;

    private final PageableMapper pageableMapper;

    public static final MediaType VND_JSON_V1 =
            MediaType.parseMediaType("application/vnd.hmcts.appreg.v1+json");

    @Override
    public ResponseEntity<EntryPage> getEntries(
            EntryGetFilterDto filter, Integer page, Integer size, List<String> sort) {
        List<String> entitySortFields = toEntitySort(sort);

        // if we do not have a sort then default to multiple code sort entity fields
        if (entitySortFields.isEmpty()) {
            entitySortFields = Arrays.asList(ApplicationEntrySortFieldEnum.CODE.getEntityValue());
        }

        Pageable pageInfo =
                pageableMapper.from(
                        page,
                        size,
                        entitySortFields,
                        ApplicationEntrySortFieldEnum.CODE.getEntityValue()[0],
                        Sort.Direction.ASC);

        log.info("Retrieved Application Entry Lists");
        return ResponseEntity.ok()
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(VND_JSON_V1)
                .body(applicationEntryService.search(filter, pageInfo));
    }

    @Override
    public ResponseEntity<EntryGetDetailDto> createApplicationListEntry(
            UUID listId, EntryCreateDto entryCreateDto) {
        // create the entry
        MatchResponse<EntryGetDetailDto> entryGetDetailDto =
                applicationEntryService.createEntry(
                        PayloadForCreate.<EntryCreateDto>builder()
                                .id(listId)
                                .data(entryCreateDto)
                                .build());
        log.info("Create Application List Entry");
        return ResponseEntity.created(locationOf(entryGetDetailDto.getPayload().getId()))
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(VND_JSON_V1)
                .eTag(entryGetDetailDto.getEtag())
                .body(entryGetDetailDto.getPayload());
    }

    @Override
    public ResponseEntity<EntryGetDetailDto> updateApplicationListEntry(
            UUID listId, UUID entryId, EntryUpdateDto entryUpdateDto) {
        PayloadForUpdateEntry payloadForUpdateEntry =
                new PayloadForUpdateEntry(entryUpdateDto, listId, entryId);

        // create the entry
        MatchResponse<EntryGetDetailDto> entryGetDetailDto =
                applicationEntryService.updateEntry(payloadForUpdateEntry);
        log.info("Update Application List Entry");
        return ResponseEntity.ok()
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(VND_JSON_V1)
                .headers(h -> h.setLocation(locationOf(entryGetDetailDto.getPayload().getId())))
                .eTag(entryGetDetailDto.getEtag())
                .body(entryGetDetailDto.getPayload());
    }

    @Override
    public ResponseEntity<EntryGetDetailDto> getApplicationListEntry(UUID listId, UUID entryId) {
        PayloadGetEntryInList payloadForGet =
                PayloadGetEntryInList.builder().listId(listId).entryId(entryId).build();

        MatchResponse<EntryGetDetailDto> matchResponse =
                applicationEntryService.getApplicationListEntryDetail(payloadForGet);
        log.info("Get Application List Entry");
        return ResponseEntity.ok()
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(VND_JSON_V1)
                .eTag(matchResponse.getEtag())
                .body(matchResponse.getPayload());
    }

    private List<String> toEntitySort(List<String> sort) {
        if (sort == null || sort.isEmpty()) {
            return List.of();
        }
        return sortMapper.map(
                SortableField.of(sort.toArray(new String[0])),
                ApplicationEntrySortFieldEnum::getEntityValue);
    }

    /**
     * Builds the resource location URI for a given Application List ID.
     *
     * @param entry the unique is for the entry
     * @return a {@link URI} pointing to the resource location
     */
    private static URI locationOf(UUID entry) {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{entryId}")
                .buildAndExpand(entry)
                .toUri();
    }
}
