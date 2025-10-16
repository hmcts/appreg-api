package uk.gov.hmcts.appregister.applicationlist.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapStructMapper;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.applicationlist.mapper.ApplicationListMapper;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationListLocationValidator;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntrySummaryProjection;
import uk.gov.hmcts.appregister.courtlocation.exception.CourtLocationError;
import uk.gov.hmcts.appregister.criminaljusticearea.exception.CriminalJusticeAreaError;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;

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

    private final ApplicationListRepository repository;
    private final NationalCourtHouseRepository courtHouseRepository;
    private final CriminalJusticeAreaRepository cjaRepository;
    private final ApplicationListEntryRepository entryRepository;
    private final ApplicationListMapper mapper;
    // Mapper for transferring Spring Data {@link Page} metadata into API page objects.
    private final PageMapper pageMapper;
    private final ApplicationListEntryMapStructMapper entryMapper;
    private final ApplicationListLocationValidator validator;
    private final EntityManager entityManager;

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
        ApplicationList list = repository.findByUuid(id)
            .orElseThrow(() -> new AppRegistryException(
                ApplicationListError.LIST_NOT_FOUND,
                "No application list found for UUID '%s'".formatted(id)));

        // Fetch results from the repository using pagination
        Page<ApplicationListEntrySummaryProjection> dbPage = entryRepository.findSummariesById(id, pageable);

        List<ApplicationListEntrySummary> summaries = new ArrayList<>();

        // Map each projection to a summary model
        dbPage.forEach(projection -> summaries.add(entryMapper.toSummaryModel(projection)));

        return buildDto(list, summaries.size(), summaries);
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
        var courtCode = dto.getCourtLocationCode().trim();
        final List<NationalCourtHouse> courts = courtHouseRepository.findActiveCourts(courtCode);

        if (courts.isEmpty()) {
            throw new AppRegistryException(
                    CourtLocationError.COURT_NOT_FOUND,
                    "No court found for code '%s'".formatted(courtCode));
        } else if (courts.size() > SINGLE_RECORD) {
            throw new AppRegistryException(
                    CourtLocationError.DUPLICATE_COURT_FOUND,
                    "Multiple courts found for code '%s'".formatted(courtCode));
        }

        var savedEntity = repository.save(mapper.toCreateEntityWithCourt(dto, courts.getFirst()));
        var hydrated = refreshEntity(savedEntity);
        return mapper.toGetDetailDto(hydrated, null);
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
        var cjaCode = dto.getCjaCode().trim();
        final List<CriminalJusticeArea> criminalJusticeAreas = cjaRepository.findByCode(cjaCode);

        if (criminalJusticeAreas.isEmpty()) {
            throw new AppRegistryException(
                    CriminalJusticeAreaError.CJA_NOT_FOUND,
                    "No Criminal Justice Areas found for code '%s'".formatted(cjaCode));
        } else if (criminalJusticeAreas.size() > SINGLE_RECORD) {
            throw new AppRegistryException(
                    CriminalJusticeAreaError.DUPLICATE_CJA_FOUND,
                    "Multiple Criminal Justice Areas found for code '%s'".formatted(cjaCode));
        }

        var cja = criminalJusticeAreas.getFirst();

        var savedEntity = repository.save(mapper.toCreateEntityWithCja(dto, cja));
        var hydrated = refreshEntity(savedEntity);

        return mapper.toGetDetailDto(hydrated, cja);
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

    private ApplicationListGetDetailDto buildDto(ApplicationList list, Integer entriesCount,
                                                 List<ApplicationListEntrySummary> entriesSummary) {
        ApplicationListGetDetailDto dto = mapper.toGetDetailDto(list, null);
        dto.setEntriesCount(entriesCount);
        dto.setEntriesSummary(entriesSummary);

        return dto;
    }
}
