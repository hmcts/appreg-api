package uk.gov.hmcts.appregister.applicationlist.service;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.applicationlist.audit.AppListAuditOperation;
import uk.gov.hmcts.appregister.applicationlist.mapper.ApplicationListMapper;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationCreateListLocationValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationListDeletionValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationListGetValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationUpdateListLocationValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ListLocationValidationSuccess;
import uk.gov.hmcts.appregister.applicationlist.validator.ListUpdateValidationSuccess;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.concurrency.MatchService;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.base.EntryCount;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForUpdate;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;

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

    private final ApplicationListRepository repository;
    private final ApplicationListEntryRepository aleRepository;
    private final ApplicationListMapper mapper;
    private final ApplicationCreateListLocationValidator applicationCreateListLocationValidator;
    private final ApplicationUpdateListLocationValidator applicationUpdateListLocationValidator;
    private final ApplicationListGetValidator applicationListGetValidator;
    private final EntityManager entityManager;
    private final MatchService matchService;

    private final PageMapper pageMapper;
    private final ApplicationListDeletionValidator deletionValidator;
    private final AuditOperationService auditService;
    private final List<AuditOperationLifecycleListener> auditLifecycleListeners;

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
        log.debug("Start: Request to create application list : {}", dto);

        return auditService.processAudit(
                Optional.empty(),
                AppListAuditOperation.CREATE_APP_LIST,
                req ->
                        applicationCreateListLocationValidator.validate(
                                dto,
                                (listCreateDto, success) ->
                                        success.hasCourt()
                                                ? Optional.of(
                                                        createWithCourt(listCreateDto, success))
                                                : Optional.of(
                                                        createWithCja(listCreateDto, success))),
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
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
        log.debug("Start: Request to update application list : {}", dto);

        MatchResponse<ApplicationListGetDetailDto> response =
                applicationUpdateListLocationValidator.validate(
                        dto,
                        (updateDto, success) ->
                                auditService.processAudit(
                                        success.getCopyOfApplicationList(),
                                        AppListAuditOperation.UPDATE_APP_LIST,
                                        (evnt) -> {
                                            return success.hasCourt()
                                                    ? Optional.of(
                                                            updateWithCourt(updateDto, success))
                                                    : Optional.of(
                                                            updateWithCja(updateDto, success));
                                        },
                                        auditLifecycleListeners.toArray(
                                                new AuditOperationLifecycleListener[0])));

        log.debug("Finish: Request to update application list : {}", response.getPayload());
        return response;
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

        return new AuditableResult<MatchResponse<ApplicationListGetDetailDto>, ApplicationList>(
                MatchResponse.of(
                        hydrated.getUuid(), hydrated, mapper.toGetDetailDto(hydrated, null)),
                Optional.of(hydrated));
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

        return new AuditableResult<MatchResponse<ApplicationListGetDetailDto>, ApplicationList>(
                MatchResponse.of(
                        hydrated.getUuid(), hydrated, mapper.toGetDetailDto(hydrated, cja)),
                Optional.of(hydrated));
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

        return new AuditableResult<MatchResponse<ApplicationListGetDetailDto>, ApplicationList>(
                matchService.matchOnRequest(
                        success.getApplicationList().getUuid(),
                        success.getApplicationList(),
                        () -> {
                            var savedEntity = repository.save(success.getApplicationList());
                            var hydrated = refreshEntity(savedEntity);
                            return MatchResponse.of(
                                    hydrated.getUuid(),
                                    hydrated,
                                    mapper.toGetDetailDto(hydrated, null));
                        }),
                Optional.of(success.getApplicationList()));
    }

    /**
     * Update an Application List associated with a Criminal Justice Area.
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

        return new AuditableResult<MatchResponse<ApplicationListGetDetailDto>, ApplicationList>(
                matchService.matchOnRequest(
                        success.getApplicationList().getUuid(),
                        success.getApplicationList(),
                        () -> {
                            var savedEntity = repository.save(applicationList);
                            var hydrated = refreshEntity(savedEntity);

                            return MatchResponse.of(
                                    hydrated.getUuid(),
                                    hydrated,
                                    mapper.toGetDetailDto(hydrated, cja));
                        }),
                Optional.of(applicationList));
    }

    @Override
    @Transactional
    public void delete(UUID idToDelete) {
        log.debug("Start: Deleting Application List with id: {}", idToDelete);
        deletionValidator.validate(idToDelete);
        Optional<ApplicationList> applicationList = repository.findByUuid(idToDelete);

        if (applicationList.isPresent()) {
            applicationList.get().setDeleted(true);
            repository.save(applicationList.get());
        }
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
    @Transactional(readOnly = true)
    @Override
    public ApplicationListPage getPage(ApplicationListGetFilterDto dto, Pageable pageable) {
        return applicationListGetValidator.validateCja(
                dto,
                (getDto, success) -> {
                    final Page<ApplicationList> dbPage =
                            repository.findAllByFilter(
                                    dto.getStatus(),
                                    dto.getCourtLocationCode(),
                                    success.getCriminalJusticeArea(),
                                    dto.getDate(),
                                    dto.getTime(),
                                    dto.getDescription(),
                                    dto.getOtherLocationDescription(),
                                    pageable);

                    // Pre-fetch the number of entries linked to each list in the page.
                    // Avoids having to do a separate count query per list when mapping to DTOs.
                    Map<UUID, Long> entriesPerListCounter =
                            dbPage.isEmpty()
                                    ? Map.of()
                                    : fetchEntryCounts(
                                            dbPage.map(ApplicationList::getUuid).toList());

                    return assembleResponsePage(dbPage, entriesPerListCounter);
                },
                true);
    }

    private Map<UUID, Long> fetchEntryCounts(List<UUID> uuids) {
        return aleRepository.countByApplicationListUuids(uuids).stream()
                .collect(
                        Collectors.toMap(
                                EntryCount::getPrimaryKey,
                                ec -> ec.getCount() == null ? ZERO_ENTITIES : ec.getCount()));
    }

    private ApplicationListPage assembleResponsePage(
            Page<ApplicationList> appLists, Map<UUID, Long> entriesPerListCounter) {
        var responsePage = new ApplicationListPage();
        pageMapper.toPage(appLists, responsePage);

        // Ensure content is never null:
        // API spec requires an array, so return an empty one instead of null.
        if (responsePage.getContent() == null) {
            responsePage.setContent(new ArrayList<>());
        }

        for (ApplicationList al : appLists) {
            long entryCount = entriesPerListCounter.getOrDefault(al.getUuid(), ZERO_ENTITIES);
            String location = deriveLocation(al);
            responsePage.addContentItem(mapper.toGetSummaryDto(al, entryCount, location));
        }
        return responsePage;
    }

    private String deriveLocation(ApplicationList al) {
        if (al.getCourtName() != null) {
            return al.getCourtName();
        }
        if (al.getCja() != null) {
            return al.getCja().getDescription();
        }
        return "Location not set";
    }
}
