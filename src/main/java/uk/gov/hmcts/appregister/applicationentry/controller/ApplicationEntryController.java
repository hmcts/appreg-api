package uk.gov.hmcts.appregister.applicationentry.controller;

import static uk.gov.hmcts.appregister.common.api.ApiConstants.MediaTypes.VND_JSON_V1;

import jakarta.validation.Validator;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.gov.hmcts.appregister.applicationentry.api.ApplicationEntrySortConfig;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapper;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUploadRow;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForDeleteEntry;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForUpdateClosedEntry;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForUpdateEntry;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadGetEntryInList;
import uk.gov.hmcts.appregister.applicationentry.service.ApplicationEntryService;
import uk.gov.hmcts.appregister.applicationentry.service.BulkImportService;
import uk.gov.hmcts.appregister.applicationentry.service.BulkUploadAsyncLifecycle;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkCreateApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkUploadApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkUploadCsvFormatValidator;
import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;
import uk.gov.hmcts.appregister.common.async.model.TrackJobStatusResponse;
import uk.gov.hmcts.appregister.common.async.reader.CsvReader;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobService;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.api.ApplicationListEntriesApi;
import uk.gov.hmcts.appregister.generated.model.BulkActionPreviewRequestDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionPreviewResponseDto;
import uk.gov.hmcts.appregister.generated.model.BulkFeesUpdateDto;
import uk.gov.hmcts.appregister.generated.model.BulkOfficialsUpdateDto;
import uk.gov.hmcts.appregister.generated.model.BulkUpdateResponseDto;
import uk.gov.hmcts.appregister.generated.model.EntryApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryIdsDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.EntryUpdateClosedDto;
import uk.gov.hmcts.appregister.generated.model.EntryUpdateDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;

@PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
@Controller
@RequiredArgsConstructor
@Slf4j
public class ApplicationEntryController implements ApplicationListEntriesApi {
    private final ApplicationEntryService applicationEntryService;

    private final BulkImportService bulkImportService;

    private final PageableMapper pageableMapper;

    private final AsyncJobService asyncJobService;

    private final UserProvider userProvider;

    private final BulkUploadApplicationEntryValidator bulkUploadApplicationEntryValidator;

    private final BulkCreateApplicationEntryValidator bulkCreateApplicationEntryValidator;

    private final BulkUploadCsvFormatValidator bulkUploadCsvFormatValidator;

    private final ApplicationListEntryMapper applicationListEntryMapper;

    private final Validator beanValidator;

    @Value("${appreg.bulk-import.page-size:25}")
    private int bulkImportPageSize = 25;

    @Override
    public ResponseEntity<EntryPage> getEntries(
            EntryGetFilterDto filter, Integer page, Integer size, List<String> sort) {
        PagingWrapper pageInfo =
                pageableMapper.from(
                        page, size, sort, ApplicationEntrySortConfig.SEARCH, Sort.Direction.ASC);

        EntryPage entryPage = applicationEntryService.search(filter, pageInfo);

        return ResponseEntity.ok()
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(VND_JSON_V1)
                .body(entryPage);
    }

    @Override
    public ResponseEntity<EntryIdsDto> getEntryIds(EntryGetFilterDto filter) {
        EntryIdsDto entryIds = applicationEntryService.getEntryIds(filter);

        return ResponseEntity.ok()
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(VND_JSON_V1)
                .body(entryIds);
    }

    @Override
    public ResponseEntity<BulkActionPreviewResponseDto> bulkActionPreview(
            BulkActionPreviewRequestDto bulkActionPreviewRequestDto) {
        BulkActionPreviewResponseDto response =
                applicationEntryService.bulkActionPreview(bulkActionPreviewRequestDto);

        return ResponseEntity.ok()
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(VND_JSON_V1)
                .body(response);
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

        // update the entry
        MatchResponse<EntryGetDetailDto> entryGetDetailDto =
                applicationEntryService.updateEntry(payloadForUpdateEntry);
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
        return ResponseEntity.ok()
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(VND_JSON_V1)
                .eTag(matchResponse.getEtag())
                .body(matchResponse.getPayload());
    }

    @Override
    public ResponseEntity<EntryGetDetailDto> getApplicationListEntryFromClosedList(
            UUID listId, UUID entryId) {
        PayloadGetEntryInList payloadForGet =
                PayloadGetEntryInList.builder().listId(listId).entryId(entryId).build();

        MatchResponse<EntryGetDetailDto> matchResponse =
                applicationEntryService.getApplicationListEntryDetailFromClosedList(payloadForGet);
        return ResponseEntity.ok()
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(VND_JSON_V1)
                .eTag(matchResponse.getEtag())
                .body(matchResponse.getPayload());
    }

    @Override
    public ResponseEntity<EntryPage> getApplicationListEntries(
            UUID listId,
            EntryApplicationListGetFilterDto filter,
            Integer pageNumber,
            Integer pageSize,
            List<String> sort) {
        PayloadGetEntryInList payloadForGet =
                PayloadGetEntryInList.builder().listId(listId).build();

        PagingWrapper pageInfo =
                pageableMapper.from(
                        pageNumber,
                        pageSize,
                        sort,
                        ApplicationEntrySortConfig.BY_LIST_ID,
                        Sort.Direction.ASC);

        EntryPage entryResponse =
                applicationEntryService.getApplicationListEntries(payloadForGet, pageInfo, filter);

        log.info("Get Application List Entries for listId: {}", listId);

        return ResponseEntity.ok()
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(VND_JSON_V1)
                .body(entryResponse);
    }

