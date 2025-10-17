package uk.gov.hmcts.appregister.applicationlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.applicationlist.mapper.ApplicationListMapper;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationListDeletionValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationListLocationValidator;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.service.LocationLookupService;
import uk.gov.hmcts.appregister.courtlocation.exception.CourtLocationError;
import uk.gov.hmcts.appregister.criminaljusticearea.exception.CriminalJusticeAreaError;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;

@ExtendWith(MockitoExtension.class)
public class ApplicationListServiceImplTest {

    private static final LocalDate DEFAULT_DATE = LocalDate.of(2025, 10, 7);
    private static final LocalTime DEFAULT_TIME = LocalTime.of(10, 30);

    @Mock private ApplicationListRepository repository;
    @Mock private ApplicationListEntryRepository aleRepository;
    @Mock private ApplicationListMapper mapper;
    @Mock private ApplicationListLocationValidator validator;
    @Mock private EntityManager entityManager;
    @Mock private ApplicationListDeletionValidator deletionValidator;
    @Mock private PageMapper pageMapper;
    @Mock private LocationLookupService locationLookupService;

    private ApplicationListServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new ApplicationListServiceImpl(
                        repository,
                        aleRepository,
                        mapper,
                        validator,
                        entityManager,
                        pageMapper,
                        locationLookupService,
                        deletionValidator);
    }

    // -------- CREATE: COURT PATH --------

    @Test
    void create_validCourt_savesAndReturnsDto() {

        doNothing().when(entityManager).flush();
        doNothing().when(entityManager).refresh(any(ApplicationList.class));

        ApplicationListCreateDto dto = new ApplicationListCreateDto().courtLocationCode("ABC123");

        NationalCourtHouse court = new NationalCourtHouse();
        when(locationLookupService.getActiveCourtOrThrow("ABC123")).thenReturn(court);

        ApplicationList toSave = new ApplicationList();
        when(mapper.toCreateEntityWithCourt(dto, court)).thenReturn(toSave);

        ApplicationList saved = new ApplicationList();
        when(repository.save(toSave)).thenReturn(saved);

        ApplicationListGetDetailDto expected = new ApplicationListGetDetailDto();
        when(mapper.toGetDetailDto(saved, null)).thenReturn(expected);

        ApplicationListGetDetailDto result = service.create(dto);

        Assertions.assertEquals(expected, result);
        verify(validator).validate(dto);
        verify(locationLookupService).getActiveCourtOrThrow("ABC123");
        verify(mapper).toCreateEntityWithCourt(dto, court);
        verify(repository).save(toSave);
        verify(entityManager).flush();
        verify(entityManager).refresh(saved);
        verify(mapper).toGetDetailDto(saved, null);
    }

    @Test
    void create_noCourtFound_throwsAppRegistryException() {
        ApplicationListCreateDto dto = new ApplicationListCreateDto().courtLocationCode("CODE1");

        when(locationLookupService.getActiveCourtOrThrow("CODE1"))
                .thenThrow(
                        new AppRegistryException(
                                CourtLocationError.COURT_NOT_FOUND, "No court found"));

        // expect
        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("No court found");

        verify(validator).validate(dto);
        verify(repository, never()).save(any());
    }

    @Test
    void create_multipleCourtsFound_throwsAppRegistryException() {
        ApplicationListCreateDto dto = new ApplicationListCreateDto().courtLocationCode("DUPE");

        when(locationLookupService.getActiveCourtOrThrow("DUPE"))
                .thenThrow(
                        new AppRegistryException(
                                CourtLocationError.DUPLICATE_COURT_FOUND, "Multiple courts found"));

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("Multiple courts found");

        verify(validator).validate(dto);
        verify(repository, never()).save(any());
    }

    // -------- CREATE: CJA PATH ----------

    @Test
    void create_validCja_savesAndReturnsDto() {
        doNothing().when(entityManager).flush();
        doNothing().when(entityManager).refresh(any(ApplicationList.class));

        ApplicationListCreateDto dto =
                new ApplicationListCreateDto().courtLocationCode("   ").cjaCode("CJA-42");

        CriminalJusticeArea cja = new CriminalJusticeArea();
        when(locationLookupService.getCjaOrThrow("CJA-42")).thenReturn(cja);

        ApplicationList toSave = new ApplicationList();
        when(mapper.toCreateEntityWithCja(dto, cja)).thenReturn(toSave);

        ApplicationList saved = new ApplicationList();
        when(repository.save(toSave)).thenReturn(saved);

        ApplicationListGetDetailDto expected = new ApplicationListGetDetailDto();
        when(mapper.toGetDetailDto(saved, cja)).thenReturn(expected);

        ApplicationListGetDetailDto result = service.create(dto);

        verify(validator).validate(dto);
        verify(locationLookupService).getCjaOrThrow("CJA-42");
        verify(repository).save(toSave);
        verify(mapper).toGetDetailDto(saved, cja);
        assertThat(result).isSameAs(expected);
        verify(entityManager).flush();
        verify(entityManager).refresh(saved);
    }

    @Test
    void create_noCjaFound_throwsAppRegistryException() {
        ApplicationListCreateDto dto =
                new ApplicationListCreateDto().courtLocationCode(null).cjaCode("X1");

        when(locationLookupService.getCjaOrThrow("X1"))
                .thenThrow(
                        new AppRegistryException(
                                CriminalJusticeAreaError.CJA_NOT_FOUND,
                                "No Criminal Justice Areas found"));

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("No Criminal Justice Areas found");

        verify(validator).validate(dto);
        verify(repository, never()).save(any());
    }

    @Test
    void create_multipleCjaFound_throwsAppRegistryException() {
        ApplicationListCreateDto dto =
                new ApplicationListCreateDto().courtLocationCode("").cjaCode("Y2");

        when(locationLookupService.getCjaOrThrow("Y2"))
                .thenThrow(
                        new AppRegistryException(
                                CriminalJusticeAreaError.DUPLICATE_CJA_FOUND,
                                "Multiple Criminal Justice Areas found"));

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("Multiple Criminal Justice Areas found");

        verify(validator).validate(dto);
        verify(repository, never()).save(any());
    }

    @Test
    void delete_validId_deletesEntry() {
        UUID id = UUID.randomUUID();
        when(repository.findByUuid(id)).thenReturn(Optional.of(new ApplicationList()));

        service.delete(id);

        verify(deletionValidator).validate(id);
        verify(repository).findByUuid(id);
        verify(repository).save(any(ApplicationList.class));
    }

    @Test
    void getPage_cjaAndOtherLocationFilled_success_returnsMappedPage() {

        // Resolve CJA
        CriminalJusticeArea cja = new CriminalJusticeArea();
        cja.setDescription("CJA Desc");
        when(locationLookupService.getCjaOrThrow("52")).thenReturn(cja);

        // DB results
        ApplicationList row = new ApplicationList();
        row.setUuid(UUID.randomUUID());
        row.setCja(cja);
        Page<ApplicationList> dbPage = new PageImpl<>(List.of(row));

        Pageable pageable = mock(Pageable.class);
        when(repository.findAllByFilter(
                        eq(ApplicationListStatus.OPEN),
                        isNull(),
                        eq(cja),
                        eq(DEFAULT_DATE),
                        eq(DEFAULT_TIME),
                        eq("morning"),
                        eq("town hall"),
                        eq(pageable)))
                .thenReturn(dbPage);

        when(aleRepository.countByApplicationListUuids(List.of(row.getUuid())))
                .thenReturn(List.of());

        // Page metadata mapping
        doAnswer(
                        inv -> {
                            ApplicationListPage target = inv.getArgument(1);
                            target.totalPages(1);
                            target.elementsOnPage(1);
                            return null;
                        })
                .when(pageMapper)
                .toPage(eq(dbPage), any(ApplicationListPage.class));

        // Given a filter with CJA + otherLocation (court is null)
        ApplicationListGetFilterDto filter =
                new ApplicationListGetFilterDto()
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(null)
                        .cjaCode("52")
                        .date(DEFAULT_DATE)
                        .time(DEFAULT_TIME)
                        .description("morning")
                        .otherLocationDescription("town hall");

        // When
        ApplicationListPage result = service.getPage(filter, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotNull();
        assertThat(result.getContent().size()).isEqualTo(1);

        verify(locationLookupService).getCjaOrThrow("52");
        verify(aleRepository).countByApplicationListUuids(List.of(row.getUuid()));
        verify(mapper).toGetSummaryDto(eq(row), eq(0L), anyString());
    }

    @Test
    void getPage_courtFilled_success_returnsMappedPage() {

        Pageable pageable = mock(Pageable.class);

        // DB results
        ApplicationList row = new ApplicationList();
        row.setUuid(UUID.randomUUID());
        row.setCourtName("Central Court");
        Page<ApplicationList> dbPage = new PageImpl<>(List.of(row));

        when(repository.findAllByFilter(
                        eq(ApplicationListStatus.CLOSED),
                        eq("LOC123"),
                        isNull(),
                        eq(DEFAULT_DATE),
                        eq(DEFAULT_TIME),
                        isNull(),
                        isNull(),
                        eq(pageable)))
                .thenReturn(dbPage);

        when(aleRepository.countByApplicationListUuids(List.of(row.getUuid())))
                .thenReturn(List.of());
        doAnswer(inv -> null).when(pageMapper).toPage(eq(dbPage), any(ApplicationListPage.class));

        // Given a filter with COURT (CJA is null)
        ApplicationListGetFilterDto filter =
                new ApplicationListGetFilterDto()
                        .status(ApplicationListStatus.CLOSED)
                        .courtLocationCode("LOC123")
                        .cjaCode(null)
                        .date(DEFAULT_DATE)
                        .time(DEFAULT_TIME);

        // When
        ApplicationListPage result = service.getPage(filter, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotNull();
        assertThat(result.getContent().size()).isEqualTo(1);

        verify(locationLookupService, never()).getCjaOrThrow(anyString());
        verify(aleRepository).countByApplicationListUuids(List.of(row.getUuid()));
        verify(mapper).toGetSummaryDto(eq(row), eq(0L), eq("Central Court"));
    }

    @Test
    void getPage_missingEntryCount_defaultsZero_mapsSummary() {

        CriminalJusticeArea cja = new CriminalJusticeArea();
        cja.setDescription("CJA Desc");
        when(locationLookupService.getCjaOrThrow("52")).thenReturn(cja);

        ApplicationList row = new ApplicationList();
        row.setUuid(UUID.randomUUID());
        row.setCja(cja);

        Pageable pageable = mock(Pageable.class);

        Page<ApplicationList> dbPage = new PageImpl<>(List.of(row));
        when(repository.findAllByFilter(
                        eq(ApplicationListStatus.OPEN),
                        isNull(),
                        eq(cja),
                        isNull(),
                        isNull(),
                        isNull(),
                        eq("town"),
                        eq(pageable)))
                .thenReturn(dbPage);

        when(aleRepository.countByApplicationListUuids(List.of(row.getUuid())))
                .thenReturn(List.of());
        doAnswer(inv -> null).when(pageMapper).toPage(eq(dbPage), any(ApplicationListPage.class));

        // Given CJA filter, no entry count returned
        ApplicationListGetFilterDto filter =
                new ApplicationListGetFilterDto()
                        .status(ApplicationListStatus.OPEN)
                        .cjaCode("52")
                        .otherLocationDescription("town");

        ApplicationListPage result = service.getPage(filter, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotNull();
        assertThat(result.getContent().size()).isEqualTo(1);

        verify(mapper).toGetSummaryDto(eq(row), eq(0L), eq("CJA Desc"));
    }

    @Test
    void getPage_emptyRepositoryPage_returnsEmptyContent() {
        ApplicationListGetFilterDto filter =
                new ApplicationListGetFilterDto().status(ApplicationListStatus.OPEN);

        Pageable pageable = mock(Pageable.class);

        Page<ApplicationList> dbPage = Page.empty();
        when(repository.findAllByFilter(
                        eq(ApplicationListStatus.OPEN),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        eq(pageable)))
                .thenReturn(dbPage);

        doAnswer(inv -> null).when(pageMapper).toPage(eq(dbPage), any(ApplicationListPage.class));

        ApplicationListPage result = service.getPage(filter, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotNull();
        assertThat(result.getContent()).isEmpty();

        verify(aleRepository, never()).countByApplicationListUuids(any());
        verify(mapper, never()).toGetSummaryDto(any(), anyLong(), anyString());
    }

    @Test
    void getPage_cjaPresent_derivesLocation_usesCjaDescription() {

        CriminalJusticeArea cja = new CriminalJusticeArea();
        cja.setDescription("CJA Name");

        ApplicationList row = new ApplicationList();
        row.setUuid(UUID.randomUUID());
        row.setCja(cja);

        Pageable pageable = mock(Pageable.class);

        Page<ApplicationList> dbPage = new PageImpl<>(List.of(row));
        when(repository.findAllByFilter(
                        eq(ApplicationListStatus.OPEN),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        eq(pageable)))
                .thenReturn(dbPage);

        when(aleRepository.countByApplicationListUuids(List.of(row.getUuid())))
                .thenReturn(List.of());
        doAnswer(inv -> null).when(pageMapper).toPage(eq(dbPage), any(ApplicationListPage.class));

        ApplicationListGetFilterDto filter =
                new ApplicationListGetFilterDto().status(ApplicationListStatus.OPEN);

        ApplicationListPage result = service.getPage(filter, pageable);

        assertThat(result.getContent()).isNotNull().hasSize(1);
        verify(mapper).toGetSummaryDto(eq(row), eq(0L), eq("CJA Name"));
    }

    @Test
    void getPage_courtNamePresent_derivesLocation_usesCourtName() {

        Pageable pageable = mock(Pageable.class);

        ApplicationList row = new ApplicationList();
        row.setUuid(UUID.randomUUID());
        row.setCourtName("Some Court");

        Page<ApplicationList> dbPage = new PageImpl<>(List.of(row));
        when(repository.findAllByFilter(
                        eq(ApplicationListStatus.OPEN),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        eq(pageable)))
                .thenReturn(dbPage);

        when(aleRepository.countByApplicationListUuids(List.of(row.getUuid())))
                .thenReturn(List.of());
        doAnswer(inv -> null).when(pageMapper).toPage(eq(dbPage), any(ApplicationListPage.class));

        ApplicationListGetFilterDto filter =
                new ApplicationListGetFilterDto().status(ApplicationListStatus.OPEN);

        ApplicationListPage result = service.getPage(filter, pageable);

        assertThat(result.getContent()).isNotNull().hasSize(1);
        verify(mapper).toGetSummaryDto(eq(row), eq(0L), eq("Some Court"));
    }

    @Test
    void getPage_noCourtOrCja_derivesLocation_usesFallback() {

        Pageable pageable = mock(Pageable.class);

        ApplicationList row = new ApplicationList();
        row.setUuid(UUID.randomUUID());

        Page<ApplicationList> dbPage = new PageImpl<>(List.of(row));
        when(repository.findAllByFilter(
                        eq(ApplicationListStatus.OPEN),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        eq(pageable)))
                .thenReturn(dbPage);

        when(aleRepository.countByApplicationListUuids(List.of(row.getUuid())))
                .thenReturn(List.of());
        doAnswer(inv -> null).when(pageMapper).toPage(eq(dbPage), any(ApplicationListPage.class));

        ApplicationListGetFilterDto filter =
                new ApplicationListGetFilterDto().status(ApplicationListStatus.OPEN);
        ApplicationListPage result = service.getPage(filter, pageable);

        assertThat(result.getContent()).isNotNull().hasSize(1);
        verify(mapper).toGetSummaryDto(eq(row), eq(0L), eq("Location not set"));
    }
}
