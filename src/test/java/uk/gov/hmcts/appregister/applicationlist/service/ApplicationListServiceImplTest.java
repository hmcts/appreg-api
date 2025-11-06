package uk.gov.hmcts.appregister.applicationlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.appregister.common.util.OfficialTypeUtil.MAGISTRATE_CODE;
import static uk.gov.hmcts.appregister.data.AppListEntryResolutionTestData.WORDING_1;
import static uk.gov.hmcts.appregister.data.AppListEntryResolutionTestData.WORDING_2;
import static uk.gov.hmcts.appregister.util.ApplicationListEntryPrintProjectionUtil.applicationListEntryPrintProjection;
import static uk.gov.hmcts.appregister.util.ApplicationListEntrySummaryProjectionUtil.applicationListEntrySummaryProjection;
import static uk.gov.hmcts.appregister.util.ApplicationListOfficialPrintProjectionUtil.applicationListOfficialPrintProjection;
import static uk.gov.hmcts.appregister.util.TestConstants.APPLICATIONCODE1_CODE;
import static uk.gov.hmcts.appregister.util.TestConstants.APPLICATIONCODE1_TITLE;
import static uk.gov.hmcts.appregister.util.TestConstants.APPLICATIONLISTENTRY1_ACCOUNTNUMBER;
import static uk.gov.hmcts.appregister.util.TestConstants.APPLICATIONLISTENTRY1_CASEREFERENCE;
import static uk.gov.hmcts.appregister.util.TestConstants.APPLICATIONLISTENTRY1_NOTES;
import static uk.gov.hmcts.appregister.util.TestConstants.APPLICATIONLISTENTRY1_WORDING;
import static uk.gov.hmcts.appregister.util.TestConstants.MRS;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON1_FORENAME1;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON1_SURNAME;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_ADDRESSLINE1;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_ADDRESSLINE2;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_ADDRESSLINE3;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_ADDRESSLINE4;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_ADDRESSLINE5;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_EMAIL;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_FORENAME1;
import static uk.gov.hmcts.appregister.util.TestConstants.MR;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_FORENAME2;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_FORENAME3;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_MOBILE;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_PHONE;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_POSTCODE;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_SURNAME;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_ADDRESSLINE1;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_ADDRESSLINE2;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_ADDRESSLINE3;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_ADDRESSLINE4;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_ADDRESSLINE5;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_EMAIL;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_FORENAME1;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_FORENAME2;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_FORENAME3;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_MOBILE;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_PHONE;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_POSTCODE;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON5_SURNAME;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapStructMapper;
import uk.gov.hmcts.appregister.applicationlist.mapper.ApplicationListMapper;
import uk.gov.hmcts.appregister.applicationlist.mapper.ApplicationListOfficalMapper;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationCreateListLocationValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationListDeletionValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationListGetValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationUpdateListLocationValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ListLocationValidationSuccess;
import uk.gov.hmcts.appregister.applicationlist.validator.ListUpdateValidationSuccess;
import uk.gov.hmcts.appregister.common.concurrency.MatchProvider;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.concurrency.MatchService;
import uk.gov.hmcts.appregister.common.concurrency.MatchServiceImpl;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryOfficialRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForUpdate;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryPrintProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntrySummaryProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListOfficialPrintProjection;
import uk.gov.hmcts.appregister.common.util.OfficialTypeUtil;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetPrintDto;

@ExtendWith(MockitoExtension.class)
public class ApplicationListServiceImplTest {

    private static final LocalDate DEFAULT_DATE = LocalDate.of(2025, 10, 7);
    private static final LocalTime DEFAULT_TIME = LocalTime.of(10, 30);

    @Mock private ApplicationListRepository repository;
    @Mock private ApplicationListEntryRepository aleRepository;
    @Mock private AppListEntryResolutionRepository alerRepository;
    @Mock private ApplicationListEntryOfficialRepository aleoRepository;
    @Mock private NationalCourtHouseRepository courtHouseRepository;
    @Mock private CriminalJusticeAreaRepository cjaRepository;
    @Mock private ApplicationListMapper mapper;
    @Mock private ApplicationListOfficalMapper officalMapper;

