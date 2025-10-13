package uk.gov.hmcts.appregister.applicationlist.service;

import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.appregister.applicationlist.mapper.ApplicationListMapper;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationListLocationValidator;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.service.LocationLookupService;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;

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
public class ApplicationListServiceImpl implements ApplicationListService {

    private static final int SINGLE_RECORD = 1;
    private static final long ZERO_ENTITIES = 0L;

    private final ApplicationListRepository repository;
    private final ApplicationListEntryRepository aleRepository;
    private final ApplicationListMapper mapper;
    private final ApplicationListLocationValidator validator;
    private final EntityManager entityManager;
    private final PageMapper pageMapper;
    private final LocationLookupService locationLookupService;

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
        return mapper.toGetDetailDto(hydratedEntity, null);
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
        return mapper.toGetDetailDto(hydratedEntity, cja);
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

    private Optional<CriminalJusticeArea> resolveCja(String cjaCode) {
        return cjaCode == null
                ? Optional.empty()
                : Optional.of(locationLookupService.getCjaOrThrow(cjaCode));
    }

    private Map<UUID, Long> fetchEntryCounts(List<UUID> uuids) {
        return aleRepository.countByApplicationListUuids(uuids).stream()
                .collect(
                        Collectors.toMap(
                                ApplicationListEntryRepository.EntryCount::getPk,
                                ApplicationListEntryRepository.EntryCount::getCnt));
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
