package uk.gov.hmcts.appregister.applicationlist.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.applicationlist.mapper.ApplicationListMapper;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationCreateListLocationValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationUpdateListLocationValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ListLocationValidationSuccess;
import uk.gov.hmcts.appregister.applicationlist.validator.ListUpdateValidationSuccess;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.concurrency.MatchService;
import uk.gov.hmcts.appregister.common.concurrency.MatchServiceImpl;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.common.model.PayloadForUpdate;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;

@ExtendWith(MockitoExtension.class)
public class ApplicationListServiceImplTest {

    @Mock private ApplicationListRepository repository;
    @Mock private NationalCourtHouseRepository courtHouseRepository;
    @Mock private CriminalJusticeAreaRepository cjaRepository;
    @Mock private ApplicationListMapper mapper;

    @Spy
    private DummyApplicationCreateListLocationValidator validator =
            new DummyApplicationCreateListLocationValidator(
                    repository, courtHouseRepository, cjaRepository);

    @Spy
    private DummyApplicationUpdateListLocationValidator updateValidator =
            new DummyApplicationUpdateListLocationValidator(
                    repository, courtHouseRepository, cjaRepository);

    @Mock private EntityManager entityManager;
    @Spy private MatchService matchService = new MatchServiceImpl(null);