    @Override
    public ResponseEntity<EntryIdsDto> getApplicationListEntryIds(
            UUID listId, EntryApplicationListGetFilterDto filter) {
        PayloadGetEntryInList payloadForGet =
                PayloadGetEntryInList.builder().listId(listId).build();

        EntryIdsDto entryIds =
                applicationEntryService.getApplicationListEntryIds(payloadForGet, filter);

        log.info("Get Application List Entry IDs for listId: {}", listId);

        return ResponseEntity.ok()
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(VND_JSON_V1)
                .body(entryIds);
    }

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<Void> moveApplicationListEntries(
            UUID listId, MoveEntriesDto moveEntriesDto) {
        applicationEntryService.move(listId, moveEntriesDto);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<JobAcknowledgement> bulkUploadApplicationListEntries(
            UUID listId, MultipartFile file) {

        log.debug("Accepting bulk upload for application list {}", listId);

        if (file == null || file.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.BULK_UPLOAD_FILE_MISSING,
                    "Bulk upload file must be provided and not empty");
        }

        try {
            bulkUploadCsvFormatValidator.validate(file);
            var applicationList =
                    bulkCreateApplicationEntryValidator.validateApplicationList(listId);

            JobTypeRequest jobTypeRequest =
                    JobTypeRequest.builder()
                            .userName(userProvider.getUserId())
                            .jobType(JobType.BULK_UPLOAD_ENTRIES)
                            .build();

            CsvReader<BulkUploadRow> csvReader = new CsvReader<>(file, BulkUploadRow.class);

            BulkUploadAsyncLifecycle lifecycle =
                    new BulkUploadAsyncLifecycle(
                            listId,
                            applicationList,
                            bulkImportService,
                            bulkUploadApplicationEntryValidator,
                            bulkCreateApplicationEntryValidator,
                            applicationListEntryMapper,
                            beanValidator);

            lifecycle.setCSVFile(file);

            TrackJobStatusResponse trackJobStatusResponse =
                    asyncJobService.startValidationFirstJob(
                            jobTypeRequest, csvReader, lifecycle, bulkImportPageSize);

            JobAcknowledgement ack =
                    new JobAcknowledgement()
                            .id(trackJobStatusResponse.getUuid())
                            .type(trackJobStatusResponse.getType())
                            .status(trackJobStatusResponse.getStatus());

            return ResponseEntity.accepted()
                    .varyBy(HttpHeaders.ACCEPT)
                    .contentType(VND_JSON_V1)
                    .header(HttpHeaders.LOCATION, "/jobs/" + trackJobStatusResponse.getUuid())
                    .body(ack);
        } catch (IOException e) {
            log.error("Failed to initialise CSV reader for bulk upload", e);

            throw new AppRegistryException(
                    AppListEntryError.BULK_UPLOAD_INVALID_FILE_FORMAT,
                    "Unable to read uploaded file");
        }
    }

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<Void> updateClosedApplicationListEntry(
            UUID listId, UUID entryId, EntryUpdateClosedDto entryUpdateClosedDto) {
        PayloadForUpdateClosedEntry entryUpdateClosedDtoWithIds =
                new PayloadForUpdateClosedEntry(entryUpdateClosedDto, listId, entryId);

        MatchResponse<Void> matchResponse =
                applicationEntryService.updateClosedEntry(entryUpdateClosedDtoWithIds);
        return ResponseEntity.noContent()
                .varyBy(HttpHeaders.ACCEPT)
                .eTag(matchResponse.getEtag())
                .build();
    }

    @Override
    public ResponseEntity<Void> deleteApplicationListEntry(UUID listId, UUID entryId) {
        PayloadForDeleteEntry payload = new PayloadForDeleteEntry(listId, entryId);
        applicationEntryService.deleteEntry(payload);

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> replaceApplicationListEntryOfficials(
            UUID listId, BulkOfficialsUpdateDto bulkOfficialsUpdateDto) {
        applicationEntryService.replaceOfficials(listId, bulkOfficialsUpdateDto);

        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<BulkUpdateResponseDto> bulkUpdateApplicationListEntryFees(
            UUID listId, BulkFeesUpdateDto bulkFeesUpdateDto) {
        BulkUpdateResponseDto response =
                applicationEntryService.bulkUpdateFees(listId, bulkFeesUpdateDto);

        return ResponseEntity.ok()
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(VND_JSON_V1)
                .body(response);
    }

    @Override
    public ResponseEntity<List<UUID>> getBulkResultApplicationListEntriesByJobId(UUID jobId) {
        return ResponseEntity.ok()
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(VND_JSON_V1)
                .body(applicationEntryService.getApplicationListEntriesByJobId(jobId));
    }

    /**
     * Builds the resource location URI for a given Application List Entry ID.
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
