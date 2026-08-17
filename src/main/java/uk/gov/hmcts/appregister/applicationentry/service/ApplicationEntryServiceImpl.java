package uk.gov.hmcts.appregister.applicationentry.service;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.applicationentry.api.ApplicationEntrySortConfig;
import uk.gov.hmcts.appregister.applicationentry.audit.AppListEntryAuditOperation;
import uk.gov.hmcts.appregister.applicationentry.audit.ApplicationListEntryReadAudit;
import uk.gov.hmcts.appregister.applicationentry.audit.BulkApplicationListEntriesReadAudit;
import uk.gov.hmcts.appregister.applicationentry.audit.BulkMoveApplicationListEntriesAudit;
import uk.gov.hmcts.appregister.applicationentry.audit.BulkUpdateFeesAudit;
import uk.gov.hmcts.appregister.applicationentry.audit.BulkUpdateOfficialsAudit;
import uk.gov.hmcts.appregister.applicationentry.audit.model.DeleteAuditable;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryEntityMapper;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapper;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUpdateFeesPayload;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUpdateOfficialsPayload;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForDeleteEntry;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForUpdateClosedEntry;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForUpdateEntry;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadGetEntryInList;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkActionPreviewValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkGetApplicationListEntriesValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkUpdateFeesValidationSuccess;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkUpdateFeesValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkUpdateOfficialsValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.CreateApplicationEntryValidationSuccess;
import uk.gov.hmcts.appregister.applicationentry.validator.CreateApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.DeleteApplicationListEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.GetApplicationEntryFromClosedListValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.GetApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.GetApplicationListEntriesValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.GetEntryValidationSuccess;
import uk.gov.hmcts.appregister.applicationentry.validator.UpdateApplicationEntryValidationSuccess;
import uk.gov.hmcts.appregister.applicationentry.validator.UpdateApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.UpdateClosedApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.applicationlist.model.MoveEntriesPayload;
import uk.gov.hmcts.appregister.applicationlist.validator.MoveEntriesValidator;
import uk.gov.hmcts.appregister.audit.annotation.NestedAudit;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.async.exception.JobError;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.concurrency.MatchService;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.AppListEntrySequenceMapping;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.AsyncJobsAppListEntry;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeStatusRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryOfficialRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntrySequenceMappingRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AsyncJobAppListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NameAddressRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapper;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryGetSummaryProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryResolutionProjection;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.common.util.BeanUtil;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntryBulkActionPreviewRequestDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntryBulkActionSelectionDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.BulkActionPreviewRequestDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionPreviewResponseDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionSelectionDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionSelectionType;
import uk.gov.hmcts.appregister.generated.model.BulkActionType;
import uk.gov.hmcts.appregister.generated.model.BulkFeeDetailsDto;
import uk.gov.hmcts.appregister.generated.model.BulkFeesUpdateDto;
import uk.gov.hmcts.appregister.generated.model.BulkGetApplicationListEntriesRequestDto;
import uk.gov.hmcts.appregister.generated.model.BulkOfficialsUpdateDto;
import uk.gov.hmcts.appregister.generated.model.BulkUpdateResponseDto;
import uk.gov.hmcts.appregister.generated.model.EntryApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetSummaryDto;
import uk.gov.hmcts.appregister.job.validator.JobExistanceValidator;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationEntryServiceImpl implements ApplicationEntryService {
    private static final String CREATED_OFFSITE_FEE_LOG = "Created Offsite Fee: {} to Entry: {}";

    private static final String BULK_FEE_UPDATE_REQUESTS_METRIC =
            "appregister.application_entry.bulk_fee_update.requests";
    private static final String BULK_FEE_UPDATE_ENTRIES_METRIC =
            "appregister.application_entry.bulk_fee_update.entries";
    private static final String BULK_FEE_UPDATE_DURATION_METRIC =
            "appregister.application_entry.bulk_fee_update.duration";
    private static final String METRIC_STATUS_TAG = "status";
    private static final String METRIC_SUCCEEDED = "succeeded";
    private static final String METRIC_FAILED = "failed";
    private static final int NOTES_MAX_LENGTH = 4000;
    private static final UUID BULK_ACTION_PREVIEW_PLACEHOLDER_ENTRY_ID = new UUID(0L, 0L);

    @Value("${appreg.bulk-action-preview.global-limit:2000}")
    private int bulkActionPreviewGlobalLimit;

    @Value("${appreg.bulk-action-preview.single-list-limit:1050}")
    private int bulkActionPreviewSingleListLimit;

    private final ApplicationListEntryRepository applicationListEntryRepository;

    private final FeeRepository feeRepository;

    private final PageMapper pageMapper;

    private final PageableMapper pageableMapper;

    private final CreateApplicationEntryValidator createApplicationEntryValidator;

    private final BulkActionPreviewValidator bulkActionPreviewValidator;

    private final BulkGetApplicationListEntriesValidator bulkGetApplicationListEntriesValidator;

    private final UpdateApplicationEntryValidator updateApplicationEntryValidator;

    private final UpdateClosedApplicationEntryValidator updateClosedApplicationEntryValidator;

    private final MoveEntriesValidator moveEntriesValidator;

    private final BulkUpdateOfficialsValidator bulkUpdateOfficialsValidator;

    private final BulkUpdateFeesValidator bulkUpdateFeesValidator;

    // Services
    private final MatchService matchService;

    // Audit
    private final AuditOperationService auditService;

    private final AppListEntryFeeStatusRepository appListEntryFeeStatusRepository;
    private final NameAddressRepository nameAddressRepository;
    private final AppListEntryOfficialRepository appListEntryOfficialRepository;
    private final AppListEntryFeeRepository appListEntryFeeRepository;
    private final AppListEntrySequenceMappingRepository appListEntrySequenceMappingRepository;
    private final AsyncJobAppListEntryRepository asyncJobAppListEntryRepository;

    private final ApplicationListEntryMapper applicationListEntryMapStructMapper;
    private final ApplicantMapper applicantMapper;

    private final ApplicationListEntryEntityMapper applicationListEntryEntityMapper;

    // Infrastructure
    private final EntityManager entityManager;

    private final GetApplicationEntryValidator getEntryValidator;

    private final GetApplicationEntryFromClosedListValidator getEntryFromClosedListValidator;

    private final GetApplicationListEntriesValidator getApplicationListEntriesValidator;

    private final Clock clock;
    private final BusinessDateProvider businessDateProvider;

    private final DeleteApplicationListEntryValidator deleteApplicationListEntryValidator;
    private final MeterRegistry meterRegistry;

    private final JobExistanceValidator jobExistanceValidator;

    @Override
    public EntryPage search(EntryGetFilterDto filterDto, PagingWrapper pageable) {
        log.debug(
                "Started find application entries page={} size={}",
                pageable.getPageable().getPageNumber(),
                pageable.getPageable().getPageSize());

        return auditService.processAudit(
                null,
                AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST,
                req -> {
                    Status status =
                            applicationListEntryMapStructMapper.toStatus(filterDto.getStatus());

                    Page<ApplicationListEntryGetSummaryProjection> resultPage =
                            applicationListEntryRepository.searchForGetSummary(
                                    null,
                                    filterDto.getDate() != null,
                                    filterDto.getDate(),
                                    filterDto.getCourtCode(),
                                    filterDto.getOtherLocationDescription(),
                                    filterDto.getCjaCode(),
                                    filterDto.getApplicantOrganisation(),
                                    filterDto.getApplicantSurname(),
                                    filterDto.getApplicantName(),
                                    filterDto.getStandardApplicantCode(),
                                    status,
                                    filterDto.getRespondentOrganisation(),
                                    filterDto.getRespondentSurname(),
                                    filterDto.getRespondentName(),
                                    filterDto.getRespondentPostcode(),
                                    filterDto.getAccountReference(),
                                    filterDto.getApplicationTitle(),
                                    null,
                                    null,
                                    null,
                                    pageable.getPageable());

                    // breaks name into individual and/or organisation parts
                    EntryPage newPage = buildEntryPage(resultPage, pageable);

                    log.debug(
                            "Finished find application entries page={} size={} results={}",
                            pageable.getPageable().getPageNumber(),
                            pageable.getPageable().getPageSize(),
                            newPage.getElementsOnPage());

                    AuditableResult<EntryPage, ApplicationListEntry> result =
                            new AuditableResult<>(
                                    newPage,
                                    applicationListEntryMapStructMapper.toApplicationListEntry(
                                            filterDto));

                    return Optional.of(result);
                });
    }

    @Override
    public List<EntryGetSummaryDto> bulkGetApplicationListEntries(
            BulkGetApplicationListEntriesRequestDto request) {
        bulkGetApplicationListEntriesValidator.validate(request);
        var entryIds = request.getEntryIds();
        var hasEntryIds = entryIds != null && !entryIds.isEmpty();

        return auditService.processAudit(
                null,
                AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST,
                req -> {
                    List<ApplicationListEntryGetSummaryProjection> summaries =
                            applicationListEntryRepository.findSummariesForApplicationListIds(
                                    request.getListIds(),
                                    hasEntryIds,
                                    hasEntryIds
                                            ? entryIds
                                            : List.of(BULK_ACTION_PREVIEW_PLACEHOLDER_ENTRY_ID));

                    List<EntryGetSummaryDto> orderedEntries =
                            orderBulkEntrySummaries(
                                    buildEntrySummaries(summaries), request.getListIds(), entryIds);

                    return Optional.of(
                            new AuditableResult<>(
                                    orderedEntries,
                                    new BulkApplicationListEntriesReadAudit(
                                            request.getListIds(), request.getEntryIds())));
                });
    }

    @Override
    public BulkActionPreviewResponseDto bulkActionPreview(BulkActionPreviewRequestDto request) {
        bulkActionPreviewValidator.validate(request);

        BulkActionPreviewResolution resolution =
                resolveBulkActionPreviewSelection(request.getSelection());
        return buildBulkActionPreviewResponse(
                request.getAction(), bulkActionPreviewGlobalLimit, resolution);
    }

    @Override
    public BulkActionPreviewResponseDto bulkActionPreview(
            UUID listId, ApplicationListEntryBulkActionPreviewRequestDto request) {
        bulkActionPreviewValidator.validateApplicationListEntryBulkActionPreview(listId, request);

        BulkActionPreviewResolution resolution =
                resolveApplicationListBulkActionPreviewSelection(listId, request.getSelection());
        return buildBulkActionPreviewResponse(
                request.getAction(), bulkActionPreviewSingleListLimit, resolution);
    }

    private BulkActionPreviewResponseDto buildBulkActionPreviewResponse(
            BulkActionType action, int limit, BulkActionPreviewResolution resolution) {
        BulkActionPreviewEligibility eligibility =
                resolveBulkActionPreviewEligibility(action, resolution);

        return new BulkActionPreviewResponseDto()
                .action(action)
                .limit(limit)
                .selectedCount(resolution.selectedCount())
                .eligibleCount(eligibility.eligibleCount())
                .ineligibleCount(resolution.selectedCount() - eligibility.eligibleCount())
                .entryIds(eligibility.entryIds())
                .entries(eligibility.entries());
    }

    private BulkActionPreviewResolution resolveBulkActionPreviewSelection(
            BulkActionSelectionDto selection) {
        if (selection.getSelectionType() == BulkActionSelectionType.IDS) {
            return resolveIdsBulkActionPreview(selection);
        }

        return resolveFilterBulkActionPreview(selection);
    }

    private BulkActionPreviewResolution resolveApplicationListBulkActionPreviewSelection(
            UUID listId, ApplicationListEntryBulkActionSelectionDto selection) {
        if (selection.getSelectionType() == BulkActionSelectionType.IDS) {
            return resolveApplicationListIdsBulkActionPreview(listId, selection);
        }

        return resolveApplicationListFilterBulkActionPreview(listId, selection);
    }

    private BulkActionPreviewEligibility resolveBulkActionPreviewEligibility(
            BulkActionType action, BulkActionPreviewResolution resolution) {
        if (action != BulkActionType.RESULT_SELECTED
                && action != BulkActionType.UPDATE_FEE_DETAILS) {
            return new BulkActionPreviewEligibility(
                    resolution.entryIds(), resolution.entries(), resolution.entryIds().size());
        }

        int eligibleCount =
                Math.toIntExact(
                        resolution.entries().stream()
                                .filter(entry -> isBulkActionEligible(action, entry))
                                .count());

        return new BulkActionPreviewEligibility(
                resolution.entryIds(), resolution.entries(), eligibleCount);
    }

    private static boolean isBulkActionEligible(BulkActionType action, EntryGetSummaryDto entry) {
        return switch (action) {
            case RESULT_SELECTED -> isResultSelectedEligible(entry);
            case UPDATE_FEE_DETAILS -> Boolean.TRUE.equals(entry.getIsFeeRequired());
            default -> true;
        };
    }

    private static boolean isResultSelectedEligible(EntryGetSummaryDto entry) {
        return entry.getStatus() == ApplicationListStatus.OPEN;
    }

    private BulkActionPreviewResolution resolveFilterBulkActionPreview(
            BulkActionSelectionDto selection) {
        EntryGetFilterDto filterDto =
                selection.getFilter() == null ? new EntryGetFilterDto() : selection.getFilter();
        List<UUID> excludedEntryIds = safeEntryIds(selection.getExcludedEntryIds());
        PagingWrapper pageable =
                pageableMapper.from(
                        selection.getSort(),
                        bulkActionPreviewGlobalLimit,
                        ApplicationEntrySortConfig.SEARCH,
                        Sort.Direction.ASC);

        Page<ApplicationListEntryGetSummaryProjection> resultPage =
                searchForBulkActionPreviewSummary(filterDto, List.of(), excludedEntryIds, pageable);

        int selectedCount = selectedCountWithinLimit(resultPage.getTotalElements());
        List<ApplicationListEntryGetSummaryProjection> entries = resultPage.getContent();

        return new BulkActionPreviewResolution(
                selectedCount, toEntryIds(entries), buildEntrySummaries(entries));
    }

    private BulkActionPreviewResolution resolveIdsBulkActionPreview(
            BulkActionSelectionDto selection) {
        List<UUID> selectedEntryIds = List.copyOf(selection.getEntryIds());
        bulkActionPreviewValidator.validateLimit(
                selectedEntryIds.size(), bulkActionPreviewGlobalLimit);

        PagingWrapper pageable =
                pageableMapper.from(
                        List.of(),
                        bulkActionPreviewGlobalLimit,
                        ApplicationEntrySortConfig.SEARCH,
                        Sort.Direction.ASC);

        Page<ApplicationListEntryGetSummaryProjection> resultPage =
                searchForBulkActionPreviewSummary(
                        new EntryGetFilterDto(), selectedEntryIds, List.of(), pageable);
        List<ApplicationListEntryGetSummaryProjection> entries =
                orderEntriesBySelectedIds(resultPage.getContent(), selectedEntryIds);

        return new BulkActionPreviewResolution(
                Math.toIntExact(resultPage.getTotalElements()),
                presentSelectedEntryIds(entries, selectedEntryIds),
                buildEntrySummaries(entries));
    }

    private BulkActionPreviewResolution resolveApplicationListFilterBulkActionPreview(
            UUID listId, ApplicationListEntryBulkActionSelectionDto selection) {
        EntryApplicationListGetFilterDto filterDto =
                normaliseEntryListFilter(selection.getFilter());
        List<UUID> excludedEntryIds = safeEntryIds(selection.getExcludedEntryIds());
        PagingWrapper pageable =
                pageableMapper.from(
                        selection.getSort(),
                        bulkActionPreviewSingleListLimit,
                        ApplicationEntrySortConfig.BY_LIST_ID,
                        Sort.Direction.ASC);

        Page<ApplicationListEntryGetSummaryProjection> resultPage =
                searchForBulkActionPreviewSummary(
                        listId, filterDto, List.of(), excludedEntryIds, pageable);

        int selectedCount =
                selectedCountWithinLimit(
                        resultPage.getTotalElements(), bulkActionPreviewSingleListLimit);
        List<ApplicationListEntryGetSummaryProjection> entries = resultPage.getContent();

        return new BulkActionPreviewResolution(
                selectedCount, toEntryIds(entries), buildEntrySummaries(entries));
    }

    private BulkActionPreviewResolution resolveApplicationListIdsBulkActionPreview(
            UUID listId, ApplicationListEntryBulkActionSelectionDto selection) {
        List<UUID> selectedEntryIds = List.copyOf(selection.getEntryIds());
        bulkActionPreviewValidator.validateLimit(
                selectedEntryIds.size(), bulkActionPreviewSingleListLimit);

        PagingWrapper pageable =
                pageableMapper.from(
                        List.of(),
                        bulkActionPreviewSingleListLimit,
                        ApplicationEntrySortConfig.BY_LIST_ID,
                        Sort.Direction.ASC);

        Page<ApplicationListEntryGetSummaryProjection> resultPage =
                searchForBulkActionPreviewSummary(
                        listId,
                        new EntryApplicationListGetFilterDto(),
                        selectedEntryIds,
                        List.of(),
                        pageable);
        List<ApplicationListEntryGetSummaryProjection> entries =
                orderEntriesBySelectedIds(resultPage.getContent(), selectedEntryIds);

        return new BulkActionPreviewResolution(
                Math.toIntExact(resultPage.getTotalElements()),
                presentSelectedEntryIds(entries, selectedEntryIds),
                buildEntrySummaries(entries));
    }

    private Page<ApplicationListEntryGetSummaryProjection> searchForBulkActionPreviewSummary(
            EntryGetFilterDto filterDto,
            List<UUID> entryIds,
            List<UUID> excludedEntryIds,
            PagingWrapper pageable) {
        Status status = applicationListEntryMapStructMapper.toStatus(filterDto.getStatus());
        boolean hasEntryIds = !entryIds.isEmpty();
        boolean hasExcludedEntryIds = !excludedEntryIds.isEmpty();

        return applicationListEntryRepository.searchForBulkActionPreviewSummary(
                null,
                filterDto.getDate() != null,
                filterDto.getDate(),
                filterDto.getCourtCode(),
                filterDto.getOtherLocationDescription(),
                filterDto.getCjaCode(),
                filterDto.getApplicantOrganisation(),
                filterDto.getApplicantSurname(),
                filterDto.getApplicantName(),
                filterDto.getStandardApplicantCode(),
                status,
                filterDto.getRespondentOrganisation(),
                filterDto.getRespondentSurname(),
                filterDto.getRespondentName(),
                filterDto.getRespondentPostcode(),
                filterDto.getAccountReference(),
                filterDto.getApplicationTitle(),
                null,
                null,
                null,
                hasEntryIds,
                queryEntryIds(entryIds),
                hasExcludedEntryIds,
                queryEntryIds(excludedEntryIds),
                pageable.getPageable());
    }

    private Page<ApplicationListEntryGetSummaryProjection> searchForBulkActionPreviewSummary(
            UUID listId,
            EntryApplicationListGetFilterDto filterDto,
            List<UUID> entryIds,
            List<UUID> excludedEntryIds,
            PagingWrapper pageable) {
        boolean hasEntryIds = !entryIds.isEmpty();
        boolean hasExcludedEntryIds = !excludedEntryIds.isEmpty();

        return applicationListEntryRepository.searchForBulkActionPreviewSummary(
                listId,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                filterDto.getApplicantName(),
                null,
                null,
                null,
                null,
                filterDto.getRespondentName(),
                filterDto.getRespondentPostcode(),
                filterDto.getAccountReference(),
                filterDto.getApplicationTitle(),
                filterDto.getResulted(),
                filterDto.getFeeRequired(),
                filterDto.getSequenceNumber(),
                hasEntryIds,
                queryEntryIds(entryIds),
                hasExcludedEntryIds,
                queryEntryIds(excludedEntryIds),
                pageable.getPageable());
    }

    private int selectedCountWithinLimit(long selectedCount) {
        return selectedCountWithinLimit(selectedCount, bulkActionPreviewGlobalLimit);
    }

    private int selectedCountWithinLimit(long selectedCount, int limit) {
        bulkActionPreviewValidator.validateLimit(selectedCount, limit);
        return Math.toIntExact(selectedCount);
    }

    private static List<UUID> safeEntryIds(List<UUID> entryIds) {
        return entryIds == null ? List.of() : List.copyOf(entryIds);
    }

    private static List<UUID> queryEntryIds(List<UUID> entryIds) {
        return entryIds.isEmpty() ? List.of(BULK_ACTION_PREVIEW_PLACEHOLDER_ENTRY_ID) : entryIds;
    }

    private List<ApplicationListEntryGetSummaryProjection> orderEntriesBySelectedIds(
            List<ApplicationListEntryGetSummaryProjection> entries, List<UUID> selectedEntryIds) {
        Map<String, ApplicationListEntryGetSummaryProjection> entriesByUuid =
                entries.stream()
                        .collect(
                                Collectors.toMap(
                                        ApplicationListEntryGetSummaryProjection::getUuid,
                                        entry -> entry,
                                        (first, second) -> first));

        return selectedEntryIds.stream()
                .map(UUID::toString)
                .map(entriesByUuid::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private static List<UUID> toEntryIds(List<ApplicationListEntryGetSummaryProjection> entries) {
        return entries.stream().map(ApplicationEntryServiceImpl::getEntryUuid).toList();
    }

    private static List<UUID> presentSelectedEntryIds(
            List<ApplicationListEntryGetSummaryProjection> entries, List<UUID> selectedEntryIds) {
        Set<String> returnedUuids =
                entries.stream()
                        .map(ApplicationListEntryGetSummaryProjection::getUuid)
                        .collect(Collectors.toSet());

        return selectedEntryIds.stream()
                .filter(entryId -> returnedUuids.contains(entryId.toString()))
                .toList();
    }

    private static UUID getEntryUuid(ApplicationListEntryGetSummaryProjection entry) {
        return UUID.fromString(entry.getUuid());
    }

    @Override
    @Transactional
    @NestedAudit
    public MatchResponse<EntryGetDetailDto> createEntry(
            PayloadForCreate<EntryCreateDto> entryCreateDto) {
        log.debug("Started create application entry for list {}", entryCreateDto.getId());

        // creates the entity and return the etag for matching
        MatchResponse<EntryGetDetailDto> getDetailDto =
                createApplicationEntryValidator.validate(
                        entryCreateDto,
                        (dto, success) ->
                                auditService.processAudit(
                                        AppListEntryAuditOperation.CREATE_APP_ENTRY_LIST,
                                        req -> {
                                            NameAddress applicantToSave =
                                                    createApplicant(entryCreateDto);

                                            NameAddress respondentToSave =
                                                    createRespondent(entryCreateDto);

                                            // save the list
                                            ApplicationListEntry listEntryEntity =
                                                    applicationListEntryEntityMapper
                                                            .toApplicationListEntry(
                                                                    entryCreateDto.getData(),
                                                                    success.getWordingSentence()
                                                                            .substitute(
                                                                                    entryCreateDto
                                                                                            .getData()
                                                                                            .getWordingFields())
                                                                            .getSubstitutedString(),
                                                                    success.getSa(),
                                                                    applicantToSave,
                                                                    respondentToSave,
                                                                    success.getApplicationCode(),
                                                                    success.getApplicationList(),
                                                                    YesOrNo.NO);

                                            Long alId = success.getApplicationList().getId();
                                            short seq = allocateNextSequence(alId);
                                            listEntryEntity.setSequenceNumber(seq);

                                            listEntryEntity =
                                                    refreshEntity(
                                                            applicationListEntryRepository.save(
                                                                    listEntryEntity));

                                            log.debug(
                                                    "Created application entry with id: {}",
                                                    listEntryEntity.getId());

                                            List<AppListEntryFeeStatus> statusList =
                                                    createFeeStatus(
                                                            listEntryEntity, entryCreateDto);

                                            List<AppListEntryOfficial> officialList =
                                                    createOfficial(listEntryEntity, entryCreateDto);

                                            createFees(success, listEntryEntity, entryCreateDto);

                                            EntryGetDetailDto entryGetDetailDto =
                                                    applicationListEntryMapStructMapper
                                                            .toEntryGetDetailDto(
                                                                    listEntryEntity,
                                                                    statusList,
                                                                    success.getFee(),
                                                                    officialList,
                                                                    success.getSa());
                                            entryGetDetailDto.setHasOffsiteFee(
                                                    entryCreateDto.getData().getHasOffsiteFee());

                                            return Optional.of(
                                                    new AuditableResult<>(
                                                            MatchResponse.of(
                                                                    entryGetDetailDto,
                                                                    getKeyablesForCreateUpdateEtag(
                                                                            listEntryEntity)),
                                                            listEntryEntity));
                                        }));

        log.debug(
                "Finished create application entry for list {} entry {}",
                entryCreateDto.getId(),
                getDetailDto.getPayload().getId());

        return getDetailDto;
    }

    @Override
    @Transactional
    @NestedAudit
    public MatchResponse<EntryGetDetailDto> updateEntry(PayloadForUpdateEntry updateEntry) {
        log.debug(
                "Started update application entry {} in list {}",
                updateEntry.getEntryId(),
                updateEntry.getId());

        // creates the entity and return the etag for matching
        MatchResponse<EntryGetDetailDto> getDetailDto =
                updateApplicationEntryValidator.validate(
                        updateEntry,
                        (dto, success) ->
                                // lets check the concurrent match before we process the update
                                matchService.matchOnRequest(
                                        () ->
                                                auditService.processAudit(
                                                        BeanUtil.copyBean(
                                                                success.getApplicationEntryId()),
                                                        AppListEntryAuditOperation
                                                                .UPDATE_APP_ENTRY_LIST,
                                                        req -> {

                                                            // save the applicant
                                                            updateApplicant(updateEntry, success);

                                                            // save the respondent
                                                            updateRespondent(updateEntry, success);

                                                            // update entry with a standard
                                                            // applicant
                                                            updateStandardApplicant(success);

                                                            // save the list
                                                            ApplicationListEntry listEntryEntity =
                                                                    success.getApplicationEntryId();

                                                            // update the core list data
                                                            applicationListEntryEntityMapper
                                                                    .toApplicationListEntry(
                                                                            updateEntry.getData(),
                                                                            success.getWordingSentence()
                                                                                    .substitute(
                                                                                            updateEntry
                                                                                                    .getData()
                                                                                                    .getWordingFields())
                                                                                    .getSubstitutedString(),
                                                                            success.getSa(),
                                                                            success
                                                                                    .getApplicationCode(),
                                                                            success
                                                                                    .getApplicationList(),
                                                                            listEntryEntity);

                                                            // save the core list data
                                                            listEntryEntity =
                                                                    refreshEntity(
                                                                            applicationListEntryRepository
                                                                                    .save(
                                                                                            listEntryEntity));
                                                            log.debug(
                                                                    "Created application entry with id: {}",
                                                                    listEntryEntity.getId());

                                                            // add any new fee statuses while
                                                            // preserving existing history
                                                            updateFeeStatus(updateEntry, success);

                                                            // update the officials
                                                            List<AppListEntryOfficial>
                                                                    updatedOfficialList =
                                                                            updateOfficials(
                                                                                    updateEntry,
                                                                                    success);

                                                            // update the fees for the entry
                                                            updateFees(success, updateEntry);

                                                            List<AppListEntryFeeStatus>
                                                                    updatedFeeStatusLst =
                                                                            appListEntryFeeStatusRepository
                                                                                    .findByAppListEntryId(
                                                                                            listEntryEntity
                                                                                                    .getId());

                                                            // create the fee entry mappings
                                                            EntryGetDetailDto entryGetDetailDto =
                                                                    applicationListEntryMapStructMapper
                                                                            .toEntryGetDetailDto(
                                                                                    success
                                                                                            .getApplicationEntryId(),
                                                                                    updatedFeeStatusLst,
                                                                                    success
                                                                                            .getFee(),
                                                                                    updatedOfficialList,
                                                                                    success
                                                                                            .getSa());
                                                            entryGetDetailDto.setHasOffsiteFee(
                                                                    updateEntry
                                                                            .getData()
                                                                            .getHasOffsiteFee());

                                                            return Optional.of(
                                                                    new AuditableResult<>(
                                                                            MatchResponse.of(
                                                                                    entryGetDetailDto,
                                                                                    getKeyablesForCreateUpdateEtag(
                                                                                            listEntryEntity)),
                                                                            success
                                                                                    .getApplicationEntryId()));
                                                        }),

                                        // return the latest entities for the entry read on the
                                        // update
                                        getKeyablesForCreateUpdateEtag(
                                                success.getApplicationEntryId())));

        log.debug(
                "Finished update application entry {} in list {}",
                updateEntry.getEntryId(),
                updateEntry.getId());

        return getDetailDto;
    }

    @Override
    @Transactional
    public MatchResponse<Void> updateClosedEntry(PayloadForUpdateClosedEntry updateEntry) {

        return updateClosedApplicationEntryValidator.validate(
                updateEntry,
                (ue, success) ->
                        // lets check the concurrent match before we process the update
                        matchService.matchOnRequest(
                                () ->
                                        auditService.processAudit(
                                                BeanUtil.copyBean(success.getApplicationEntryId()),
                                                AppListEntryAuditOperation
                                                        .UPDATE_CLOSED_APP_ENTRY_LIST,
                                                req -> {
                                                    String updatedNotes =
                                                            appendNotes(
                                                                    success.getApplicationEntryId()
                                                                            .getNotes(),
                                                                    updateEntry
                                                                            .getData()
                                                                            .getAdditionalNotes());
                                                    validateNotesLength(updatedNotes);
                                                    success.getApplicationEntryId()
                                                            .setNotes(updatedNotes);

                                                    // update the notes by appending with the
                                                    // alternative
                                                    // notes
                                                    applicationListEntryRepository.save(
                                                            success.getApplicationEntryId());

                                                    return Optional.of(
                                                            new AuditableResult<>(
                                                                    MatchResponse.of(
                                                                            null,
                                                                            getKeyablesForCreateUpdateEtag(
                                                                                    success
                                                                                            .getApplicationEntryId())),
                                                                    success
                                                                            .getApplicationEntryId()));
                                                }),
                                // return the latest entities for the entry read on the update
                                getKeyablesForCreateUpdateEtag(success.getApplicationEntryId())));
    }

    private static void validateNotesLength(String notes) {
        if (notes != null && notes.length() > NOTES_MAX_LENGTH) {
            throw new AppRegistryException(
                    AppListEntryError.NOTES_TOO_LONG,
                    "notes must not be longer than %s characters".formatted(NOTES_MAX_LENGTH));
        }
    }

    private static String appendNotes(String existingNotes, String additionalNotes) {
        if (additionalNotes == null || additionalNotes.isBlank()) {
            return existingNotes;
        }

        if (existingNotes == null || existingNotes.isBlank()) {
            return additionalNotes;
        }

        return existingNotes + " " + additionalNotes;
    }

    @Override
    @Transactional
    public void replaceOfficials(UUID listId, BulkOfficialsUpdateDto bulkOfficialsUpdateDto) {
        var payload = new BulkUpdateOfficialsPayload(listId, bulkOfficialsUpdateDto);

        bulkUpdateOfficialsValidator.validate(
                payload,
                (req, success) -> {
                    List<ApplicationListEntry> entries = new ArrayList<>(success.getEntries());
                    entries.sort(Comparator.comparing(ApplicationListEntry::getSequenceNumber));
                    List<Official> replacementOfficials = req.data().getOfficials();
                    var applicationList =
                            entries.isEmpty() ? null : entries.getFirst().getApplicationList();

                    List<UUID> entryUuids =
                            entries.stream().map(ApplicationListEntry::getUuid).toList();
                    Map<UUID, List<AppListEntryOfficial>> existingOfficialsByEntryUuid =
                            appListEntryOfficialRepository
                                    .findByAppListEntry_UuidIn(entryUuids)
                                    .stream()
                                    .collect(
                                            Collectors.groupingBy(
                                                    official ->
                                                            official.getAppListEntry().getUuid()));

                    List<Long> entryIds =
                            entries.stream().map(ApplicationListEntry::getId).toList();
                    List<AppListEntryOfficial> officialsToCreate =
                            new ArrayList<>(entries.size() * replacementOfficials.size());
                    List<AppListEntryOfficial> deletedOfficials =
                            existingOfficialsByEntryUuid.values().stream()
                                    .flatMap(List::stream)
                                    .toList();
                    var deletedOfficialsAudit =
                            new BulkUpdateOfficialsAudit(
                                    applicationList != null ? applicationList.getId() : null,
                                    listId,
                                    entryUuids,
                                    entries.size(),
                                    BulkUpdateOfficialsAudit.formatDeletedOfficials(
                                            deletedOfficials));
                    var replacementOfficialsAudit =
                            new BulkUpdateOfficialsAudit(
                                    applicationList != null ? applicationList.getId() : null,
                                    listId,
                                    entryUuids,
                                    entries.size(),
                                    BulkUpdateOfficialsAudit.formatReplacementOfficials(
                                            replacementOfficials));

                    for (ApplicationListEntry entry : entries) {
                        addOfficialsForEntry(officialsToCreate, entry, replacementOfficials);
                    }

                    auditService.processAudit(
                            deletedOfficialsAudit,
                            AppListEntryAuditOperation.BULK_UPDATE_OFFICIALS,
                            ignored -> {
                                if (!entryIds.isEmpty()) {
                                    appListEntryOfficialRepository.deleteAllForEntryIds(entryIds);
                                }

                                appListEntryOfficialRepository.saveAll(officialsToCreate);
                                return Optional.of(
                                        new AuditableResult<>(null, replacementOfficialsAudit));
                            });

                    log.info(
                            "Completed bulk officials replacement for {} entries in list {}",
                            entries.size(),
                            listId);
                    return null;
                });
    }

    @Override
    @NestedAudit
    @Transactional
    public BulkUpdateResponseDto bulkUpdateFees(UUID listId, BulkFeesUpdateDto bulkFeesUpdateDto) {
        var payload = new BulkUpdateFeesPayload(listId, bulkFeesUpdateDto);
        int requestedCount = requestedBulkFeeUpdateCount(bulkFeesUpdateDto);
        long startNanos = System.nanoTime();

        log.info("Starting bulk fee update listId={} requestedCount={}", listId, requestedCount);

        try {
            BulkUpdateResponseDto response =
                    bulkUpdateFeesValidator.validate(
                            payload, (req, success) -> processBulkFeeUpdate(req, success, listId));

            long durationNanos = System.nanoTime() - startNanos;
            recordBulkFeeUpdateMetrics(
                    METRIC_SUCCEEDED, response.getUpdatedCount(), Duration.ofNanos(durationNanos));
            log.info(
                    "Completed bulk fee update listId={} requestedCount={} updatedCount={} status={} durationMs={}",
                    listId,
                    requestedCount,
                    response.getUpdatedCount(),
                    response.getStatus(),
                    Duration.ofNanos(durationNanos).toMillis());

            return response;
        } catch (RuntimeException exception) {
            long durationNanos = System.nanoTime() - startNanos;
            recordBulkFeeUpdateMetrics(METRIC_FAILED, null, Duration.ofNanos(durationNanos));
            log.warn(
                    "Failed bulk fee update listId={} requestedCount={} status=FAILED errorType={} durationMs={}",
                    listId,
                    requestedCount,
                    exception.getClass().getSimpleName(),
                    Duration.ofNanos(durationNanos).toMillis());

            throw exception;
        }
    }

    @Override
    public List<UUID> getApplicationListEntriesByJobId(UUID jobId) {
        List<AsyncJobsAppListEntry> entryIds =
                asyncJobAppListEntryRepository.findByAsyncJobId(jobId);

        return jobExistanceValidator.validate(
                jobId,
                (uuid, success) -> {
                    if (entryIds.isEmpty()) {
                        throw new AppRegistryException(
                                JobError.JOB_DOES_NOT_EXIST_OR_NOT_FOR_USER,
                                "No entries found for jobId: " + jobId);
                    }

                    return entryIds.stream().map(AsyncJobsAppListEntry::getAppListEntryId).toList();
                });
    }

    private int requestedBulkFeeUpdateCount(BulkFeesUpdateDto bulkFeesUpdateDto) {
        return Optional.ofNullable(bulkFeesUpdateDto)
                .map(BulkFeesUpdateDto::getEntryIds)
                .map(Set::size)
                .orElse(0);
    }

    private void recordBulkFeeUpdateMetrics(
            String status, Integer updatedCount, Duration duration) {
        meterRegistry
                .counter(BULK_FEE_UPDATE_REQUESTS_METRIC, METRIC_STATUS_TAG, status)
                .increment();
        meterRegistry
                .timer(BULK_FEE_UPDATE_DURATION_METRIC, METRIC_STATUS_TAG, status)
                .record(duration);

        if (METRIC_SUCCEEDED.equals(status) && updatedCount != null) {
            meterRegistry
                    .summary(BULK_FEE_UPDATE_ENTRIES_METRIC, METRIC_STATUS_TAG, status)
                    .record(updatedCount);
        }
    }

    private BulkUpdateResponseDto processBulkFeeUpdate(
            BulkUpdateFeesPayload req, BulkUpdateFeesValidationSuccess success, UUID listId) {
        List<ApplicationListEntry> entries = new ArrayList<>(success.getEntries());
        entries.sort(Comparator.comparing(ApplicationListEntry::getSequenceNumber));

        var feeUpdateContext = prepareBulkFeeUpdateContext(req, entries);
        applyRequestedFeeChanges(entries, feeUpdateContext);

        var oldAudit = buildExistingFeesAudit(listId, entries, feeUpdateContext);
        var newAudit = buildRequestedFeesAudit(listId, req, entries, feeUpdateContext);

        return auditService.processAudit(
                oldAudit,
                AppListEntryAuditOperation.BULK_UPDATE_FEES,
                ignored -> persistBulkFeeUpdate(req, entries, feeUpdateContext, newAudit));
    }

    private BulkFeeUpdateContext prepareBulkFeeUpdateContext(
            BulkUpdateFeesPayload req, List<ApplicationListEntry> entries) {
        List<BulkFeeDetailsDto> feeDetails = req.data().getFeeDetails().orElse(List.of());
        boolean hasOffsiteFee =
                Boolean.TRUE.equals(req.data().getHasOffsiteFee().orElse(Boolean.FALSE));
        Supplier<Fee> offsiteFeeSupplier = offsiteFeeSupplier(hasOffsiteFee);
        List<UUID> entryUuids = entries.stream().map(ApplicationListEntry::getUuid).toList();
        Map<Long, UUID> entryUuidsById =
                entries.stream()
                        .collect(
                                Collectors.toMap(
                                        ApplicationListEntry::getId,
                                        ApplicationListEntry::getUuid));
        List<AppListEntryFeeStatus> existingFeeStatuses =
                appListEntryFeeStatusRepository.findByAppListEntry_UuidIn(entryUuids);
        Set<Long> entryIdsWithOffsiteMapping =
                hasOffsiteFee ? getEntryIdsWithOffsiteMapping(entries) : Set.of();
        List<AppListEntryFeeStatus> feeStatusesToCreate =
                new ArrayList<>(entries.size() * feeDetails.size());
        List<AppListEntryFeeId> offsiteFeeMappingsToCreate = new ArrayList<>();

        return new BulkFeeUpdateContext(
                feeDetails,
                hasOffsiteFee,
                offsiteFeeSupplier,
                entryUuids,
                entryUuidsById,
                existingFeeStatuses,
                entryIdsWithOffsiteMapping,
                feeStatusesToCreate,
                offsiteFeeMappingsToCreate);
    }

    private void applyRequestedFeeChanges(
            List<ApplicationListEntry> entries, BulkFeeUpdateContext feeUpdateContext) {
        if (!feeUpdateContext.feeDetails().isEmpty()) {
            for (ApplicationListEntry entry : entries) {
                appendFeeDetailsForEntry(
                        entry,
                        feeUpdateContext.feeDetails(),
                        feeUpdateContext.hasOffsiteFee(),
                        feeUpdateContext.offsiteFeeSupplier(),
                        feeUpdateContext.entryIdsWithOffsiteMapping(),
                        feeUpdateContext.feeStatusesToCreate(),
                        feeUpdateContext.offsiteFeeMappingsToCreate());
            }
            return;
        }

        if (feeUpdateContext.hasOffsiteFee()) {
            for (ApplicationListEntry entry : entries) {
                ensureOffsiteFeeMapping(
                        entry,
                        feeUpdateContext.offsiteFeeSupplier(),
                        feeUpdateContext.entryIdsWithOffsiteMapping(),
                        feeUpdateContext.offsiteFeeMappingsToCreate());
            }
            return;
        }

        for (ApplicationListEntry entry : entries) {
            deleteOffsiteFeeForEntry(entry);
        }
    }

    private BulkUpdateFeesAudit buildExistingFeesAudit(
            UUID listId,
            List<ApplicationListEntry> entries,
            BulkFeeUpdateContext feeUpdateContext) {
        return new BulkUpdateFeesAudit(
                applicationListId(entries),
                listId,
                feeUpdateContext.entryUuids(),
                entries.size(),
                BulkUpdateFeesAudit.formatExistingFeeStatuses(
                        feeUpdateContext.existingFeeStatuses()),
                BulkUpdateFeesAudit.formatOffsiteEntryIds(
                        feeUpdateContext.entryIdsWithOffsiteMapping(),
                        feeUpdateContext.entryUuidsById()));
    }

    private BulkUpdateFeesAudit buildRequestedFeesAudit(
            UUID listId,
            BulkUpdateFeesPayload req,
            List<ApplicationListEntry> entries,
            BulkFeeUpdateContext feeUpdateContext) {
        Set<Long> updatedOffsiteEntryIds =
                new HashSet<>(feeUpdateContext.entryIdsWithOffsiteMapping());
        feeUpdateContext.offsiteFeeMappingsToCreate().stream()
                .map(AppListEntryFeeId::getAppListEntryId)
                .forEach(updatedOffsiteEntryIds::add);

        return new BulkUpdateFeesAudit(
                applicationListId(entries),
                listId,
                feeUpdateContext.entryUuids(),
                entries.size(),
                BulkUpdateFeesAudit.formatRequestedFeeDetails(
                        feeUpdateContext.feeDetails(),
                        req.data().getHasOffsiteFee().orElse(Boolean.FALSE)),
                BulkUpdateFeesAudit.formatOffsiteEntryIds(
                        updatedOffsiteEntryIds, feeUpdateContext.entryUuidsById()));
    }

    private Optional<AuditableResult<BulkUpdateResponseDto, BulkUpdateFeesAudit>>
            persistBulkFeeUpdate(
                    BulkUpdateFeesPayload req,
                    List<ApplicationListEntry> entries,
                    BulkFeeUpdateContext feeUpdateContext,
                    BulkUpdateFeesAudit newAudit) {
        if (!feeUpdateContext.feeStatusesToCreate().isEmpty()) {
            appListEntryFeeStatusRepository.saveAll(feeUpdateContext.feeStatusesToCreate());
        }

        if (!feeUpdateContext.offsiteFeeMappingsToCreate().isEmpty()) {
            appListEntryFeeRepository.saveAll(feeUpdateContext.offsiteFeeMappingsToCreate());
        }

        var bulkUpdateResponse =
                new BulkUpdateResponseDto()
                        .totalCount(req.data().getEntryIds().size())
                        .updatedCount(entries.size())
                        .status(BulkUpdateResponseDto.StatusEnum.SUCCEEDED);

        return Optional.of(new AuditableResult<>(bulkUpdateResponse, newAudit));
    }

    private void appendFeeDetailsForEntry(
            ApplicationListEntry entry,
            List<BulkFeeDetailsDto> feeDetails,
            boolean hasOffsiteFee,
            Supplier<Fee> offsiteFeeSupplier,
            Set<Long> entryIdsWithOffsiteMapping,
            List<AppListEntryFeeStatus> feeStatusesToCreate,
            List<AppListEntryFeeId> offsiteFeeMappingsToCreate) {
        for (BulkFeeDetailsDto feeDetail : feeDetails) {
            feeStatusesToCreate.add(createBulkFeeStatus(entry, feeDetail));
        }

        if (hasOffsiteFee) {
            ensureOffsiteFeeMapping(
                    entry,
                    offsiteFeeSupplier,
                    entryIdsWithOffsiteMapping,
                    offsiteFeeMappingsToCreate);
        }
    }

    private record BulkFeeUpdateContext(
            List<BulkFeeDetailsDto> feeDetails,
            boolean hasOffsiteFee,
            Supplier<Fee> offsiteFeeSupplier,
            List<UUID> entryUuids,
            Map<Long, UUID> entryUuidsById,
            List<AppListEntryFeeStatus> existingFeeStatuses,
            Set<Long> entryIdsWithOffsiteMapping,
            List<AppListEntryFeeStatus> feeStatusesToCreate,
            List<AppListEntryFeeId> offsiteFeeMappingsToCreate) {}

    /**
     * creates the fees for the entry.
     *
     * @param success The successful validation result
     * @param listEntryEntity The entry entity
     * @param entryCreateDto The create payload containing the fees
     */
    private void createFees(
            CreateApplicationEntryValidationSuccess success,
            ApplicationListEntry listEntryEntity,
            PayloadForCreate<EntryCreateDto> entryCreateDto) {
        if (success.getFee() != null && success.getFee().mainFee() != null) {
            // save and audit
            auditService.processAudit(
                    AppListEntryAuditOperation.CREATE_FEE_ENTRY,
                    req -> {
                        // create the link between the entry and the
                        // fees
                        AppListEntryFeeId appListEntryFeeId = new AppListEntryFeeId();
                        appListEntryFeeId.setAppListEntryId(listEntryEntity.getId());
                        appListEntryFeeId.setFeeId(success.getFee().mainFee().getId());
                        var savedAppListEntryFeeId =
                                appListEntryFeeRepository.save(appListEntryFeeId);

                        log.debug(
                                "Created Fee: {} to Entry: {}",
                                appListEntryFeeId.getFeeId(),
                                appListEntryFeeId.getAppListEntryId());

                        return Optional.of(new AuditableResult<>(null, savedAppListEntryFeeId));
                    });
        }

        if (success.getFee() != null
                && success.getFee().offsiteFee() != null
                && entryCreateDto.getData() != null
                && Boolean.TRUE.equals(entryCreateDto.getData().getHasOffsiteFee())) {
            // save the offsite fee
            auditService.processAudit(
                    AppListEntryAuditOperation.CREATE_FEE_ENTRY,
                    req -> {
                        // create the link between the entry and the
                        // fees
                        AppListEntryFeeId appListEntryFeeId = new AppListEntryFeeId();
                        appListEntryFeeId.setAppListEntryId(listEntryEntity.getId());
                        appListEntryFeeId.setFeeId(success.getFee().offsiteFee().getId());
                        var savedAppListEntryFeeId =
                                appListEntryFeeRepository.save(appListEntryFeeId);

                        log.debug(
                                CREATED_OFFSITE_FEE_LOG,
                                appListEntryFeeId.getFeeId(),
                                appListEntryFeeId.getAppListEntryId());

                        return Optional.of(new AuditableResult<>(null, savedAppListEntryFeeId));
                    });
        }
    }

    /**
     * create all officials for the entry.
     *
     * @param listEntryEntity The list entry entity to add the officials to
     * @param entryCreateDto The create payload containing the officials
     * @return The application list entry officials that were created
     */
    private List<AppListEntryOfficial> createOfficial(
            ApplicationListEntry listEntryEntity, PayloadForCreate<EntryCreateDto> entryCreateDto) {
        List<AppListEntryOfficial> officialList = new ArrayList<>();
        if (entryCreateDto.getData().getOfficials() != null) {
            // create the official for the entry
            for (Official official : entryCreateDto.getData().getOfficials()) {

                // save and audit
                auditService.processAudit(
                        AppListEntryAuditOperation.CREATE_OFFICIAL_ENTRY,
                        req -> {
                            AppListEntryOfficial newOfficialEntity =
                                    appListEntryOfficialRepository.save(
                                            applicationListEntryEntityMapper.toOfficial(
                                                    official, listEntryEntity));

                            log.debug(
                                    "Official created and mapped to application entry with id: {}",
                                    newOfficialEntity.getId());
                            officialList.add(newOfficialEntity);

                            return Optional.of(new AuditableResult<>(null, newOfficialEntity));
                        });
            }
        }
        return officialList;
    }

    /**
     * create all fee statuses and map them to the entry.
     *
     * @param listEntryEntity The list entry entity to add the officials to
     * @param entryCreateDto The create payload containing the fee statuses
     * @return The application fees that were created
     */
    private List<AppListEntryFeeStatus> createFeeStatus(
            ApplicationListEntry listEntryEntity, PayloadForCreate<EntryCreateDto> entryCreateDto) {
        List<AppListEntryFeeStatus> statusList = new ArrayList<>();

        List<FeeStatus> feeStatuses =
                entryCreateDto.getData().getFeeStatuses() == null
                        ? List.of()
                        : entryCreateDto.getData().getFeeStatuses();

        for (FeeStatus feeStatus : feeStatuses) {
            AppListEntryFeeStatus appListEntryFeeStatus =
                    applicationListEntryEntityMapper.toFeeStatus(feeStatus, listEntryEntity);
            saveFeeStatus(appListEntryFeeStatus, statusList);
        }

        return statusList;
    }

    private AppListEntryFeeStatus createBulkFeeStatus(
            ApplicationListEntry entry, BulkFeeDetailsDto feeDetails) {
        AppListEntryFeeStatus feeStatus = new AppListEntryFeeStatus();
        feeStatus.setAppListEntry(entry);
        feeStatus.setAlefsFeeStatus(
                ApplicationListEntryEntityMapper.toStatus(feeDetails.getPaymentStatus()));
        feeStatus.setAlefsFeeStatusDate(feeDetails.getStatusDate());
        feeStatus.setAlefsPaymentReference(feeDetails.getPaymentReference());
        feeStatus.setAlefsStatusCreationDate(OffsetDateTime.now(clock));
        return feeStatus;
    }

    private void saveFeeStatus(
            AppListEntryFeeStatus appListEntryFeeStatus, List<AppListEntryFeeStatus> statusList) {
        auditService.processAudit(
                AppListEntryAuditOperation.CREATE_FEE_STATUS_ENTRY,
                req -> {
                    AppListEntryFeeStatus createdAppListStatus =
                            appListEntryFeeStatusRepository.save(appListEntryFeeStatus);
                    statusList.add(createdAppListStatus);
                    log.debug(
                            "Fee status created and mapped to application entry with id: {}",
                            createdAppListStatus.getId());
                    return Optional.of(new AuditableResult<>(null, createdAppListStatus));
                });
    }

    private void deleteFeeStatusesForEntry(UUID entryId) {
        List<AppListEntryFeeStatus> feeStatuses =
                appListEntryFeeStatusRepository.getFeeStatusByEntryUuid(entryId);

        for (AppListEntryFeeStatus feeStatus : feeStatuses) {
            auditService.processAudit(
                    feeStatus,
                    AppListEntryAuditOperation.DELETE_FEE_STATUS_ENTRY,
                    req -> {
                        appListEntryFeeStatusRepository.delete(feeStatus);
                        return Optional.empty();
                    });
        }

        if (!feeStatuses.isEmpty()) {
            appListEntryFeeStatusRepository.flush();
        }
    }

    private Supplier<Fee> offsiteFeeSupplier(boolean hasOffsiteFee) {
        if (!hasOffsiteFee) {
            return () -> null;
        }

        return new Supplier<>() {
            private Fee offsiteFee;

            @Override
            public Fee get() {
                if (offsiteFee == null) {
                    offsiteFee =
                            feeRepository.findOffsite(businessDateProvider.currentUkDate()).stream()
                                    .findFirst()
                                    .orElseThrow(
                                            () ->
                                                    new AppRegistryException(
                                                            AppListEntryError
                                                                    .FEE_OFFSITE_NOT_SUITABLE,
                                                            "Offsite fee does not exist"));
                }
                return offsiteFee;
            }
        };
    }

    private Set<Long> getEntryIdsWithOffsiteMapping(List<ApplicationListEntry> entries) {
        return appListEntryFeeRepository
                .getOffsiteEntryFeesForEntries(
                        entries.stream().map(ApplicationListEntry::getId).toList())
                .stream()
                .map(AppListEntryFeeId::getAppListEntryId)
                .collect(Collectors.toSet());
    }

    private void ensureOffsiteFeeMapping(
            ApplicationListEntry entry,
            Supplier<Fee> offsiteFeeSupplier,
            Set<Long> entryIdsWithOffsiteMapping,
            List<AppListEntryFeeId> offsiteFeeMappingsToCreate) {
        if (entryIdsWithOffsiteMapping.contains(entry.getId())) {
            return;
        }

        Fee offsiteFee = offsiteFeeSupplier.get();
        AppListEntryFeeId offsiteEntryFee = new AppListEntryFeeId();
        offsiteEntryFee.setAppListEntryId(entry.getId());
        offsiteEntryFee.setFeeId(offsiteFee.getId());
        offsiteFeeMappingsToCreate.add(offsiteEntryFee);
        log.debug(
                CREATED_OFFSITE_FEE_LOG,
                offsiteEntryFee.getFeeId(),
                offsiteEntryFee.getAppListEntryId());
    }

    private static Long applicationListId(List<ApplicationListEntry> entries) {
        return entries.isEmpty() ? null : entries.getFirst().getApplicationList().getId();
    }

    /**
     * creates the applicant for the entry.
     *
     * @param entryCreateDto The applicant data to create
     * @return The created applicant
     */
    private NameAddress createApplicant(PayloadForCreate<EntryCreateDto> entryCreateDto) {
        // save the applicant
        NameAddress applicantToSave = null;
        if (entryCreateDto.getData().getApplicant() != null
                && (entryCreateDto.getData().getApplicant().getOrganisation() != null
                        || entryCreateDto.getData().getApplicant().getPerson() != null)) {

            applicantToSave =
                    auditService.processAudit(
                            AppListEntryAuditOperation.CREATE_APPLICANT,
                            req -> {
                                NameAddress applicantToAdded =
                                        applicantMapper.toApplicant(
                                                entryCreateDto.getData().getApplicant());
                                nameAddressRepository.save(applicantToAdded);
                                log.debug(
                                        "Created applicant with id: {}", applicantToAdded.getId());

                                return Optional.of(
                                        new AuditableResult<>(applicantToAdded, applicantToAdded));
                            });
        }
        return applicantToSave;
    }

    /**
     * creates the respondent for the application entry.
     *
     * @param entryCreateDto The applicant data to create
     * @return The created respondent
     */
    private NameAddress createRespondent(PayloadForCreate<EntryCreateDto> entryCreateDto) {
        // save the respondent
        NameAddress respondentToSave = null;
        if (entryCreateDto.getData().getRespondent() != null) {
            respondentToSave =
                    auditService.processAudit(
                            AppListEntryAuditOperation.CREATE_RESPONDENT,
                            req -> {
                                NameAddress respondentToAdded =
                                        nameAddressRepository.save(
                                                applicantMapper.toRespondent(
                                                        entryCreateDto.getData().getRespondent()));
                                log.debug(
                                        "Created respondent with id: {}",
                                        respondentToAdded.getId());

                                return Optional.of(
                                        new AuditableResult<>(
                                                respondentToAdded, respondentToAdded));
                            });
        }

        return respondentToSave;
    }

    /**
     * Updates the applicant. Deletes the old respondent.
     *
     * @param updateEntry the update payload
     * @param success The success validation response
     */
    private void updateRespondent(
            PayloadForUpdateEntry updateEntry, UpdateApplicationEntryValidationSuccess success) {
        log.debug("Updating respondent");

        // capture the respondent before the change
        NameAddress existingRespondent = null;
        if (success.getApplicationEntryId().getRnameaddress() != null) {
            existingRespondent =
                    BeanUtil.copyBean(success.getApplicationEntryId().getRnameaddress());
        }

        // if we are not expecting a respondent set to null
        if (updateEntry.getData().getRespondent() != null
                && (updateEntry.getData().getRespondent().getOrganisation() != null
                        || updateEntry.getData().getRespondent().getPerson() != null)) {
            NameAddress nameAddress =
                    auditService.processAudit(
                            AppListEntryAuditOperation.CREATE_RESPONDENT,
                            req -> {
                                NameAddress na =
                                        nameAddressRepository.save(
                                                applicantMapper.toRespondent(
                                                        updateEntry.getData().getRespondent()));
                                return Optional.of(new AuditableResult<>(na, na));
                            });
            log.debug("Assigning new respondent {}", nameAddress.getId());
            success.getApplicationEntryId().setRnameaddress(nameAddress);
        } else {
            log.debug("No respondent present. Setting respondent to null");
            success.getApplicationEntryId().setRnameaddress(null);
        }

        applicationListEntryRepository.save(success.getApplicationEntryId());
        applicationListEntryRepository.flush();

        if (existingRespondent != null) {
            auditService.processAudit(
                    existingRespondent,
                    AppListEntryAuditOperation.DELETE_RESPONDENT,
                    req -> {
                        // delete the respondent that already exists
                        nameAddressRepository.deleteForId(req.getOldValue().getId());
                        log.debug(
                                "Deleted old respondent with id: {}",
                                success.getApplicationEntryId().getId());

                        return Optional.empty();
                    });
        }
    }

    /**
     * Updates the applicant. Deletes the old applicant.
     *
     * @param updateEntry the update payload
     * @param success The success validation response
     */
    private void updateApplicant(
            PayloadForUpdateEntry updateEntry, UpdateApplicationEntryValidationSuccess success) {
        log.debug("Updating applicant");

        // capture the applicant before the change
        NameAddress existingApplicant = null;
        if (success.getApplicationEntryId().getAnamedaddress() != null) {
            existingApplicant =
                    BeanUtil.copyBean(success.getApplicationEntryId().getAnamedaddress());
        }

        if (updateEntry.getData().getApplicant() != null
                && (updateEntry.getData().getApplicant().getOrganisation() != null
                        || updateEntry.getData().getApplicant().getPerson() != null)) {

            // set the standard applicant
            success.getApplicationEntryId().setStandardApplicant(null);

            NameAddress nameAddress =
                    auditService.processAudit(
                            AppListEntryAuditOperation.CREATE_APPLICANT,
                            req -> {
                                // now add the new applicant
                                NameAddress applicant =
                                        applicantMapper.toApplicant(
                                                updateEntry.getData().getApplicant());
                                NameAddress na = nameAddressRepository.save(applicant);
                                log.debug("Assigning new applicant {}", na.getId());
                                return Optional.of(new AuditableResult<>(na, na));
                            });

            log.debug("Update applicant with id: {}", nameAddress.getId());
            success.getApplicationEntryId().setAnamedaddress(nameAddress);
        } else if (success.getSa() != null) {
            success.getApplicationEntryId().setStandardApplicant(success.getSa());
            success.getApplicationEntryId().setAnamedaddress(null);

            log.debug("No applicant present. Using standard applicant {}", success.getSa().getId());
        } else {
            log.debug("No applicant present. Setting applicant to null");
            success.getApplicationEntryId().setAnamedaddress(null);
        }

        applicationListEntryRepository.save(success.getApplicationEntryId());
        applicationListEntryRepository.flush();

        // delete the applicant that already exists
        if (existingApplicant != null) {
            auditService.processAudit(
                    existingApplicant,
                    AppListEntryAuditOperation.DELETE_APPLICANT,
                    req -> {
                        nameAddressRepository.deleteForId(req.getOldValue().getId());
                        log.debug("Deleted old applicant with id: {}", req.getOldValue().getId());
                        return Optional.empty();
                    });
        }
    }

    /**
     * Replaces the entry fee statuses when a new list is supplied. A null fee-status payload is
     * treated as no change so existing history is preserved.
     *
     * @param updateEntry The update payload
     * @param success The successful validation result
     */
    private List<AppListEntryFeeStatus> updateFeeStatus(
            PayloadForUpdateEntry updateEntry, UpdateApplicationEntryValidationSuccess success) {
        log.debug("Updating fee status");

        List<AppListEntryFeeStatus> statusList = new ArrayList<>();

        if (updateEntry.getData().getFeeStatuses() != null) {
            deleteFeeStatusesForEntry(success.getApplicationEntryId().getUuid());

            // create the fee statuses and map to entry
            for (FeeStatus feeStatus : updateEntry.getData().getFeeStatuses()) {
                auditService.processAudit(
                        AppListEntryAuditOperation.CREATE_FEE_STATUS_ENTRY,
                        req -> {
                            AppListEntryFeeStatus createdAppListStatus =
                                    appListEntryFeeStatusRepository.save(
                                            applicationListEntryEntityMapper.toFeeStatus(
                                                    feeStatus, success.getApplicationEntryId()));

                            statusList.add(createdAppListStatus);
                            log.debug(
                                    "Fee status created and "
                                            + "mapped to application "
                                            + "entry with id: {}",
                                    createdAppListStatus.getId());
                            return Optional.of(new AuditableResult<>(null, createdAppListStatus));
                        });
            }
        }

        return statusList;
    }

    /**
     * updates the fees for the entry.
     *
     * @param success The successful validation result
     */
    private void updateFees(
            UpdateApplicationEntryValidationSuccess success, PayloadForUpdateEntry updateEntry) {
        log.debug("Updating fees");
        // deletes all the fees
        List<AppListEntryFeeId> appListEntryFeeIdList =
                appListEntryFeeRepository.getEntryFeesForEntry(
                        success.getApplicationEntryId().getId());
        for (AppListEntryFeeId feeId : appListEntryFeeIdList) {
            auditService.processAudit(
                    feeId,
                    AppListEntryAuditOperation.DELETE_FEE_ENTRY,
                    req -> {
                        appListEntryFeeRepository.delete(feeId);
                        return Optional.empty();
                    });
        }

        appListEntryFeeRepository.flush();

        // if we have a fee, remove all other fees associated with the entry
        if (success.getFee() != null && success.getFee().mainFee() != null) {
            log.debug("A fee update is present for fee {}", success.getFee().mainFee().getId());

            Optional<AppListEntryFeeId> appListEntryFeeId =
                    appListEntryFeeRepository.getEntryFeesForFee(
                            success.getApplicationEntryId().getId(),
                            success.getFee().mainFee().getId());

            // if we have no fees associated then create a new one
            if (appListEntryFeeId.isEmpty()) {
                log.debug(
                        "Adding new fee {} to entry {}",
                        success.getFee().mainFee().getId(),
                        success.getApplicationEntryId().getId());

                // the main fee
                auditService.processAudit(
                        AppListEntryAuditOperation.CREATE_FEE_ENTRY,
                        req -> {
                            // create the link between the entry and the
                            // fees
                            AppListEntryFeeId newAppListEntryFeeId = new AppListEntryFeeId();
                            newAppListEntryFeeId.setAppListEntryId(
                                    success.getApplicationEntryId().getId());
                            newAppListEntryFeeId.setFeeId(success.getFee().mainFee().getId());

                            newAppListEntryFeeId =
                                    appListEntryFeeRepository.save(newAppListEntryFeeId);

                            log.debug(
                                    "Created Fee: {} to Entry: {}",
                                    newAppListEntryFeeId.getFeeId(),
                                    newAppListEntryFeeId.getAppListEntryId());

                            return Optional.of(new AuditableResult<>(null, newAppListEntryFeeId));
                        });
            }
        }

        // update the offsite
        if (success.getFee() != null
                && success.getFee().offsiteFee() != null
                && updateEntry.getData() != null
                && (updateEntry.getData().getHasOffsiteFee() != null)) {
            Optional<AppListEntryFeeId> appListEntryOffsiteFeeId =
                    appListEntryFeeRepository.getEntryFeesForFee(
                            success.getApplicationEntryId().getId(),
                            success.getFee().offsiteFee().getId());

            // add the offsite fee
            if (appListEntryOffsiteFeeId.isEmpty() && success.getFee().offsiteFee() != null) {
                log.debug(
                        "Adding new offsite fee {} to entry {}",
                        success.getFee().offsiteFee().getId(),
                        success.getApplicationEntryId().getId());
                auditService.processAudit(
                        AppListEntryAuditOperation.CREATE_FEE_ENTRY,
                        req -> {
                            AppListEntryFeeId offsiteEntryFee = new AppListEntryFeeId();
                            offsiteEntryFee.setFeeId(success.getFee().offsiteFee().getId());
                            offsiteEntryFee.setAppListEntryId(
                                    success.getApplicationEntryId().getId());
                            offsiteEntryFee = appListEntryFeeRepository.save(offsiteEntryFee);

                            log.debug(
                                    CREATED_OFFSITE_FEE_LOG,
                                    offsiteEntryFee.getFeeId(),
                                    offsiteEntryFee.getAppListEntryId());

                            return Optional.of(new AuditableResult<>(null, offsiteEntryFee));
                        });
            }
        }
    }

    /**
     * Updates the standard applicant. Deletes the old applicant.
     *
     * @param success The successful validation result
     */
    private void updateStandardApplicant(UpdateApplicationEntryValidationSuccess success) {
        if (success.getSa() != null) {
            success.getApplicationEntryId().setStandardApplicant(success.getSa());
            success.getApplicationEntryId().setAnamedaddress(null);
        }
    }

    /**
     * updates the officials for the entry.
     *
     * @param success The success validation
     * @return The update officials
     */
    private List<AppListEntryOfficial> updateOfficials(
            PayloadForUpdateEntry payload, UpdateApplicationEntryValidationSuccess success) {
        log.debug("Updating officials");

        List<AppListEntryOfficial> officials =
                appListEntryOfficialRepository.getOfficialByEntryUuid(
                        success.getApplicationEntryId().getUuid());

        // delete existing officials and audit each
        for (AppListEntryOfficial off : officials) {
            auditService.processAudit(
                    off,
                    AppListEntryAuditOperation.DELETE_OFFICIAL_ENTRY,
                    req -> {
                        log.debug("Deleting officials");

                        // delete the officials that already exist
                        appListEntryOfficialRepository.deleteAllForEntryId(
                                success.getApplicationEntryId().getId());
                        return Optional.empty();
                    });
        }

        // add officials
        List<AppListEntryOfficial> officialList = new ArrayList<>();
        if (payload.getData().getOfficials() != null) {
            // create the official for the entry
            for (Official official : payload.getData().getOfficials()) {
                auditService.processAudit(
                        AppListEntryAuditOperation.CREATE_OFFICIAL_ENTRY,
                        req -> {
                            AppListEntryOfficial createdOriginal =
                                    appListEntryOfficialRepository.save(
                                            applicationListEntryEntityMapper.toOfficial(
                                                    official, success.getApplicationEntryId()));
                            officialList.add(createdOriginal);
                            log.debug(
                                    "Original created and mapped to application "
                                            + "entry with id: {}",
                                    createdOriginal.getId());
                            return Optional.of(new AuditableResult<>(null, createdOriginal));
                        });
            }
        }

        return officialList;
    }

    private void addOfficialsForEntry(
            List<AppListEntryOfficial> officialsToCreate,
            ApplicationListEntry entry,
            List<Official> replacementOfficials) {
        for (Official official : replacementOfficials) {
            officialsToCreate.add(applicationListEntryEntityMapper.toOfficial(official, entry));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MatchResponse<EntryGetDetailDto> getApplicationListEntryDetail(
            PayloadGetEntryInList entry) {
        log.debug(
                "Started: Getting application list entry detail: {} for list: {}",
                entry.getEntryId(),
                entry.getListId());

        return getEntryValidator.validate(
                entry, (req, success) -> buildApplicationListEntryDetailResponse(entry, success));
    }

    @Override
    @Transactional(readOnly = true)
    public MatchResponse<EntryGetDetailDto> getApplicationListEntryDetailFromClosedList(
            PayloadGetEntryInList entry) {
        log.debug(
                "Started: Getting application list entry detail from closed list: {} for list: {}",
                entry.getEntryId(),
                entry.getListId());

        return getEntryFromClosedListValidator.validate(
                entry, (req, success) -> buildApplicationListEntryDetailResponse(entry, success));
    }

    private MatchResponse<EntryGetDetailDto> buildApplicationListEntryDetailResponse(
            PayloadGetEntryInList entry, GetEntryValidationSuccess success) {
        return auditService.processAudit(
                null,
                AppListEntryAuditOperation.GET_APP_ENTRY_LIST_DETAIL,
                r -> {
                    EntryGetDetailDto dto =
                            applicationListEntryMapStructMapper.toEntryGetDetailDto(
                                    success.getApplicationListEntry(),
                                    hasOffsite(success.getApplicationListEntry()));
                    log.debug(
                            "Finished: Getting application list entry detail: {} for list: {}",
                            entry.getEntryId(),
                            entry.getListId());
                    AuditableResult<MatchResponse<EntryGetDetailDto>, ApplicationListEntry> result =
                            new AuditableResult<>(
                                    MatchResponse.of(
                                            dto,
                                            getKeyablesForCreateUpdateEtag(
                                                    success.getApplicationListEntry())),
                                    applicationListEntryMapStructMapper.toApplicationListEntry(
                                            entry));
                    return Optional.of(result);
                });
    }

    @Override
    public EntryPage getApplicationListEntries(
            PayloadGetEntryInList payloadForGet,
            PagingWrapper pageable,
            EntryApplicationListGetFilterDto filterDto) {
        log.debug(
                "Started: Getting application list entries for list: {}",
                payloadForGet.getListId());

        EntryApplicationListGetFilterDto normalisedFilterDto = normaliseEntryListFilter(filterDto);

        return getApplicationListEntriesValidator.validate(
                payloadForGet,
                (req, success) ->
                        auditService.processAudit(
                                null,
                                AppListEntryAuditOperation.SEARCH_APP_ENTRY_LIST,
                                r -> {
                                    // get the entries for the list
                                    Page<ApplicationListEntryGetSummaryProjection> entries =
                                            applicationListEntryRepository.searchForGetSummary(
                                                    payloadForGet.getListId(),
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    normalisedFilterDto.getApplicantName(),
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    normalisedFilterDto.getRespondentName(),
                                                    normalisedFilterDto.getRespondentPostcode(),
                                                    normalisedFilterDto.getAccountReference(),
                                                    normalisedFilterDto.getApplicationTitle(),
                                                    normalisedFilterDto.getResulted(),
                                                    normalisedFilterDto.getFeeRequired(),
                                                    normalisedFilterDto.getSequenceNumber(),
                                                    pageable.getPageable());

                                    EntryPage entryPage = buildEntryPage(entries, pageable);

                                    if (entryPage.getContent() == null) {
                                        entryPage.setContent(List.of());
                                    }

                                    log.debug(
                                            "Finished: Getting application list entries for list: {}",
                                            payloadForGet.getListId());

                                    return Optional.of(
                                            new AuditableResult<>(
                                                    entryPage,
                                                    new ApplicationListEntryReadAudit(
                                                            applicationListEntryMapStructMapper
                                                                    .toApplicationListEntry(
                                                                            payloadForGet,
                                                                            normalisedFilterDto),
                                                            normalisedFilterDto.getResulted())));
                                }));
    }

    private EntryApplicationListGetFilterDto normaliseEntryListFilter(
            EntryApplicationListGetFilterDto filterDto) {
        EntryApplicationListGetFilterDto normalisedFilterDto =
                filterDto == null ? new EntryApplicationListGetFilterDto() : filterDto;

        normalisedFilterDto.setApplicantName(
                normaliseStringFilter(normalisedFilterDto.getApplicantName()));
        normalisedFilterDto.setRespondentName(
                normaliseStringFilter(normalisedFilterDto.getRespondentName()));
        normalisedFilterDto.setRespondentPostcode(
                normaliseStringFilter(normalisedFilterDto.getRespondentPostcode()));
        normalisedFilterDto.setAccountReference(
                normaliseStringFilter(normalisedFilterDto.getAccountReference()));
        normalisedFilterDto.setApplicationTitle(
                normaliseStringFilter(normalisedFilterDto.getApplicationTitle()));
        normalisedFilterDto.setResulted(normaliseStringFilter(normalisedFilterDto.getResulted()));

        return normalisedFilterDto;
    }

    private static String normaliseStringFilter(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    @Override
    @Transactional
    public void move(UUID sourceListId, MoveEntriesDto moveEntriesDto) {
        var payload = new MoveEntriesPayload(sourceListId, moveEntriesDto);
        final ApplicationList targetList =
                moveEntriesValidator.validate(payload, (req, success) -> success.getTargetList());

        Set<UUID> requestedIds = new HashSet<>(moveEntriesDto.getEntryIds());
        List<ApplicationListEntry> entriesToMove =
                applicationListEntryRepository.findByUuidsInSourceList(sourceListId, requestedIds);
        Set<UUID> existingIds =
                entriesToMove.stream()
                        .map(ApplicationListEntry::getUuid)
                        .collect(Collectors.toSet());

        if (existingIds.size() != requestedIds.size()) {
            Set<UUID> missingIds = new HashSet<>(requestedIds);
            missingIds.removeAll(existingIds);

            throw new AppRegistryException(
                    ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST,
                    "One or more entries were not found in the source list",
                    Map.of("invalid_entry_ids", missingIds.toString()));
        }

        List<ApplicationListEntry> orderedEntriesToMove = new ArrayList<>(entriesToMove);
        orderedEntriesToMove.sort(Comparator.comparing(ApplicationListEntry::getSequenceNumber));
        var oldAudit =
                BulkMoveApplicationListEntriesAudit.forState(
                        orderedEntriesToMove.getFirst().getApplicationList().getId(),
                        sourceListId,
                        targetList.getId(),
                        targetList.getUuid(),
                        orderedEntriesToMove);

        auditService.processAudit(
                oldAudit,
                AppListEntryAuditOperation.BULK_MOVE_APP_ENTRIES,
                req -> {
                    short nextSequence =
                            allocateNextSequence(targetList.getId(), orderedEntriesToMove.size());
                    short startingSequence =
                            (short) (nextSequence - orderedEntriesToMove.size() + 1);

                    for (int i = 0; i < orderedEntriesToMove.size(); i++) {
                        var entryToMove = orderedEntriesToMove.get(i);
                        entryToMove.setApplicationList(targetList);
                        entryToMove.setSequenceNumber((short) (startingSequence + i));
                    }

                    applicationListEntryRepository.saveAll(orderedEntriesToMove);

                    var newAudit =
                            BulkMoveApplicationListEntriesAudit.forState(
                                    oldAudit.sourceListId(),
                                    sourceListId,
                                    targetList.getId(),
                                    targetList.getUuid(),
                                    orderedEntriesToMove);

                    return Optional.of(new AuditableResult<>(null, newAudit));
                });

        log.info(
                "Completed bulk move for {} entries from list {}",
                existingIds.size(),
                sourceListId);
    }

    @Override
    @Transactional
    public void deleteEntry(PayloadForDeleteEntry idToDelete) {
        deleteApplicationListEntryValidator.validate(
                idToDelete,
                (id, success) ->
                        auditService.processAudit(
                                new DeleteAuditable(
                                        BeanUtil.copyBean(success.getApplicationListEntry())),
                                AppListEntryAuditOperation.DELETE_ENTRY,
                                req -> {
                                    success.getApplicationListEntry().setDeleted(true);
                                    applicationListEntryRepository.save(
                                            success.getApplicationListEntry());
                                    return Optional.empty();
                                }));

        log.debug(
                "Finished delete application entry {} in list {}",
                idToDelete.getEntryId(),
                idToDelete.getId());
    }

    /**
     * has an offsite fee for the entry.
     *
     * @param entry The entry
     * @return Whether we have an offsite fee
     */
    private boolean hasOffsite(ApplicationListEntry entry) {
        if (!entry.getEntryFeeIds().isEmpty()) {
            List<Long> feeIds =
                    entry.getEntryFeeIds().stream().map(AppListEntryFeeId::getFeeId).toList();

            if (!feeIds.isEmpty()) {
                List<Fee> fees =
                        feeRepository.findByIdsBetweenDate(
                                feeIds, businessDateProvider.currentUkDate());

                return fees.stream().anyMatch(Fee::isOffsite);
            }
        }
        return false;
    }

    private void deleteOffsiteFeeForEntry(ApplicationListEntry entry) {
        List<AppListEntryFeeId> appListEntryFeeIdList =
                appListEntryFeeRepository.getEntryFeesForEntry(entry.getId());
        for (AppListEntryFeeId feeId : appListEntryFeeIdList) {
            Optional<Fee> fee = feeRepository.findById(feeId.getFeeId());
            if (fee.isPresent() && fee.get().isOffsite()) {
                auditService.processAudit(
                        feeId,
                        AppListEntryAuditOperation.DELETE_FEE_ENTRY,
                        req -> {
                            appListEntryFeeRepository.delete(feeId);
                            return Optional.empty();
                        });
            }
        }
    }

    /**
     * Reloads the entity so DB-generated fields (e.g. UUID via gen_random_uuid()) are available
     * immediately after save. Calls: - flush(): force the INSERT - refresh(): reselect the row with
     * DB defaults/triggers
     */
    private ApplicationListEntry refreshEntity(ApplicationListEntry entity) {
        entityManager.flush();
        entityManager.refresh(entity);
        return entity;
    }

    /**
     * gets the keyable for the create/update entry.
     *
     * @param updateEntry The entry that was created or is being updated
     * @return The list of keyables that constitute an etag
     */
    private List<Keyable> getKeyablesForCreateUpdateEtag(ApplicationListEntry updateEntry) {
        List<AppListEntryOfficial> officialList =
                appListEntryOfficialRepository.getOfficialByEntryUuid(updateEntry.getUuid());
        List<AppListEntryFeeStatus> appListStatus =
                appListEntryFeeStatusRepository.getFeeStatusByEntryUuid(updateEntry.getUuid());
        List<Fee> feesForEntry = appListEntryFeeRepository.getFeeForEntryId(updateEntry.getId());

        // create the update etag based on the following details
        List<Keyable> keyables = new ArrayList<>();
        keyables.add(updateEntry);
        keyables.addAll(officialList);
        keyables.addAll(appListStatus);
        keyables.addAll(feesForEntry);
        return keyables;
    }

    private short allocateNextSequence(Long alId) {
        return allocateNextSequence(alId, 1);
    }

    private short allocateNextSequence(Long alId, int requiredCount) {

        AppListEntrySequenceMapping mapping =
                appListEntrySequenceMappingRepository.findByAlIdForUpdate(alId).orElse(null);

        if (mapping == null) {
            mapping =
                    AppListEntrySequenceMapping.builder()
                            .alId(alId)
                            .aleLastSequence(requiredCount)
                            .build();

            appListEntrySequenceMappingRepository.save(mapping);
            return (short) requiredCount;
        }

        int next = mapping.getAleLastSequence() + requiredCount;
        mapping.setAleLastSequence(next);

        return (short) next;
    }

    private Map<Long, List<ResultCodeGetSummaryDto>> getCodesByEntryId(
            List<ApplicationListEntryGetSummaryProjection> entries) {

        List<Long> entryIds =
                entries.stream().map(ApplicationListEntryGetSummaryProjection::getId).toList();

        if (entryIds.isEmpty()) {
            return Map.of();
        }

        List<ApplicationListEntryResolutionProjection> resolutionProjections =
                applicationListEntryRepository.findResolutionCodesByEntryIds(entryIds);

        return resolutionProjections.stream()
                .collect(
                        Collectors.groupingBy(
                                ApplicationListEntryResolutionProjection::getEntryId,
                                Collectors.mapping(
                                        projection ->
                                                applicationListEntryMapStructMapper
                                                        .toResultCodeGetSummaryDto(
                                                                projection.getResolutionCode()),
                                        Collectors.toList())));
    }

    private List<EntryGetSummaryDto> buildEntrySummaries(
            List<ApplicationListEntryGetSummaryProjection> entries) {

        Map<Long, List<ResultCodeGetSummaryDto>> codesByEntryId = getCodesByEntryId(entries);

        return entries.stream()
                .map(
                        entry -> {
                            EntryGetSummaryDto entrySummary =
                                    applicationListEntryMapStructMapper.toEntrySummary(entry);

                            List<ResultCodeGetSummaryDto> resultCodes =
                                    codesByEntryId.getOrDefault(entry.getId(), List.of());

                            entrySummary.setResulted(resultCodes);
                            entrySummary.setIsResulted(!resultCodes.isEmpty());

                            return entrySummary;
                        })
                .toList();
    }

    private List<EntryGetSummaryDto> orderBulkEntrySummaries(
            List<EntryGetSummaryDto> entries, List<UUID> listIds, List<UUID> entryIds) {
        if (entryIds != null && !entryIds.isEmpty()) {
            Map<UUID, Integer> entryOrder = buildUuidOrder(entryIds);
            return entries.stream()
                    .sorted(Comparator.comparing(entry -> entryOrder.get(entry.getId())))
                    .toList();
        }

        Map<UUID, Integer> listOrder = buildUuidOrder(listIds);
        return entries.stream()
                .sorted(
                        Comparator.comparing(
                                        (EntryGetSummaryDto entry) ->
                                                listOrder.get(entry.getListId()))
                                .thenComparing(EntryGetSummaryDto::getSequenceNumber))
                .toList();
    }

    private static Map<UUID, Integer> buildUuidOrder(List<UUID> ids) {
        return IntStream.range(0, ids.size())
                .boxed()
                .collect(
                        Collectors.toMap(
                                ids::get, index -> index, (left, right) -> left, HashMap::new));
    }

    private EntryPage buildEntryPage(
            Page<ApplicationListEntryGetSummaryProjection> resultPage, PagingWrapper pageable) {

        EntryPage entryPage = new EntryPage();
        pageMapper.toPage(resultPage, entryPage, pageable.getSortStrings());
        entryPage.setContent(buildEntrySummaries(resultPage.getContent()));

        return entryPage;
    }

    private record BulkActionPreviewResolution(
            int selectedCount, List<UUID> entryIds, List<EntryGetSummaryDto> entries) {}

    private record BulkActionPreviewEligibility(
            List<UUID> entryIds, List<EntryGetSummaryDto> entries, int eligibleCount) {}
}