    private ApplicationListServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new ApplicationListServiceImpl(
                        repository,
                        courtHouseRepository,
                        cjaRepository,
                        mapper,
                        validator,
                        updateValidator,
                        entityManager,
                        matchService);
    }

    @Test
    void create_validCourt_savesAndReturnsDto() {

        doNothing().when(entityManager).flush();
        doNothing().when(entityManager).refresh(any(ApplicationList.class));

        // given
        ApplicationListCreateDto dto = mock(ApplicationListCreateDto.class);

        NationalCourtHouse court = new NationalCourtHouse();

        ListLocationValidationSuccess success = new ListLocationValidationSuccess();
        success.setNationalCourtHouse(court);

        validator.setSuccess(success);

        ApplicationList entityToSave = new ApplicationList();
        when(mapper.toCreateEntityWithCourt(dto, court)).thenReturn(entityToSave);

        ApplicationList saved = new ApplicationList();
        when(repository.save(entityToSave)).thenReturn(saved);

        ApplicationListGetDetailDto expectedDto = new ApplicationListGetDetailDto();
        when(mapper.toGetDetailDto(saved, null)).thenReturn(expectedDto);

        MatchResponse<ApplicationListGetDetailDto> result = service.create(dto);
        Assertions.assertNotNull(result.getEtag());
        Assertions.assertEquals(result.getPayload(), expectedDto);

        verify(entityManager).flush();
        verify(entityManager).refresh(saved);
    }

    @Test
    void update_validCourt_savesAndReturnsDto() {

        doNothing().when(entityManager).flush();
        doNothing().when(entityManager).refresh(any(ApplicationList.class));

        // given
        NationalCourtHouse court = new NationalCourtHouse();

        // the app list that is updated
        ApplicationList applicationList = new ApplicationList();

        ListUpdateValidationSuccess success = new ListUpdateValidationSuccess();
        success.setNationalCourtHouse(court);
        success.setApplicationList(applicationList);
        updateValidator.setSuccess(success);

        ApplicationList entityToSave = new ApplicationList();

        ApplicationList saved = new ApplicationList();
        when(repository.save(entityToSave)).thenReturn(saved);

        ApplicationListGetDetailDto expectedDto = new ApplicationListGetDetailDto();
        when(mapper.toGetDetailDto(saved, null)).thenReturn(expectedDto);

        ApplicationListUpdateDto dto = mock(ApplicationListUpdateDto.class);
        PayloadForUpdate.builder().id(UUID.randomUUID()).data(dto).build();

        PayloadForUpdate<ApplicationListUpdateDto> payloadForUpdate =
                new PayloadForUpdate<>(dto, UUID.randomUUID());
        MatchResponse<ApplicationListGetDetailDto> result = service.update(payloadForUpdate);
        Assertions.assertNotNull(result.getEtag());
        Assertions.assertEquals(result.getPayload(), expectedDto);

        verify(entityManager).flush();
        verify(entityManager).refresh(saved);
    }

    // -------- CJA PATH --------

    @Test
    void create_withValidCja_savesAndReturnsDto() {

        doNothing().when(entityManager).flush();
        doNothing().when(entityManager).refresh(any(ApplicationList.class));

        ApplicationListCreateDto dto = mock(ApplicationListCreateDto.class);
        CriminalJusticeArea cja = new CriminalJusticeArea();

        ListLocationValidationSuccess success = new ListLocationValidationSuccess();
        success.setCriminalJusticeArea(cja);
        validator.setSuccess(success);

        ApplicationList entityToSave = new ApplicationList();
        when(mapper.toCreateEntityWithCja(dto, cja)).thenReturn(entityToSave);

        ApplicationList saved = new ApplicationList();
        when(repository.save(entityToSave)).thenReturn(saved);

        ApplicationListGetDetailDto expected = new ApplicationListGetDetailDto();
        when(mapper.toGetDetailDto(saved, cja)).thenReturn(expected);

        MatchResponse<ApplicationListGetDetailDto> result = service.create(dto);
        Assertions.assertNotNull(result.getEtag());
        Assertions.assertEquals(expected, result.getPayload());

        verify(validator).validate(eq(dto), notNull());
        verify(repository).save(entityToSave);
        verify(mapper).toGetDetailDto(saved, cja);
        assertThat(result.getPayload()).isSameAs(expected);
        verify(entityManager).flush();
        verify(entityManager).refresh(saved);
    }

    @Test
    void update_withValidCja_savesAndReturnsDto() {

        doNothing().when(entityManager).flush();
        doNothing().when(entityManager).refresh(any(ApplicationList.class));
        CriminalJusticeArea cja = new CriminalJusticeArea();

        // the app list that is updated
        ApplicationList applicationList = new ApplicationList();

        ListUpdateValidationSuccess success = new ListUpdateValidationSuccess();
        success.setCriminalJusticeArea(cja);
        success.setApplicationList(applicationList);

        updateValidator.setSuccess(success);

        ApplicationList entityToSave = new ApplicationList();

        ApplicationList saved = new ApplicationList();
        when(repository.save(entityToSave)).thenReturn(saved);

        ApplicationListGetDetailDto expected = new ApplicationListGetDetailDto();
        when(mapper.toGetDetailDto(saved, cja)).thenReturn(expected);

        ApplicationListUpdateDto dto = mock(ApplicationListUpdateDto.class);
        PayloadForUpdate<ApplicationListUpdateDto> payloadForUpdate =
                new PayloadForUpdate<>(dto, UUID.randomUUID());

        MatchResponse<ApplicationListGetDetailDto> result = service.update(payloadForUpdate);
        Assertions.assertNotNull(result.getEtag());
        Assertions.assertEquals(expected, result.getPayload());

        verify(updateValidator).validate(eq(payloadForUpdate), notNull());
        verify(repository).save(entityToSave);
        verify(mapper).toGetDetailDto(saved, cja);
        assertThat(result.getPayload()).isSameAs(expected);
        verify(entityManager).flush();
        verify(entityManager).refresh(saved);
    }

    @Setter
    class DummyApplicationCreateListLocationValidator
            extends ApplicationCreateListLocationValidator {
        private ListLocationValidationSuccess success;

        public DummyApplicationCreateListLocationValidator(
                ApplicationListRepository repository,
                NationalCourtHouseRepository courtHouseRepository,
                CriminalJusticeAreaRepository cjaRepository) {
            super(repository, courtHouseRepository, cjaRepository);
        }

        @Override
        public <R> R validate(
                ApplicationListCreateDto dto,
                BiFunction<ApplicationListCreateDto, ListLocationValidationSuccess, R>
                        createApplicationSupplier) {
            return createApplicationSupplier.apply(dto, success);
        }
    }

    @Setter
    class DummyApplicationUpdateListLocationValidator
            extends ApplicationUpdateListLocationValidator {
        private ListUpdateValidationSuccess success;

        public DummyApplicationUpdateListLocationValidator(
                ApplicationListRepository repository,
                NationalCourtHouseRepository courtHouseRepository,
                CriminalJusticeAreaRepository cjaRepository) {
            super(repository, courtHouseRepository, cjaRepository);
        }

        @Override
        public <R> R validate(
                PayloadForUpdate<ApplicationListUpdateDto> dto,
                BiFunction<
                                PayloadForUpdate<ApplicationListUpdateDto>,
                                ListUpdateValidationSuccess,
                                R>
                        createApplicationSupplier) {
            return createApplicationSupplier.apply(dto, success);
        }
    }
}
