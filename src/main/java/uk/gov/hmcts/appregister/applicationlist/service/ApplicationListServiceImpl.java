package uk.gov.hmcts.appregister.applicationlist.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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
import uk.gov.hmcts.appregister.courtlocation.exception.CourtLocationError;
import uk.gov.hmcts.appregister.criminaljusticearea.exception.CriminalJusticeAreaError;
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

    private final ApplicationListRepository repository;
    private final NationalCourtHouseRepository courtHouseRepository;
    private final CriminalJusticeAreaRepository cjaRepository;
    private final ApplicationListEntryRepository aleRepository;
    private final ApplicationListMapper mapper;
    private final ApplicationListLocationValidator validator;
    private final EntityManager entityManager;
    private final PageMapper pageMapper;

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
    public ApplicationListPage getPage(ApplicationListGetFilterDto dto, Pageable pageable) {

        CriminalJusticeArea cja = null;

        if (dto.getCjaCode() != null) {
            final List<CriminalJusticeArea> criminalJusticeAreas = cjaRepository.findByCode(dto.getCjaCode());

            // =======
            // TODO - This code is copied across the app. Split out into it's own service.
            var cjaCode = dto.getCjaCode().trim();
            if (criminalJusticeAreas.isEmpty()) {
                throw new AppRegistryException(
                    CriminalJusticeAreaError.CJA_NOT_FOUND,
                    "No Criminal Justice Areas found for code '%s'".formatted(cjaCode));
            } else if (criminalJusticeAreas.size() > 1) {
                throw new AppRegistryException(
                    CriminalJusticeAreaError.DUPLICATE_CJA_FOUND,
                    "Multiple Criminal Justice Areas found for code '%s'".formatted(cjaCode));
            }
            cja = criminalJusticeAreas.getFirst();
            // ========
        }

        // TODO - Date/Time converter needs to be consistent across the app - split into it's own service
        LocalDate date = dto.getDate();
        LocalTime time = LocalTime.parse(dto.getTime());
        LocalDateTime dateTime = date.atTime(time);

        // TODO - Pass in dto as single object?
        final Page<ApplicationList> dbPage = repository.findAllByFilter(
            dto.getStatus(),
            dto.getCourtLocationCode(),
            cja,
            dto.getDate().atStartOfDay(),
            dateTime,
            dto.getDescription(),
            dto.getOtherLocationDescription(),
            pageable
            );

        var responsePage = new ApplicationListPage();

        if (responsePage.getContent() == null) {
            responsePage.setContent(new ArrayList<>());
        }

        pageMapper.toPage(dbPage, responsePage);

        dbPage.forEach(applicationList -> {

            String location = "Location not set";

            if (applicationList.getCourtName() != null) {
                location = applicationList.getCourtName();
            // TODO - check in the legacy app if the location for CJA is a combo of Other Location and CJA
            } else if (applicationList.getCja() != null) {
                location = applicationList.getCja().getDescription();
            }

            // TODO - This count is going to be called for every application list - can we optimise?
            long entryCount = aleRepository.countByApplicationListPk(applicationList.getPk());
            responsePage.addContentItem(mapper.toGetSummaryDto(applicationList, entryCount, location));
        });

        return responsePage;
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
        } else if (courts.size() > 1) {
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
        } else if (criminalJusticeAreas.size() > 1) {
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
}