    @Spy
    private DummyApplicationCreateListLocationValidator validator =
            new DummyApplicationCreateListLocationValidator(
                    repository, courtHouseRepository, cjaRepository);

    @Spy
    private DummyApplicationUpdateListLocationValidator updateValidator =
            new DummyApplicationUpdateListLocationValidator(
                    repository, courtHouseRepository, cjaRepository);

    @Spy
    private DummyApplicationListGetValidator getValidator =
            new DummyApplicationListGetValidator(repository, courtHouseRepository, cjaRepository);

    @Mock private PageMapper pageMapper;
    @Mock private ApplicationListEntryMapStructMapper entryMapper;

    @Mock private EntityManager entityManager;

    // A null match provider that returns a null etag
    private static MatchProvider NULL_MATCH_PROVIDER =
            new MatchProvider() {
                @Override
                public String getEtag() {
                    return null;
                }
            };

    @Spy private MatchService matchService = new MatchServiceImpl(NULL_MATCH_PROVIDER);

    @Mock private ApplicationListDeletionValidator deletionValidator;

    private ApplicationListServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new ApplicationListServiceImpl(
                        repository,
                        aleRepository,
                        alerRepository,
                        aleoRepository,
                        mapper,
                        validator,
                        updateValidator,
                        getValidator,
                        entryMapper,
                        officalMapper,
                        entityManager,
                        matchService,
                        pageMapper,
                        deletionValidator);
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

        ApplicationListGetDetailDto expected = new ApplicationListGetDetailDto();
        when(mapper.toGetDetailDto(saved, null, 0L)).thenReturn(expected);

        MatchResponse<ApplicationListGetDetailDto> result = service.create(dto);
        Assertions.assertNotNull(result.getEtag());
        Assertions.assertEquals(result.getPayload(), expected);

        verify(entityManager).flush();
        verify(entityManager).refresh(saved);

        verify(mapper).toGetDetailDto(saved, null, 0L);
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
        when(mapper.toGetDetailDto(saved, null, 0L)).thenReturn(expectedDto);

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
        when(mapper.toGetDetailDto(saved, cja, 0L)).thenReturn(expected);

        MatchResponse<ApplicationListGetDetailDto> result = service.create(dto);
        Assertions.assertNotNull(result.getEtag());
        Assertions.assertEquals(expected, result.getPayload());

        verify(validator).validate(eq(dto), notNull());
        verify(repository).save(entityToSave);
        verify(mapper).toGetDetailDto(saved, cja, 0L);

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
        when(mapper.toGetDetailDto(saved, cja, 0L)).thenReturn(expected);

        ApplicationListUpdateDto dto = mock(ApplicationListUpdateDto.class);
        PayloadForUpdate<ApplicationListUpdateDto> payloadForUpdate =
                new PayloadForUpdate<>(dto, UUID.randomUUID());

        MatchResponse<ApplicationListGetDetailDto> result = service.update(payloadForUpdate);
        Assertions.assertNotNull(result.getEtag());
        Assertions.assertEquals(expected, result.getPayload());

        verify(updateValidator).validate(eq(payloadForUpdate), notNull());
        verify(repository).save(entityToSave);
        verify(mapper).toGetDetailDto(saved, cja, 0L);
        assertThat(result.getPayload()).isSameAs(expected);
        verify(entityManager).flush();
        verify(entityManager).refresh(saved);
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

        ListLocationValidationSuccess success = new ListUpdateValidationSuccess();
        success.setCriminalJusticeArea(cja);
        getValidator.setSuccess(success);

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

