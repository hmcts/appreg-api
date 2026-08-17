package uk.gov.hmcts.appregister.applicationlist.service;

import jakarta.persistence.EntityManager;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.applicationentry.audit.BulkApplicationListEntriesReadAudit;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapper;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkGetApplicationListEntriesValidator;
import uk.gov.hmcts.appregister.applicationlist.audit.AppListAuditOperation;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.applicationlist.mapper.ApplicationListMapper;
import uk.gov.hmcts.appregister.applicationlist.mapper.ApplicationListOfficialMapper;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationCreateListLocationValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationListDeletionValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationListGetValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationUpdateListLocationValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ListLocationValidationSuccess;
import uk.gov.hmcts.appregister.applicationlist.validator.ListUpdateValidationSuccess;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.concurrency.MatchService;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry_;
import uk.gov.hmcts.appregister.common.entity.base.EntryCount;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryOfficialRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.enumeration.OfficialType;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForUpdate;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryOfficialPrintProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryPrintProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryResolutionPrintProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListSummaryProjection;
import uk.gov.hmcts.appregister.common.util.BeanUtil;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;
import uk.gov.hmcts.appregister.generated.model.BulkGetApplicationListEntriesRequestDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.Official;

/**
 * Service implementation for managing Application Lists.
 *
 * <p>Handles persistence, validation, and entity-to-DTO mapping logic. Responsibilities:
 *
 * <ul>
 *   <li>Validate input data before persistence.
 *   <li>Persist application lists associated with a Court or Criminal Justice Area.
 *   <li>Handle duplicate and not-found scenarios gracefully via {@link AppRegistryException}.
 *   <li>Map between entities and DTOs using {@link ApplicationListMapper}.
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ApplicationListServiceImpl implements ApplicationListService {
    private static final long ZERO_ENTITIES = 0L;
    private static final UUID PRINT_PLACEHOLDER_ENTRY_ID = new UUID(0L, 0L);
    private static final List<OfficialType> PRINTABLE_OFFICIAL_TYPES =
            List.of(OfficialType.MAGISTRATE, OfficialType.CLERK);

    // Repositories
    private final ApplicationListRepository repository;
    private final ApplicationListEntryRepository aleRepository;
    private final AppListEntryResolutionRepository alerRepository;
    private final ApplicationListEntryOfficialRepository aleoRepository;

    // Mappers
    private final ApplicationListMapper mapper;
    // Mapper for transferring Spring Data {@link Page} metadata into API page objects.
    private final ApplicationListEntryMapper entryMapper;
    private final ApplicationListOfficialMapper officalMapper; // (see rename suggestion below)
    private final PageMapper pageMapper;

    // Validators
    private final ApplicationCreateListLocationValidator applicationCreateListLocationValidator;
    private final ApplicationUpdateListLocationValidator applicationUpdateListLocationValidator;
    private final ApplicationListGetValidator applicationListGetValidator;
    private final ApplicationListDeletionValidator deletionValidator;
    private final BulkGetApplicationListEntriesValidator bulkGetApplicationListEntriesValidator;

    // Services
    private final MatchService matchService;

    // Infrastructure
    private final EntityManager entityManager;

    // Audit
    private final AuditOperationService auditService;

    private record TimeWindow(LocalTime start, LocalTime end, Boolean wrapsMidnight) {}

    /**
     * The default internal application entry summary page. This guarantees a stable set of
     * summaries.
     */
    public static final Pageable ENTRY_SUMMARY_SORT =
            Pageable.unpaged(Sort.by(Sort.Direction.ASC, ApplicationListEntry_.ID));

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to either {@link #createWithCourt(ApplicationListCreateDto,
     * ListLocationValidationSuccess)} or {@link #createWithCja(ApplicationListCreateDto,
     * ListLocationValidationSuccess)} depending on whether a Court Location Code is present in the
     * DTO.
     *
     * @throws AppRegistryException if no court or multiple courts are found for the given code
     */
    @Override
    @Transactional
    public MatchResponse<ApplicationListGetDetailDto> create(ApplicationListCreateDto dto) {
        log.debug("Started create application list");

        return auditService.processAudit(
                AppListAuditOperation.CREATE_APP_LIST,
                req ->
                        applicationCreateListLocationValidator.validate(
                                dto,
                                (listCreateDto, success) ->
                                        success.hasCourt()
                                                ? Optional.of(
                                                        createWithCourt(listCreateDto, success))
                                                : Optional.of(
                                                        createWithCja(listCreateDto, success))));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to either {@link #updateWithCourt(PayloadForUpdate,
     * ListUpdateValidationSuccess)} or {@link #updateWithCja(PayloadForUpdate,
     * ListUpdateValidationSuccess)} depending on whether a Court Location Code is present in the
     * DTO.
     */
    @Override
    @Transactional
    public MatchResponse<ApplicationListGetDetailDto> update(
            PayloadForUpdate<ApplicationListUpdateDto> dto) {
        log.debug("Started update application list {}", dto.getId());

        MatchResponse<ApplicationListGetDetailDto> response =
                applicationUpdateListLocationValidator.validate(
                        dto,
                        (updateDto, success) ->
                                auditService.processAudit(
                                        BeanUtil.copyBean(success.getApplicationList()),
                                        AppListAuditOperation.UPDATE_APP_LIST,
                                        evnt ->
                                                success.hasCourt()
                                                        ? Optional.of(
                                                                updateWithCourt(updateDto, success))
                                                        : Optional.of(
                                                                updateWithCja(
                                                                        updateDto, success))));

        log.debug("Finished update application list {}", dto.getId());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationListGetDetailDto get(UUID id, PagingWrapper pageable) {

        return auditService.processAudit(
                null,
                AppListAuditOperation.GET_APP_LIST,
                req -> {
                    ApplicationList list = findApplicationListOrThrow(id);
                    AuditableResult<ApplicationListGetDetailDto, ApplicationList> result =
                            new AuditableResult<>(getListDetailDto(list), mapper.toEntity(id));
                    return Optional.of(result);
                });
    }

    /**
     * gets the list detail without a transaction. This method should be called by a method that has
     * already established a transaction
     *
     * @param list The application list entity
     */
    private ApplicationListGetDetailDto getListDetailDto(ApplicationList list) {
        UUID id = list.getUuid();
        Long entryCount = fetchEntryCounts(List.of(id)).getOrDefault(id, ZERO_ENTITIES);
        return mapper.toGetDetailDto(list, list.getCja(), entryCount);
    }

    private ApplicationList findApplicationListOrThrow(UUID id) {
        return repository
                .findByUuid(id)
                .orElseThrow(
                        () ->
                                new AppRegistryException(
                                        ApplicationListError.LIST_NOT_FOUND,
                                        "No application list found for UUID '%s'".formatted(id)));
    }

    /**
     * Creates an Application List associated with a Court.
     *
     * <p>Validates that exactly one active court exists for the provided code. If multiple or none
     * exist, an exception is thrown. Otherwise, the list is persisted and returned as a DTO.
     *
     * @param createDto the DTO containing court-based application list details
     * @param success The validation validated details
     * @return the created Application List DTO
     */
    private AuditableResult<MatchResponse<ApplicationListGetDetailDto>, ApplicationList>
            createWithCourt(
                    ApplicationListCreateDto createDto, ListLocationValidationSuccess success) {
        var court = success.getNationalCourtHouse();
        var savedEntity = repository.save(mapper.toCreateEntityWithCourt(createDto, court));
        var hydrated = refreshEntity(savedEntity);

        return new AuditableResult<>(
                MatchResponse.of(
                        mapper.toGetDetailDto(hydrated, null, ZERO_ENTITIES), List.of(hydrated)),
                hydrated);
    }

    /**
     * Creates an Application List associated with a Criminal Justice Area.
     *
     * <p>Validates that exactly one CJA exists for the provided code. If multiple or none exist, an
     * exception is thrown. Otherwise, the list is persisted and returned as a DTO.
     *
     * @param createDto the DTO containing CJA-based application list details
     * @param success The validation validated details
     * @return the created Application List DTO
     */
    private AuditableResult<MatchResponse<ApplicationListGetDetailDto>, ApplicationList>
            createWithCja(
                    ApplicationListCreateDto createDto, ListLocationValidationSuccess success) {
        var cja = success.getCriminalJusticeArea();

        var savedEntity = repository.save(mapper.toCreateEntityWithCja(createDto, cja));
        var hydrated = refreshEntity(savedEntity);

        // gets the summaries for the unpaged summaries.
        return new AuditableResult<>(
                MatchResponse.of(
                        mapper.toGetDetailDto(hydrated, cja, ZERO_ENTITIES), List.of(hydrated)),
                hydrated);
    }

    /**
     * Update an Application List associated with a Court.
     *
     * @param updateDto the DTO containing court-based application list details
     * @param success The validation validated details
     * @return the created Application List DTO
     */
    private AuditableResult<MatchResponse<ApplicationListGetDetailDto>, ApplicationList>
            updateWithCourt(
                    PayloadForUpdate<ApplicationListUpdateDto> updateDto,
                    ListUpdateValidationSuccess success) {
        var court = success.getNationalCourtHouse();

        mapper.toUpdateEntityWithCourt(
                updateDto.getData(), null, court, success.getApplicationList());

        return new AuditableResult<>(
                matchService.matchOnRequest(
                        () -> {
                            var savedEntity = repository.save(success.getApplicationList());
                            var hydrated = refreshEntity(savedEntity);
                            return MatchResponse.of(
                                    mapper.toGetDetailDto(
                                            hydrated,
                                            null,
                                            fetchEntryCounts(List.of(hydrated.getUuid()))
                                                    .getOrDefault(
                                                            hydrated.getUuid(), ZERO_ENTITIES)),
                                    List.of(hydrated));
                        },
                        List.of(success.getApplicationList())),
                success.getApplicationList());
    }

    /**
     * - an Application List associated with a Criminal Justice Area.
     *
     * <p>Validates that exactly one CJA exists for the provided code. If multiple or none exist, an
     * exception is thrown. Otherwise, the list is persisted and returned as a DTO.
     *
     * @param updateDto the DTO containing CJA-based application list details
     * @param success The validation validated details
     * @return the created Application List DTO
     */
    private AuditableResult<MatchResponse<ApplicationListGetDetailDto>, ApplicationList>
            updateWithCja(
                    PayloadForUpdate<ApplicationListUpdateDto> updateDto,
                    ListUpdateValidationSuccess success) {
        var cja = success.getCriminalJusticeArea();
        ApplicationList applicationList = success.getApplicationList();
        mapper.toUpdateEntityWithCja(updateDto.getData(), cja, applicationList);

        return new AuditableResult<>(
                matchService.matchOnRequest(
                        () -> {
                            var savedEntity = repository.save(applicationList);
                            var hydrated = refreshEntity(savedEntity);

                            return MatchResponse.of(
                                    mapper.toGetDetailDto(
                                            hydrated,
                                            cja,
                                            fetchEntryCounts(List.of(hydrated.getUuid()))
                                                    .getOrDefault(
                                                            hydrated.getUuid(), ZERO_ENTITIES)),
                                    List.of(hydrated));
                        },
                        List.of(success.getApplicationList())),
                success.getApplicationList());
    }

    @Override
    @Transactional
    public void delete(UUID idToDelete) {
        log.debug("Start: Deleting Application List with id: {}", idToDelete);

        deletionValidator.validate(
                idToDelete,
                (id, success) ->
                        auditService.processAudit(
                                BeanUtil.copyBean(success.getApplicationList()),
                                AppListAuditOperation.DELETE_APP_LIST,
                                req -> {
                                    performDelete(success.getApplicationList());
                                    return Optional.empty();
                                }));

        log.debug("Finish: Deleted Application List with id: {}", idToDelete);
    }

    /**
     * Reloads the entity so DB-generated fields (e.g. UUID via gen_random_uuid()) are available
     * immediately after save. Calls: - flush(): force the INSERT - refresh(): reselect the row with
     * DB defaults/triggers
     */
    private ApplicationList refreshEntity(ApplicationList entity) {
        entityManager.flush();
        entityManager.refresh(entity);
        return entity;
    }

    private ApplicationListGetPrintDto buildGetPrintDto(
            ApplicationList list, List<EntryGetPrintDto> entries) {
        ApplicationListGetPrintDto dto = mapper.toGetPrintDto(list);
        dto.setEntries(entries);

        return dto;
    }

    /**
     * Retrieves a paginated list of application lists based on the given filter and paging
     * parameters.
     *
     * <p>Resolves and normalizes input filters (including CJA lookup and date/time normalization),
     * queries the repository for matching records, retrieves associated entry counts, and maps the
     * results into an {@link ApplicationListPage} containing summary DTOs.
     *
     * @param dto the filter criteria used to select application lists
     * @param pageable pagination and sorting information
     * @return a populated {@link ApplicationListPage} with metadata and summary items
     */
    @Override
    public ApplicationListPage getPage(ApplicationListGetFilterDto dto, PagingWrapper pageable) {
        TimeWindow timeWindow = computeTimeWindow(dto);

        return auditService.processAudit(
                null,
                AppListAuditOperation.GET_APP_LIST,
                req ->
                        applicationListGetValidator.validateCja(
                                dto,
                                (getDto, success) -> {
                                    final Page<ApplicationListSummaryProjection> dbPage =
                                            repository.findAllByFilter(
                                                    entryMapper.toStatus(dto.getStatus()),
                                                    dto.getCourtLocationCode(),
                                                    success.getCriminalJusticeArea(),
                                                    dto.getDate(),
                                                    timeWindow.start,
                                                    timeWindow.end,
                                                    timeWindow.wrapsMidnight,
                                                    dto.getDescription(),
                                                    dto.getOtherLocationDescription(),
                                                    pageable.getPageable());

                                    AuditableResult<ApplicationListPage, ApplicationList> result =
                                            new AuditableResult<>(
                                                    assembleResponsePage(dbPage, pageable),
                                                    mapper.toEntity(dto));
                                    return Optional.of(result);
                                },
                                true));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationListGetPrintDto> print(BulkGetApplicationListEntriesRequestDto request) {
        bulkGetApplicationListEntriesValidator.validate(request);

        return auditService.processAudit(
                null,
                AppListAuditOperation.PRINT_APP_LIST,
                req ->
                        Optional.of(
                                new AuditableResult<>(
                                        buildGetPrintDtos(
                                                repository.findByUuidIn(request.getListIds()),
                                                request.getListIds(),
                                                request.getEntryIds()),
                                        new BulkApplicationListEntriesReadAudit(
                                                request.getListIds(), request.getEntryIds()))));
    }

    private List<ApplicationListGetPrintDto> buildGetPrintDtos(
            List<ApplicationList> lists, List<UUID> listIds, List<UUID> entryIds) {
        var hasEntryIds = entryIds != null && !entryIds.isEmpty();
        var entryProjections =
                aleRepository.findByApplicationListIdsForPrinting(
                        listIds,
                        hasEntryIds,
                        hasEntryIds ? entryIds : List.of(PRINT_PLACEHOLDER_ENTRY_ID));

        Map<Long, List<String>> resolvedWordingsByEntryId = Map.of();
        Map<Long, List<Official>> resolvedOfficialsByEntryId = Map.of();

        if (!entryProjections.isEmpty()) {
            var entryIdsForLookup =
                    entryProjections.stream()
                            .map(ApplicationListEntryPrintProjection::getId)
                            .toList();
            resolvedWordingsByEntryId =
                    alerRepository
                            .findByApplicationListEntryIdsForPrinting(entryIdsForLookup)
                            .stream()
                            .collect(
                                    Collectors.groupingBy(
                                            ApplicationListEntryResolutionPrintProjection
                                                    ::getEntryId,
                                            Collectors.mapping(
                                                    ApplicationListEntryResolutionPrintProjection
                                                            ::getWording,
                                                    Collectors.toList())));
            resolvedOfficialsByEntryId =
                    aleoRepository
                            .findByApplicationListEntryIdsForPrinting(
                                    entryIdsForLookup, PRINTABLE_OFFICIAL_TYPES)
                            .stream()
                            .collect(
                                    Collectors.groupingBy(
                                            ApplicationListEntryOfficialPrintProjection::getEntryId,
                                            Collectors.mapping(
                                                    officalMapper::toOfficialDto,
                                                    Collectors.toList())));
        }

        var wordingsByEntryId = resolvedWordingsByEntryId;
        var officialsByEntryId = resolvedOfficialsByEntryId;

        var listById =
                lists.stream().collect(Collectors.toMap(ApplicationList::getUuid, list -> list));
        var entryProjectionsByListId =
                entryProjections.stream()
                        .collect(
                                Collectors.groupingBy(
                                        ApplicationListEntryPrintProjection::getListId,
                                        Collectors.toList()));
        var entryOrder = hasEntryIds ? buildUuidOrder(entryIds) : Map.<UUID, Integer>of();

        return listIds.stream()
                .map(
                        listId ->
                                buildGetPrintDto(
                                        listById.get(listId),
                                        buildEntryPrintDtos(
                                                entryProjectionsByListId.getOrDefault(
                                                        listId, List.of()),
                                                entryOrder,
                                                wordingsByEntryId,
                                                officialsByEntryId)))
                .toList();
    }

    private List<EntryGetPrintDto> buildEntryPrintDtos(
            List<ApplicationListEntryPrintProjection> entryProjections,
            Map<UUID, Integer> entryOrder,
            Map<Long, List<String>> wordingsByEntryId,
            Map<Long, List<Official>> officialsByEntryId) {
        return entryProjections.stream()
                .sorted(printEntryComparator(entryOrder))
                .map(
                        entryProjection ->
                                buildEntryPrintDto(
                                        entryProjection, wordingsByEntryId, officialsByEntryId))
                .toList();
    }

    private EntryGetPrintDto buildEntryPrintDto(
            ApplicationListEntryPrintProjection entryProjection,
            Map<Long, List<String>> wordingsByEntryId,
            Map<Long, List<Official>> officialsByEntryId) {
        var entryId = entryProjection.getId();
        var dto = entryMapper.toPrintDto(entryProjection);
        dto.setResultWordings(wordingsByEntryId.getOrDefault(entryId, List.of()));
        dto.setOfficials(officialsByEntryId.getOrDefault(entryId, List.of()));
        return dto;
    }

    private static Comparator<ApplicationListEntryPrintProjection> printEntryComparator(
            Map<UUID, Integer> entryOrder) {
        if (entryOrder.isEmpty()) {
            return Comparator.comparingInt(ApplicationListEntryPrintProjection::getSequenceNumber);
        }

        return Comparator.comparingInt(
                entryProjection -> entryOrder.get(entryProjection.getUuid()));
    }

    private static Map<UUID, Integer> buildUuidOrder(List<UUID> ids) {
        var order = new HashMap<UUID, Integer>();
        for (int index = 0; index < ids.size(); index++) {
            order.put(ids.get(index), index);
        }
        return order;
    }

    private Map<UUID, Long> fetchEntryCounts(List<UUID> uuids) {
        return aleRepository.countByApplicationListUuids(uuids).stream()
                .collect(
                        Collectors.toMap(
                                EntryCount::getPrimaryKey,
                                ec -> ec.getCount() == null ? ZERO_ENTITIES : ec.getCount()));
    }

    private ApplicationListPage assembleResponsePage(
            Page<ApplicationListSummaryProjection> appLists, PagingWrapper pagingWrapper) {
        var responsePage = new ApplicationListPage();
        pageMapper.toPage(appLists, responsePage, pagingWrapper.getSortStrings());

        // Ensure content is never null:
        // API spec requires an array, so return an empty one instead of null.
        if (responsePage.getContent() == null) {
            responsePage.setContent(new ArrayList<>());
        }

        for (ApplicationListSummaryProjection alp : appLists) {
            String location = deriveLocation(alp);
            responsePage.addContentItem(mapper.toGetSummaryDto(alp, alp.getEntryCount(), location));
        }

        return responsePage;
    }

    private static String deriveLocation(ApplicationListSummaryProjection al) {
        if (al.getCourtName() != null) {
            return al.getCourtName();
        }
        if (al.getCjaDescription() != null) {
            return al.getCjaDescription();
        }
        return "Location not set";
    }

    /* Convert a user-supplied "HH:mm" time into a minute-range.
    The repository uses [start, end] to match all seconds within that minute.
    When the computed end value wraps to midnight (e.g., 23:59 -> 00:00),
    we record this so the repository can handle the boundary correctly. */
    private static TimeWindow computeTimeWindow(ApplicationListGetFilterDto dto) {
        if (dto.getTime() != null) {
            LocalTime start = dto.getTime().withSecond(0).withNano(0);
            LocalTime end = start.plusMinutes(1L);
            boolean wrapsMidnight = end.equals(LocalTime.MIDNIGHT);

            return new TimeWindow(start, end, wrapsMidnight);
        }

        return new TimeWindow(null, null, false);
    }

    /**
     * Delete an Application List.
     *
     * @param applicationList the application list entity to delete
     * @return an AuditableResult containing a MatchResponse with the soft-deleted ApplicationList
     */
    private AuditableResult<MatchResponse<Void>, ApplicationList> performDelete(
            ApplicationList applicationList) {

        // mark entity as soft-deleted
        applicationList.setDeleted(true);

        return new AuditableResult<>(
                matchService.matchOnRequest(
                        () -> {
                            repository.save(applicationList);

                            return MatchResponse.of(null, List.of());
                        },
                        List.of(applicationList)),
                applicationList);
    }
}
