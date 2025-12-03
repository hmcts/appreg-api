package uk.gov.hmcts.appregister.applicationlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
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
import static uk.gov.hmcts.appregister.data.AppListEntryResolutionTestData.WORDING_1;
import static uk.gov.hmcts.appregister.data.AppListEntryResolutionTestData.WORDING_2;
import static uk.gov.hmcts.appregister.util.ApplicationListEntryPrintProjectionUtil.applicationListEntryPrintProjection;
import static uk.gov.hmcts.appregister.util.ApplicationListEntrySummaryProjectionUtil.applicationListEntrySummaryProjection;
import static uk.gov.hmcts.appregister.util.TestConstants.MR;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_FORENAME1;
import static uk.gov.hmcts.appregister.util.TestConstants.PERSON4_SURNAME;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
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
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapper;
import uk.gov.hmcts.appregister.applicationlist.audit.AppListAuditOperation;
import uk.gov.hmcts.appregister.applicationlist.mapper.ApplicationListMapper;
import uk.gov.hmcts.appregister.applicationlist.mapper.ApplicationListOfficialMapper;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationCreateListLocationValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationListDeletionValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationListGetValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationUpdateListLocationValidator;
import uk.gov.hmcts.appregister.applicationlist.validator.ListLocationValidationSuccess;
import uk.gov.hmcts.appregister.applicationlist.validator.ListUpdateValidationSuccess;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.event.StartEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.concurrency.MatchProvider;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.concurrency.MatchService;
import uk.gov.hmcts.appregister.common.concurrency.MatchServiceImpl;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryOfficialRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForUpdate;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryOfficialPrintProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryResolutionPrintProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntrySummaryProjection;
import uk.gov.hmcts.appregister.common.util.OfficialTypeUtil;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.Official;

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
    @Mock private ApplicationListOfficialMapper officalMapper;

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
    @Mock private ApplicationListEntryMapper entryMapper;

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

    @Mock private AuditOperationLifecycleListener auditOperationLifecycleListener;

    @Spy
    private final AuditOperationService auditOperationService = new DummyAuditOperationService();

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
                        entryMapper,
                        officalMapper,
                        pageMapper,
                        validator,
                        updateValidator,
                        getValidator,
                        deletionValidator,
                        matchService,
                        entityManager,
                        auditOperationService,
                        List.of(auditOperationLifecycleListener));
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
        LocalTime expectedEndTime = DEFAULT_TIME.plusMinutes(1);
        when(repository.findAllByFilter(
                        eq(ApplicationListStatus.OPEN),
                        isNull(),
                        eq(cja),
                        eq(DEFAULT_DATE),
                        eq(DEFAULT_TIME),
                        eq(expectedEndTime),
                        eq(false),
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

        LocalTime expectedEndTime = DEFAULT_TIME.plusMinutes(1);

        when(repository.findAllByFilter(
                        eq(ApplicationListStatus.CLOSED),
                        eq("LOC123"),
                        isNull(),
                        eq(DEFAULT_DATE),
                        eq(DEFAULT_TIME),
                        eq(expectedEndTime),
                        eq(false),
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
                        eq(false),
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
                        eq(false),
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
                        eq(false),
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
                        eq(false),
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
                        eq(false),
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

    @Test
    void getPage_minuteToMidnightTime_callsFindAllByFilterWithWrapsMidnightTrue() {

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
        LocalTime time = LocalTime.of(23, 59);
        LocalTime expectedEndTime = LocalTime.of(0, 0);
        when(repository.findAllByFilter(
                        eq(ApplicationListStatus.OPEN),
                        isNull(),
                        eq(cja),
                        eq(DEFAULT_DATE),
                        eq(time),
                        eq(expectedEndTime),
                        eq(true),
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
                        .time(time)
                        .description("morning")
                        .otherLocationDescription("town hall");

        // When
        ApplicationListPage result = service.getPage(filter, pageable);

        // Then
        verify(repository)
                .findAllByFilter(
                        eq(ApplicationListStatus.OPEN),
                        isNull(),
                        eq(cja),
                        eq(DEFAULT_DATE),
                        eq(time),
                        eq(expectedEndTime),
                        eq(true),
                        eq("morning"),
                        eq("town hall"),
                        eq(pageable));
    }

    @Test
    void get_returnsDto() {
        ApplicationList saved = new ApplicationList();
        UUID id = UUID.randomUUID();
        when(repository.findByUuid(id)).thenReturn(Optional.of(saved));

        Pageable pageable = mock(Pageable.class);

        mockFindSummariesById(id, pageable);

        ApplicationListGetDetailDto expected = new ApplicationListGetDetailDto();
        when(mapper.toGetDetailDto(saved, saved.getCja(), 0L)).thenReturn(expected);

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
        // Given
        UUID id = UUID.randomUUID();
        ApplicationList list = new ApplicationList();

        when(repository.findByUuid(id)).thenReturn(Optional.of(list));

        // 1) Entry projections for the list (single query)
        var entryProjection =
                applicationListEntryPrintProjection()
                        .id(1L)
                        .sequenceNumber(1)
                        .applicantTitle(MR)
                        .applicantSurname(PERSON4_SURNAME)
                        .applicantForename1(PERSON4_FORENAME1)
                        .build();
        when(aleRepository.findByIdForPrinting(id)).thenReturn(List.of(entryProjection));

        // 2) Wordings (bulk)
        ApplicationListEntryResolutionPrintProjection wordingRow1 =
                mock(ApplicationListEntryResolutionPrintProjection.class);
        when(wordingRow1.getEntryId()).thenReturn(1L);
        when(wordingRow1.getWording()).thenReturn(WORDING_1);

        ApplicationListEntryResolutionPrintProjection wordingRow2 =
                mock(ApplicationListEntryResolutionPrintProjection.class);
        when(wordingRow2.getEntryId()).thenReturn(1L);
        when(wordingRow2.getWording()).thenReturn(WORDING_2);

        when(alerRepository.findByApplicationListUuidForPrinting(id))
                .thenReturn(List.of(wordingRow1, wordingRow2));

        // 3) Officials (bulk)
        ApplicationListEntryOfficialPrintProjection officialProj =
                mock(ApplicationListEntryOfficialPrintProjection.class);
        when(officialProj.getEntryId()).thenReturn(1L);

        when(aleoRepository.findByApplicationListUuidForPrinting(
                        id, OfficialTypeUtil.PRINTABLE_CODES))
                .thenReturn(List.of(officialProj));

        // Mapper stubs
        EntryGetPrintDto mappedEntryDto = new EntryGetPrintDto();
        when(entryMapper.toPrintDto(entryProjection)).thenReturn(mappedEntryDto);

        Official officialDto = new Official();
        when(officalMapper.toOfficialDto(officialProj)).thenReturn(officialDto);

        ApplicationListGetPrintDto expected = new ApplicationListGetPrintDto();
        when(mapper.toGetPrintDto(list)).thenReturn(expected);

        // When
        ApplicationListGetPrintDto actual = service.print(id);

        // Then: it should enrich the mapped entry with wordings + officials
        assertNotNull(actual);
        assertNotNull(actual.getEntries());
        assertEquals(1, actual.getEntries().size());

        EntryGetPrintDto dto = actual.getEntries().get(0);
        // same instance returned from mapper, then enriched by service
        assertSame(mappedEntryDto, dto);

        assertEquals(List.of(WORDING_1, WORDING_2), dto.getResultWordings());
        assertEquals(List.of(officialDto), dto.getOfficials());

        verify(aleRepository).findByIdForPrinting(id);
        verify(alerRepository).findByApplicationListUuidForPrinting(id);
        verify(aleoRepository)
                .findByApplicationListUuidForPrinting(id, OfficialTypeUtil.PRINTABLE_CODES);

        // And the per-entry mapper was invoked
        verify(entryMapper).toPrintDto(entryProjection);
    }

    @Test
    void print_returns404_whenApplicationListRepositoryEmpty() {
        UUID id = UUID.randomUUID();
        when(repository.findByUuid(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.print(id))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
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

    class DummyAuditOperationService implements AuditOperationService {

        @Override
        public <T, E extends Keyable> T processAudit(
                AuditOperation auditType,
                Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution,
                AuditOperationLifecycleListener... listener) {
            return processAudit(null, auditType, execution, listener);
        }

        @Override
        public <T, E extends Keyable> T processAudit(
                E oldValue,
                AuditOperation auditType,
                Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution,
                AuditOperationLifecycleListener... listener) {
            Optional<AuditableResult<T, E>> optional =
                    execution.apply(
                            new CompleteEvent(
                                    new StartEvent(
                                            AppListAuditOperation.CREATE_APP_LIST,
                                            UUID.randomUUID().toString(),
                                            null),
                                    "result",
                                    null));
            return optional.get().getResultingValue();
        }
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
}