        verify(aleRepository).countByApplicationListUuids(List.of(row.getUuid()));
        verify(mapper).toGetSummaryDto(eq(row), eq(0L), anyString());
    }

    @Test
    void getPage_courtFilled_success_returnsMappedPage() {

        ListLocationValidationSuccess success = new ListUpdateValidationSuccess();
        getValidator.setSuccess(success);

        // DB results
        ApplicationList row = new ApplicationList();
        row.setUuid(UUID.randomUUID());
        row.setCourtName("Central Court");
        Page<ApplicationList> dbPage = new PageImpl<>(List.of(row));

        Pageable pageable = mock(Pageable.class);

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

        verify(aleRepository).countByApplicationListUuids(List.of(row.getUuid()));
        verify(mapper).toGetSummaryDto(eq(row), eq(0L), eq("Central Court"));
    }

    @Test
    void getPage_missingEntryCount_defaultsZero_mapsSummary() {

        CriminalJusticeArea cja = new CriminalJusticeArea();
        cja.setDescription("CJA Desc");

        ListLocationValidationSuccess success = new ListUpdateValidationSuccess();
        success.setCriminalJusticeArea(cja);
        getValidator.setSuccess(success);

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

        Pageable pageable = mock(Pageable.class);

        ListLocationValidationSuccess success = new ListUpdateValidationSuccess();
        getValidator.setSuccess(success);

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

        ApplicationListGetFilterDto filter =
                new ApplicationListGetFilterDto().status(ApplicationListStatus.OPEN);

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

        ListLocationValidationSuccess success = new ListUpdateValidationSuccess();
        getValidator.setSuccess(success);

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

        ListLocationValidationSuccess success = new ListUpdateValidationSuccess();
        getValidator.setSuccess(success);

        ApplicationList row = new ApplicationList();
        row.setUuid(UUID.randomUUID());
        row.setCourtName("Some Court");

        Page<ApplicationList> dbPage = new PageImpl<>(List.of(row));
        Pageable pageable = mock(Pageable.class);
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

        ListLocationValidationSuccess success = new ListUpdateValidationSuccess();
        getValidator.setSuccess(success);

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

    @Setter
    class DummyApplicationListGetValidator extends ApplicationListGetValidator {
        private ListLocationValidationSuccess success;

        public DummyApplicationListGetValidator(
                ApplicationListRepository repository,
                NationalCourtHouseRepository courtHouseRepository,
                CriminalJusticeAreaRepository cjaRepository) {
            super(repository, courtHouseRepository, cjaRepository);
        }

        @Override
        public <R> R validate(
                ApplicationListGetFilterDto dto,
                BiFunction<ApplicationListGetFilterDto, ListLocationValidationSuccess, R>
                        createApplicationSupplier) {
            return createApplicationSupplier.apply(dto, success);
        }

        @Override
        public <R> R validateCja(
                ApplicationListGetFilterDto dto,
                BiFunction<ApplicationListGetFilterDto, ListLocationValidationSuccess, R>
                        createApplicationSupplier,
                boolean doNotFailOnMissing) {
            return createApplicationSupplier.apply(dto, success);
        }
    }

    @Test
    void get_returnsDto() {
        ApplicationList saved = new ApplicationList();
        UUID id = UUID.randomUUID();
        when(repository.findByUuid(id)).thenReturn(Optional.of(saved));

        Pageable pageable = mock(Pageable.class);

        mockFindSummariesById(id, pageable);

        ApplicationListGetDetailDto expected = new ApplicationListGetDetailDto();
        when(mapper.toGetDetailDto(saved, null, 0L)).thenReturn(expected);

        ApplicationListGetDetailDto actual = service.get(id, pageable);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void get_returns404_whenApplicationListRepositoryEmpty() {
        UUID id = UUID.randomUUID();
        when(repository.findByUuid(id)).thenReturn(Optional.empty());

        Pageable pageable = mock(Pageable.class);
        assertThatThrownBy(() -> service.get(id, pageable))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void print_returnsDto() {
        Long applicationListEntryId = 1L;

        ApplicationList saved = new ApplicationList();
        UUID id = UUID.randomUUID();
        when(repository.findByUuid(id)).thenReturn(Optional.of(saved));

        mockFindEntriesByIdForPrinting(id);

        when(alerRepository.findByIdForPrinting(eq(applicationListEntryId))).thenReturn(List.of(WORDING_1, WORDING_2));

        mockFindOfficialsByIdForPrinting(applicationListEntryId);

        EntryGetPrintDto entryGetPrintDto = new EntryGetPrintDto();
        when(entryMapper.toPrintDto(any(ApplicationListEntryPrintProjection.class))).thenReturn(entryGetPrintDto);

        ApplicationListGetPrintDto expected = new ApplicationListGetPrintDto();
        when(mapper.toGetPrintDto(saved)).thenReturn(expected);

        ApplicationListGetPrintDto actual = service.print(id);

        Assertions.assertEquals(expected, actual);
    }

    private void mockFindSummariesById(UUID id, Pageable pageable) {
        var uuid = UUID.randomUUID();
        var sequenceNumber = 1;
        var accountNumber = "1234567890";
        var applicant = "Mustafa's Org";
        var respondent = "Ahmed, Mustafa, His Majesty";
        var postCode = "SW1A 1AA";
        var applicationTitle = "Request for Certificate of Refusal to State a Case (Civil)";
        var feeRequired = true;
        var result = "APPC";
        var projection =
                applicationListEntrySummaryProjection()
                        .uuid(uuid)
                        .sequenceNumber(sequenceNumber)
                        .accountNumber(accountNumber)
                        .applicant(applicant)
                        .respondent(respondent)
                        .postCode(postCode)
                        .applicationTitle(applicationTitle)
                        .feeRequired(feeRequired)
                        .result(result)
                        .build();
        Page<ApplicationListEntrySummaryProjection> dbPage = new PageImpl<>(List.of(projection));

        when(aleRepository.findSummariesById(eq(id), eq(pageable))).thenReturn(dbPage);
    }

    private void mockFindEntriesByIdForPrinting(UUID applicationListId) {
        var projection =
            applicationListEntryPrintProjection()
                .id(1L)
                .sequenceNumber(1)
                .applicantTitle(MR)
                .applicantSurname(PERSON4_SURNAME)
                .applicantForename1(PERSON4_FORENAME1)
                .applicantForename2(PERSON4_FORENAME2)
                .applicantForename3(PERSON4_FORENAME3)
                .applicantAddressLine1(PERSON4_ADDRESSLINE1)
                .applicantAddressLine2(PERSON4_ADDRESSLINE2)
                .applicantAddressLine3(PERSON4_ADDRESSLINE3)
                .applicantAddressLine4(PERSON4_ADDRESSLINE4)
                .applicantAddressLine5(PERSON4_ADDRESSLINE5)
                .applicantPostcode(PERSON4_POSTCODE)
                .applicantPhone(PERSON4_PHONE)
                .applicantMobile(PERSON4_MOBILE)
                .applicantEmail(PERSON4_EMAIL)
                .respondentTitle(MRS)
                .respondentSurname(PERSON5_SURNAME)
                .respondentForename1(PERSON5_FORENAME1)
                .respondentForename2(PERSON5_FORENAME2)
                .respondentForename3(PERSON5_FORENAME3)
                .respondentAddressLine1(PERSON5_ADDRESSLINE1)
                .respondentAddressLine2(PERSON5_ADDRESSLINE2)
                .respondentAddressLine3(PERSON5_ADDRESSLINE3)
                .respondentAddressLine4(PERSON5_ADDRESSLINE4)
                .respondentAddressLine5(PERSON5_ADDRESSLINE5)
                .respondentPostcode(PERSON5_POSTCODE)
                .respondentPhone(PERSON5_PHONE)
                .respondentMobile(PERSON5_MOBILE)
                .respondentEmail(PERSON5_EMAIL)
                .respondentDateOfBirth(OffsetDateTime.now())
                .applicationCode(APPLICATIONCODE1_CODE)
                .applicationTitle(APPLICATIONCODE1_TITLE)
                .applicationWording(APPLICATIONLISTENTRY1_WORDING)
                .caseReference(APPLICATIONLISTENTRY1_CASEREFERENCE)
                .accountReference(APPLICATIONLISTENTRY1_ACCOUNTNUMBER)
                .notes(APPLICATIONLISTENTRY1_NOTES)
                .build();
        List<ApplicationListEntryPrintProjection> applicationListEntryPrintProjections = List.of(projection);

        when(aleRepository.findByIdForPrinting(eq(applicationListId))).thenReturn(applicationListEntryPrintProjections);
    }

    private void mockFindOfficialsByIdForPrinting(Long applicationListEntryId) {
        var projection =
            applicationListOfficialPrintProjection()
                .type(MAGISTRATE_CODE)
                .title(MR)
                .forename(PERSON1_FORENAME1)
                .surname(PERSON1_SURNAME)
                .build();
        List<ApplicationListOfficialPrintProjection> applicationListOfficialPrintProjections = List.of(projection);

        when(aleoRepository.findByIdForPrinting(eq(applicationListEntryId), eq(OfficialTypeUtil.PRINTABLE_CODES)))
            .thenReturn(applicationListOfficialPrintProjections);
    }
}
