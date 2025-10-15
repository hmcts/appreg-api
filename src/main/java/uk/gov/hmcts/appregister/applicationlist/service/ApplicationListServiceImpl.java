package uk.gov.hmcts.appregister.applicationlist.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.applicationlist.mapper.ApplicationListMapper;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationCreateListLocationValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationUpdateListLocationValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ListLocationValidationSuccess;
import uk.gov.hmcts.appregister.applicationlist.validator.ListUpdateValidationSuccess;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.concurrency.MatchService;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.model.PayloadForUpdate;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
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
@RequiredArgsConstructor
@Service
public class ApplicationListServiceImpl implements ApplicationListService {
    private final ApplicationListRepository repository;
    private final NationalCourtHouseRepository courtHouseRepository;
    private final CriminalJusticeAreaRepository cjaRepository;
    private final ApplicationListMapper mapper;
    private final ApplicationCreateListLocationValidator applicationCreateListLocationValidator;
    private final ApplicationUpdateListLocationValidator applicationUpdateListLocationValidator;

    private final EntityManager entityManager;
    private final MatchService matchService;

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
        return applicationCreateListLocationValidator.validate(
                dto,
                (listCreateDto, success) ->
                        success.hasCourt()
                                ? createWithCourt(listCreateDto, success)
                                : createWithCja(listCreateDto, success));
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
        return applicationUpdateListLocationValidator.validate(
                dto,
                (updateDto, success) ->
                        success.hasCourt()
                                ? updateWithCourt(updateDto, success)
                                : updateWithCja(updateDto, success));
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
    private MatchResponse<ApplicationListGetDetailDto> createWithCourt(
            ApplicationListCreateDto createDto, ListLocationValidationSuccess success) {
        var court = success.getNationalCourtHouse();
        var savedEntity = repository.save(mapper.toCreateEntityWithCourt(createDto, court));
        var hydrated = refreshEntity(savedEntity);
        return MatchResponse.of(
                hydrated.getUuid(), hydrated, mapper.toGetDetailDto(hydrated, null));
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
    private MatchResponse<ApplicationListGetDetailDto> createWithCja(
            ApplicationListCreateDto createDto, ListLocationValidationSuccess success) {
        var cja = success.getCriminalJusticeArea();

        var savedEntity = repository.save(mapper.toCreateEntityWithCja(createDto, cja));
        var hydrated = refreshEntity(savedEntity);

        return MatchResponse.of(hydrated.getUuid(), hydrated, mapper.toGetDetailDto(hydrated, cja));
    }

    /**
     * Update an Application List associated with a Court.
     *
     * @param updateDto the DTO containing court-based application list details
     * @param success The validation validated details
     * @return the created Application List DTO
     */
    private MatchResponse<ApplicationListGetDetailDto> updateWithCourt(
            PayloadForUpdate<ApplicationListUpdateDto> updateDto,
            ListUpdateValidationSuccess success) {
        var court = success.getNationalCourtHouse();

        mapper.toUpdateEntityWithCourt(
                updateDto.getData(), null, court, success.getApplicationList());

        return matchService.matchOnRequest(
                success.getApplicationList().getUuid(),
                success.getApplicationList(),
                () -> {
                    var savedEntity = repository.save(success.getApplicationList());
                    var hydrated = refreshEntity(savedEntity);
                    return MatchResponse.of(
                            hydrated.getUuid(), hydrated, mapper.toGetDetailDto(hydrated, null));
                });
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
    private MatchResponse<ApplicationListGetDetailDto> updateWithCja(
            PayloadForUpdate<ApplicationListUpdateDto> updateDto,
            ListUpdateValidationSuccess success) {
        var cja = success.getCriminalJusticeArea();
        ApplicationList applicationList = success.getApplicationList();

        mapper.toUpdateEntityWithCja(updateDto.getData(), cja, applicationList);

        return matchService.matchOnRequest(
                success.getApplicationList().getUuid(),
                success.getApplicationList(),
                () -> {
                    var savedEntity = repository.save(applicationList);
                    var hydrated = refreshEntity(savedEntity);

                    return MatchResponse.of(
                            hydrated.getUuid(), hydrated, mapper.toGetDetailDto(hydrated, cja));
                });
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
