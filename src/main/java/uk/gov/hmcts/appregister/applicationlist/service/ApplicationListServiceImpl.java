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
import org.springframework.util.StringUtils;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapStructMapper;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.applicationlist.mapper.ApplicationListMapper;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationListDeletionValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationListLocationValidator;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.base.EntryCount;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryOfficialRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryPrintProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntrySummaryProjection;
import uk.gov.hmcts.appregister.common.service.LocationLookupService;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.generated.model.EntryGetPrintDto;

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
@RequiredArgsConstructor
@Service
@Slf4j
public class ApplicationListServiceImpl implements ApplicationListService {

    private static final int SINGLE_RECORD = 1;
    private static final Long ZERO_ENTITIES = 0L;

    private final ApplicationListRepository repository;
    private final ApplicationListEntryRepository aleRepository;
    private final AppListEntryResolutionRepository alerRepository;
    private final ApplicationListEntryOfficialRepository aleoRepository;
    private final ApplicationListMapper mapper;
    // Mapper for transferring Spring Data {@link Page} metadata into API page objects.
    private final ApplicationListEntryMapStructMapper entryMapper;
    private final ApplicationListLocationValidator validator;
    private final EntityManager entityManager;
    private final PageMapper pageMapper;
    private final LocationLookupService locationLookupService;
    private final ApplicationListDeletionValidator deletionValidator;

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to either {@link #createWithCourt(ApplicationListCreateDto)} or {@link
     * #createWithCja(ApplicationListCreateDto)} depending on whether a Court Location Code is
     * present in the DTO.
     */
    @Override
    @Transactional
    public ApplicationListGetDetailDto create(ApplicationListCreateDto dto) {
        validator.validate(dto);
        return hasCourt(dto) ? createWithCourt(dto) : createWithCja(dto);
    }

    @Override
    @Transactional
    public ApplicationListGetDetailDto get(UUID id, Pageable pageable) {
        ApplicationList list =
                repository
                        .findByUuid(id)
                        .orElseThrow(
                                () ->
                                        new AppRegistryException(
                                                ApplicationListError.LIST_NOT_FOUND,
                                                "No application list found for UUID '%s'"
                                                        .formatted(id)));

        // Fetch results from the repository using pagination
        Page<ApplicationListEntrySummaryProjection> dbPage =
                aleRepository.findSummariesById(id, pageable);

        List<ApplicationListEntrySummary> summaries = new ArrayList<>();

        // Map each projection to a summary model
        dbPage.forEach(projection -> summaries.add(entryMapper.toSummaryDto(projection)));

        // Fetch the number of entries linked to this list.
        // Avoids running a separate count query later when mapping to a DTO.
        Long entryCount = fetchEntryCounts(List.of(id)).getOrDefault(id, ZERO_ENTITIES);

        return buildGetDetailDto(list, entryCount, summaries);
    }

    private static boolean hasCourt(ApplicationListCreateDto dto) {
        return StringUtils.hasText(dto.getCourtLocationCode());
    }

    /**
     * Creates an Application List associated with a Court.
     *
     * <p>Validates that exactly one active court exists for the provided code. If multiple or none
     * exist, an exception is thrown. Otherwise, the list is persisted and returned as a DTO.
     *
     * @param dto the DTO containing court-based application list details
     * @return the created Application List DTO
     * @throws AppRegistryException if no court or multiple courts are found for the given code
     */
    private ApplicationListGetDetailDto createWithCourt(ApplicationListCreateDto dto) {
        var court = locationLookupService.getActiveCourtOrThrow(dto.getCourtLocationCode());
        var savedEntity = repository.save(mapper.toCreateEntityWithCourt(dto, court));
        var hydratedEntity = refreshEntity(savedEntity);
        return mapper.toGetDetailDto(hydratedEntity, null, ZERO_ENTITIES);
    }

    /**
     * Creates an Application List associated with a Criminal Justice Area.
     *
     * <p>Validates that exactly one CJA exists for the provided code. If multiple or none exist, an
     * exception is thrown. Otherwise, the list is persisted and returned as a DTO.
     *
     * @param dto the DTO containing CJA-based application list details
     * @return the created Application List DTO
     * @throws AppRegistryException if no CJA or multiple CJAs are found for the given code
     */
    private ApplicationListGetDetailDto createWithCja(ApplicationListCreateDto dto) {
        var cja = locationLookupService.getCjaOrThrow(dto.getCjaCode());
        var savedEntity = repository.save(mapper.toCreateEntityWithCja(dto, cja));
        var hydratedEntity = refreshEntity(savedEntity);
        return mapper.toGetDetailDto(hydratedEntity, cja, ZERO_ENTITIES);
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

    private ApplicationListGetDetailDto buildGetDetailDto(
            ApplicationList list,
            Long entriesCount,
            List<ApplicationListEntrySummary> entriesSummary) {
        ApplicationListGetDetailDto dto = mapper.toGetDetailDto(list, null, entriesCount);
        dto.setEntriesSummary(entriesSummary);

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
    @Transactional(readOnly = true)
    @Override
    public ApplicationListPage getPage(ApplicationListGetFilterDto dto, Pageable pageable) {

        CriminalJusticeArea cja = resolveCja(dto.getCjaCode()).orElse(null);

        final Page<ApplicationList> dbPage =
                repository.findAllByFilter(
                        dto.getStatus(),
                        dto.getCourtLocationCode(),
                        cja,
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
                        : fetchEntryCounts(dbPage.map(ApplicationList::getUuid).toList());

        return assembleResponsePage(dbPage, entriesPerListCounter);
    }

    @Override
    @Transactional
    public ApplicationListGetPrintDto print(UUID id) {
        ApplicationList list =
                repository
                        .findByUuid(id)
                        .orElseThrow(
                                () ->
                                        new AppRegistryException(
                                                ApplicationListError.LIST_NOT_FOUND,
                                                "No application list found for UUID '%s'"
                                                        .formatted(id)));

        // Fetch results from the repository using pagination
        List<ApplicationListEntryPrintProjection> applicationListEntryPrintProjections =
                aleRepository.findByIdForPrinting(id);

        for (ApplicationListEntryPrintProjection entry : applicationListEntryPrintProjections) {
            // Fetch result wordings from the repository
            List<String> resultWordings = alerRepository.findByIdForPrinting(entry.getUuid());

            // Fetch officials from the repository
            List<String> officials = aleoRepository.findByIdForPrinting(entry.getUuid());
        }

        List<EntryGetPrintDto> dtos = new ArrayList<>();

        // Map each projection to a DTO
        applicationListEntryPrintProjections.forEach(projection -> dtos.add(
            entryMapper.toPrintDto(projection)));

        return null;
    }

    private Optional<CriminalJusticeArea> resolveCja(String cjaCode) {
        return cjaCode == null
                ? Optional.empty()
                : Optional.of(locationLookupService.getCjaOrThrow(cjaCode));
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
