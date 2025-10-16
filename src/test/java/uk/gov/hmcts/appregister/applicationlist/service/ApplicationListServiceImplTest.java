package uk.gov.hmcts.appregister.applicationlist.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapStructMapper;
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
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;

@ExtendWith(MockitoExtension.class)
public class ApplicationListServiceImplTest {

    @Mock private ApplicationListRepository repository;
    @Mock private NationalCourtHouseRepository courtHouseRepository;
    @Mock private CriminalJusticeAreaRepository cjaRepository;
    @Mock private ApplicationListEntryRepository entryRepository;
    @Mock private ApplicationListMapper mapper;
    @Mock private PageMapper pageMapper;
    @Mock private ApplicationListEntryMapStructMapper entryMapper;
    @Mock private ApplicationListLocationValidator validator;
    @Mock private EntityManager entityManager;

    private ApplicationListServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new ApplicationListServiceImpl(
                        repository,
                        courtHouseRepository,
                        cjaRepository,
                        entryRepository,
                        mapper,
                        pageMapper,
                        entryMapper,
                        validator,
                        entityManager);
    }

    @Test
    void create_validCourt_savesAndReturnsDto() {

        doNothing().when(entityManager).flush();
        doNothing().when(entityManager).refresh(any(ApplicationList.class));

        // given
        ApplicationListCreateDto dto = mock(ApplicationListCreateDto.class);
        when(dto.getCourtLocationCode()).thenReturn("  ABC123  ");

        NationalCourtHouse court = new NationalCourtHouse();
        when(courtHouseRepository.findActiveCourts("ABC123")).thenReturn(List.of(court));

        ApplicationList entityToSave = new ApplicationList();
        when(mapper.toCreateEntityWithCourt(dto, court)).thenReturn(entityToSave);

        ApplicationList saved = new ApplicationList();
        when(repository.save(entityToSave)).thenReturn(saved);

        ApplicationListGetDetailDto expectedDto = new ApplicationListGetDetailDto();
        when(mapper.toGetDetailDto(saved, null)).thenReturn(expectedDto);

        ApplicationListGetDetailDto result = service.create(dto);

        Assertions.assertEquals(result, expectedDto);
        verify(entityManager).flush();
        verify(entityManager).refresh(saved);
    }

    @Test
    void create_noCourtReturnedFromRepository_throwsAppRegException() {
        // given
        ApplicationListCreateDto dto = mock(ApplicationListCreateDto.class);
        when(dto.getCourtLocationCode()).thenReturn("CODE1");

        when(courtHouseRepository.findActiveCourts("CODE1")).thenReturn(List.of());

        // expect
        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("No court found");

        verify(validator).validate(dto);
        verify(repository, never()).save(any());
    }

    @Test
    void create_multipleCourtsReturnedFromRepository_throwsAppRegException() {
        // given
        ApplicationListCreateDto dto = mock(ApplicationListCreateDto.class);
        when(dto.getCourtLocationCode()).thenReturn("DUPE");

        NationalCourtHouse c1 = new NationalCourtHouse();
        NationalCourtHouse c2 = new NationalCourtHouse();
        when(courtHouseRepository.findActiveCourts("DUPE")).thenReturn(List.of(c1, c2));

        // expect
        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("Multiple courts found");

        verify(validator).validate(dto);
        verify(repository, never()).save(any());
    }

    // -------- CJA PATH --------

    @Test
    void create_withValidCja_savesAndReturnsDto() {

        doNothing().when(entityManager).flush();
        doNothing().when(entityManager).refresh(any(ApplicationList.class));

        ApplicationListCreateDto dto = mock(ApplicationListCreateDto.class);
        when(dto.getCourtLocationCode()).thenReturn("   ");
        when(dto.getCjaCode()).thenReturn("  CJA-42  ");

        CriminalJusticeArea cja = new CriminalJusticeArea();
        when(cjaRepository.findByCode("CJA-42")).thenReturn(List.of(cja));

        ApplicationList entityToSave = new ApplicationList();
        when(mapper.toCreateEntityWithCja(dto, cja)).thenReturn(entityToSave);

        ApplicationList saved = new ApplicationList();
        when(repository.save(entityToSave)).thenReturn(saved);

        ApplicationListGetDetailDto expected = new ApplicationListGetDetailDto();
        when(mapper.toGetDetailDto(saved, cja)).thenReturn(expected);

        ApplicationListGetDetailDto result = service.create(dto);

        verify(validator).validate(dto);
        verify(cjaRepository).findByCode("CJA-42");
        verify(repository).save(entityToSave);
        verify(mapper).toGetDetailDto(saved, cja);
        assertThat(result).isSameAs(expected);
        verify(entityManager).flush();
        verify(entityManager).refresh(saved);
    }

    @Test
    void create_noCjaReturnedFromRepository_throwsAppRegException() {
        ApplicationListCreateDto dto = mock(ApplicationListCreateDto.class);
        when(dto.getCourtLocationCode()).thenReturn(null);
        when(dto.getCjaCode()).thenReturn("X1");

        when(cjaRepository.findByCode("X1")).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("No Criminal Justice Areas found");

        verify(validator).validate(dto);
        verify(repository, never()).save(any());
    }

    @Test
    void create_multipleCjaReturnedFromRepository_throwsAppRegException() {
        ApplicationListCreateDto dto = mock(ApplicationListCreateDto.class);
        when(dto.getCourtLocationCode()).thenReturn("");
        when(dto.getCjaCode()).thenReturn("Y2");

        CriminalJusticeArea a = new CriminalJusticeArea();
        CriminalJusticeArea b = new CriminalJusticeArea();
        when(cjaRepository.findByCode("Y2")).thenReturn(List.of(a, b));

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("Multiple Criminal Justice Areas found");

        verify(validator).validate(dto);
        verify(repository, never()).save(any());
    }
}
