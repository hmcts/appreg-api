package uk.gov.hmcts.appregister.applicationentry.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validation;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryEntityMapper;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryEntityMapperImpl;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapper;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapperImpl;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForDeleteEntry;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForUpdateClosedEntry;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForUpdateEntry;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadGetEntryInList;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkActionPreviewValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkCreateApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkUpdateFeesValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.BulkUpdateOfficialsValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.CreateApplicationEntryValidationSuccess;
import uk.gov.hmcts.appregister.applicationentry.validator.CreateApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.DeleteApplicationListEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.DeleteEntryValidationSuccess;
import uk.gov.hmcts.appregister.applicationentry.validator.GetApplicationEntryFromClosedListValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.GetApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.GetApplicationListEntriesValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.GetEntryValidationSuccess;
import uk.gov.hmcts.appregister.applicationentry.validator.UpdateApplicationEntryClosedValidationSuccess;
import uk.gov.hmcts.appregister.applicationentry.validator.UpdateApplicationEntryValidationSuccess;
import uk.gov.hmcts.appregister.applicationentry.validator.UpdateApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.UpdateClosedApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationfee.service.ApplicationFeeService;
import uk.gov.hmcts.appregister.applicationlist.audit.AppListAuditOperation;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.applicationlist.model.MoveEntriesPayload;
import uk.gov.hmcts.appregister.applicationlist.validator.MoveEntriesValidationSuccess;
import uk.gov.hmcts.appregister.applicationlist.validator.MoveEntriesValidator;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.event.StartEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.async.exception.JobError;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.concurrency.MatchProvider;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.concurrency.MatchService;
import uk.gov.hmcts.appregister.common.concurrency.MatchServiceImpl;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.AppListEntrySequenceMapping;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.AsyncJobsAppListEntry;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeStatusRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryOfficialRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntrySequenceMappingRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AsyncJobAppListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NameAddressRepository;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapper;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapperImpl;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryGetSummaryProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryResolutionProjection;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.common.template.SubstitutedSentence;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.data.AppListEntryFeeStatusTestData;
import uk.gov.hmcts.appregister.data.AppListEntryOfficialTestData;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.data.FeeTestData;
import uk.gov.hmcts.appregister.data.NameAddressTestData;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.BulkActionPreviewRequestDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionPreviewResponseDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionSelectionDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionSelectionType;
import uk.gov.hmcts.appregister.generated.model.BulkActionType;
import uk.gov.hmcts.appregister.generated.model.BulkFeeDetailsDto;
import uk.gov.hmcts.appregister.generated.model.BulkFeesUpdateDto;
import uk.gov.hmcts.appregister.generated.model.BulkOfficialsUpdateDto;
import uk.gov.hmcts.appregister.generated.model.BulkUpdateResponseDto;
import uk.gov.hmcts.appregister.generated.model.EntryApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.EntryIdsDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.EntryUpdateClosedDto;
import uk.gov.hmcts.appregister.generated.model.EntryUpdateDto;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.OfficialType;
import uk.gov.hmcts.appregister.generated.model.PaymentStatus;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;
import uk.gov.hmcts.appregister.job.validator.JobExistanceValidator;
import uk.gov.hmcts.appregister.job.validator.JobSuccess;

@Slf4j
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicationEntryServiceImplTest {

    private static final String BULK_FEE_UPDATE_REQUESTS_METRIC =
            "appregister.application_entry.bulk_fee_update.requests";
    private static final String BULK_FEE_UPDATE_ENTRIES_METRIC =
            "appregister.application_entry.bulk_fee_update.entries";
    private static final String BULK_FEE_UPDATE_DURATION_METRIC =
            "appregister.application_entry.bulk_fee_update.duration";
    private static final String METRIC_STATUS_TAG = "status";
    private static final Instant FIXED_INSTANT = Instant.parse("2025-10-07T10:15:30Z");
    private static final LocalDate CURRENT_BUSINESS_DATE = LocalDate.of(2025, Month.OCTOBER, 7);

    @Mock private FeeRepository feeRepository;

    @Mock private ApplicationListRepository applicationListRepository;

    @Mock private ApplicationCodeRepository applicationCodeRepository;

    @Mock private ApplicationListEntryRepository applicationListEntryRepository;

    @Mock private StandardApplicantRepository standardApplicantRepository;

    @Mock private AppListEntryFeeStatusRepository appListEntryFeeStatusRepository;

    @Mock private NameAddressRepository nameAddressRepository;

    @Mock private AppListEntryOfficialRepository appListEntryOfficialRepository;

    @Mock private AppListEntryFeeRepository appListEntryFeeRepository;

    @Mock private AppListEntrySequenceMappingRepository appListEntrySequenceMappingRepository;

    @Mock private AsyncJobAppListEntryRepository asyncJobAppListEntryRepository;

    @Mock private Clock clock;

    @Mock private JobExistanceValidator jobExistanceValidator;

    @Mock private JdbcTemplate template;

    private CreateApplicationEntryValidationSuccess success;

    private UpdateApplicationEntryValidationSuccess updateSuccess;

    private GetEntryValidationSuccess getEntryValidationSuccess;

    // A null match provider that returns a null etag
    private static MatchProvider nullMatchProvider =
            new MatchProvider() {
                @Override
                public String getEtag() {
                    return null;
                }
            };

    // Services
    @Spy private MatchService matchService = new MatchServiceImpl(nullMatchProvider);

    // Audit
    @Spy
    private final AuditOperationService auditOperationService = new DummyAuditOperationService();

    @Mock private ApplicationListEntryMapper applicationListEntryMapStructMapper;

    @Mock private ApplicationListEntryEntityMapper applicationListEntryEntityMapper;

    @Mock private List<AuditOperationLifecycleListener> auditLifecycleListeners;

    @Mock private EntityManager entityManager;

    @Mock private ApplicantMapper applicantMapper;
    @Mock private BusinessDateProvider businessDateProvider;

    @Mock private ApplicationFeeService feeService;

    @Mock private UserProvider userProvider;

    private ApplicationEntryService service;

    @Spy
    private DummyCreateApplicationEntryValidator createApplicationEntryValidator =
            new DummyCreateApplicationEntryValidator(
                    applicationListRepository,
                    applicationCodeRepository,
                    feeService,
                    businessDateProvider,
                    standardApplicantRepository);

    @Spy
    private DummyBulkCreateApplicationEntryValidator bulkCreateApplicationEntryValidator =
            new DummyBulkCreateApplicationEntryValidator(
                    applicationListRepository,
                    applicationCodeRepository,
                    feeService,
                    businessDateProvider,
                    standardApplicantRepository);

    @Spy
    private DummyMoveEntriesValidator moveEntriesValidator =
            new DummyMoveEntriesValidator(applicationListRepository);

    private BulkUpdateOfficialsValidator bulkUpdateOfficialsValidator;

    private BulkUpdateFeesValidator bulkUpdateFeesValidator;
    private BulkActionPreviewValidator bulkActionPreviewValidator;
    private SimpleMeterRegistry meterRegistry;

    @Spy
    private final ApplicationListEntryEntityMapper entryEntityMapper =
            new ApplicationListEntryEntityMapperImpl();

    @Spy private final PageMapper pageMapper = new PageMapper();

    private PageableMapper pageableMapper;

    @Spy
    private DummyUpdateApplicationEntryValidator updateApplicationEntryValidator =
            new DummyUpdateApplicationEntryValidator(
                    applicationListRepository,
                    applicationCodeRepository,
                    feeService,
                    businessDateProvider,
                    standardApplicantRepository,
                    applicationListEntryRepository,
                    appListEntryFeeStatusRepository);

    @Spy
    private DummyUpdateClosedEntriesValidator updateClosedEntriesValidator =
            new DummyUpdateClosedEntriesValidator(
                    applicationListRepository, applicationListEntryRepository);

    @Spy
    private GetApplicationEntryValidator getEntryValidator =
            new DummyGetApplicationEntryValidator(
                    applicationListRepository, applicationListEntryRepository);

    @Spy
    private GetApplicationEntryFromClosedListValidator getEntryFromClosedListValidator =
            new DummyGetApplicationEntryFromClosedListValidator(
                    applicationListRepository, applicationListEntryRepository);

    @Spy
    private GetApplicationListEntriesValidator getApplicationListEntriesValidator =
            new DummyGetApplicationListEntriesValidator(applicationListRepository);

    @Spy
    private DummyDeleteEntryValidator deleteEntryValidator =
            new DummyDeleteEntryValidator(
                    applicationListRepository, applicationListEntryRepository);

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(businessDateProvider.currentUkDate()).thenReturn(CURRENT_BUSINESS_DATE);
        bulkUpdateOfficialsValidator =
                new BulkUpdateOfficialsValidator(
                        applicationListRepository, applicationListEntryRepository);
        bulkUpdateFeesValidator =
                new BulkUpdateFeesValidator(
                        applicationListRepository,
                        applicationListEntryRepository,
                        businessDateProvider,
                        Validation.buildDefaultValidatorFactory().getValidator());
        bulkActionPreviewValidator = new BulkActionPreviewValidator();
        pageableMapper = new PageableMapper();
        pageableMapper.setDefaultPageSize(10);
        pageableMapper.setMaxPageSize(100);
        meterRegistry = new SimpleMeterRegistry();

        Fee fee = new FeeTestData().someComplete();
        fee.setId(-1L);
        fee.setOffsite(true);

        service =
                new ApplicationEntryServiceImpl(
                        applicationListEntryRepository,
                        feeRepository,
                        pageMapper,
                        pageableMapper,
                        createApplicationEntryValidator,
                        bulkCreateApplicationEntryValidator,
                        bulkActionPreviewValidator,
                        updateApplicationEntryValidator,
                        updateClosedEntriesValidator,
                        moveEntriesValidator,
                        bulkUpdateOfficialsValidator,
                        bulkUpdateFeesValidator,
                        matchService,
                        auditOperationService,
                        appListEntryFeeStatusRepository,
                        nameAddressRepository,
                        appListEntryOfficialRepository,
                        appListEntryFeeRepository,
                        standardApplicantRepository,
                        appListEntrySequenceMappingRepository,
                        asyncJobAppListEntryRepository,
                        applicationListEntryMapStructMapper,
                        applicantMapper,
                        applicationListEntryEntityMapper,
                        entityManager,
                        getEntryValidator,
                        getEntryFromClosedListValidator,
                        getApplicationListEntriesValidator,
                        clock,
                        businessDateProvider,
                        deleteEntryValidator,
                        meterRegistry,
                        jobExistanceValidator,
                        template,
                        userProvider
                        );
    }

    @Test
    void testSearchForGetSummary() {
        ApplicationListEntryMapper mapStructMapper = new ApplicationListEntryMapperImpl();
        mapStructMapper.setApplicantMapper(new ApplicantMapperImpl());
        service =
                new ApplicationEntryServiceImpl(
                        applicationListEntryRepository,
                        feeRepository,
                        pageMapper,
                        pageableMapper,
                        createApplicationEntryValidator,
                        bulkCreateApplicationEntryValidator,
                        bulkActionPreviewValidator,
                        updateApplicationEntryValidator,
                        updateClosedEntriesValidator,
                        moveEntriesValidator,
                        bulkUpdateOfficialsValidator,
                        bulkUpdateFeesValidator,
                        matchService,
                        auditOperationService,
                        appListEntryFeeStatusRepository,
                        nameAddressRepository,
                        appListEntryOfficialRepository,
                        appListEntryFeeRepository,
                        standardApplicantRepository,
                        appListEntrySequenceMappingRepository,
                        asyncJobAppListEntryRepository,
                        mapStructMapper,
                        applicantMapper,
                        applicationListEntryEntityMapper,
                        entityManager,
                        getEntryValidator,
                        getEntryFromClosedListValidator,
                        getApplicationListEntriesValidator,
                        clock,
                        businessDateProvider,
                        deleteEntryValidator,
                        meterRegistry,
                        jobExistanceValidator,
                        template,
                        userProvider);

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        EntryGetFilterDto entryGetFilterDto =
                Instancio.of(EntryGetFilterDto.class).withSettings(settings).create();
        ApplicationListEntryGetSummaryProjection applicationListEntryGetSummaryProjection =
                mock(ApplicationListEntryGetSummaryProjection.class);

        when(applicationListEntryGetSummaryProjection.getApplicationOrganisation())
                .thenReturn("org1");
        when(applicationListEntryGetSummaryProjection.getApplicantSurname()).thenReturn("surname");
        when(applicationListEntryGetSummaryProjection.getAnameAddress())
                .thenReturn(new NameAddress());
        when(applicationListEntryGetSummaryProjection.getRnameAddress())
                .thenReturn(new NameAddress());
        when(applicationListEntryGetSummaryProjection.getDateOfAl())
                .thenReturn(CURRENT_BUSINESS_DATE);

        when(applicationListEntryGetSummaryProjection.getAccountReference()).thenReturn("accref");
        when(applicationListEntryGetSummaryProjection.getCjaCode()).thenReturn("cjacode");
        when(applicationListEntryGetSummaryProjection.getCourtCode()).thenReturn("courtcode");
        when(applicationListEntryGetSummaryProjection.getLegislation()).thenReturn("leg");
        when(applicationListEntryGetSummaryProjection.getTitle()).thenReturn("title");

        when(applicationListEntryGetSummaryProjection.getRespondentSurname())
                .thenReturn("ressurname");
        when(applicationListEntryGetSummaryProjection.getFeeRequired()).thenReturn(YesOrNo.NO);
        when(applicationListEntryGetSummaryProjection.getStatus()).thenReturn(Status.OPEN);

        Pageable mockPage = mock(Pageable.class);
        when(mockPage.getPageNumber()).thenReturn(1);

        Page<ApplicationListEntryGetSummaryProjection> page =
                new PageImpl<ApplicationListEntryGetSummaryProjection>(
                        List.of(applicationListEntryGetSummaryProjection), mockPage, 1);

        when(applicationListEntryMapStructMapper.toStatus(entryGetFilterDto.getStatus()))
                .thenReturn(Status.OPEN);
        when(applicationListEntryRepository.searchForGetSummary(
                        null,
                        true,
                        entryGetFilterDto.getDate(),
                        entryGetFilterDto.getCourtCode(),
                        entryGetFilterDto.getOtherLocationDescription(),
                        entryGetFilterDto.getCjaCode(),
                        entryGetFilterDto.getApplicantOrganisation(),
                        entryGetFilterDto.getApplicantSurname(),
                        entryGetFilterDto.getApplicantName(),
                        entryGetFilterDto.getStandardApplicantCode(),
                        Status.fromValue(entryGetFilterDto.getStatus().getValue()),
                        entryGetFilterDto.getRespondentOrganisation(),
                        entryGetFilterDto.getRespondentSurname(),
                        entryGetFilterDto.getRespondentName(),
                        entryGetFilterDto.getRespondentPostcode(),
                        entryGetFilterDto.getAccountReference(),
                        entryGetFilterDto.getApplicationTitle(),
                        null,
                        null,
                        null,
                        mockPage))
                .thenReturn(page);

        PagingWrapper wrapper = PagingWrapper.of(List.of(), mockPage);
        // execute
        EntryPage entryPage = service.search(entryGetFilterDto, wrapper);

        // assert
        Assertions.assertEquals(1, entryPage.getContent().size());
        Assertions.assertEquals(
                ApplicationListStatus.OPEN, entryPage.getContent().get(0).getStatus());
        Assertions.assertEquals("leg", entryPage.getContent().get(0).getLegislation());
        Assertions.assertEquals("title", entryPage.getContent().get(0).getApplicationTitle());

        Assertions.assertNotNull(entryPage.getContent().get(0).getApplicant());
        Assertions.assertNotNull(entryPage.getContent().get(0).getRespondent());
    }

    @Test
    void testSearchReturnsAllResultCodes() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        EntryGetFilterDto filterDto =
                Instancio.of(EntryGetFilterDto.class).withSettings(settings).create();
        filterDto.setStatus(ApplicationListStatus.OPEN);

        ApplicationListEntryGetSummaryProjection projection =
                mock(ApplicationListEntryGetSummaryProjection.class);

        Long entryId = 1L;
        when(projection.getId()).thenReturn(entryId);
        when(projection.getApplicationOrganisation()).thenReturn("org1");
        when(projection.getApplicantSurname()).thenReturn("surname");
        when(projection.getAnameAddress()).thenReturn(new NameAddress());
        when(projection.getRnameAddress()).thenReturn(new NameAddress());
        when(projection.getDateOfAl()).thenReturn(CURRENT_BUSINESS_DATE);
        when(projection.getAccountReference()).thenReturn("accref");
        when(projection.getCjaCode()).thenReturn("cjacode");
        when(projection.getCourtCode()).thenReturn("courtcode");
        when(projection.getLegislation()).thenReturn("leg");
        when(projection.getTitle()).thenReturn("title");
        when(projection.getRespondentSurname()).thenReturn("ressurname");
        when(projection.getFeeRequired()).thenReturn(YesOrNo.NO);
        when(projection.getStatus()).thenReturn(Status.OPEN);

        Pageable mockPage = mock(Pageable.class);
        when(mockPage.getPageNumber()).thenReturn(0);

        Page<ApplicationListEntryGetSummaryProjection> resultPage =
                new PageImpl<>(List.of(projection), mockPage, 1);

        when(applicationListEntryMapStructMapper.toStatus(ApplicationListStatus.OPEN))
                .thenReturn(Status.OPEN);

        when(applicationListEntryRepository.searchForGetSummary(
                        any(),
                        anyBoolean(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(Pageable.class)))
                .thenReturn(resultPage);

        EntryGetSummaryDto summaryDto = new EntryGetSummaryDto();
        summaryDto.setResulted(new ArrayList<>());
        summaryDto.setIsResulted(false);
        when(applicationListEntryMapStructMapper.toEntrySummary(any())).thenReturn(summaryDto);

        ApplicationListEntryResolutionProjection resolution1 =
                mock(ApplicationListEntryResolutionProjection.class);
        ApplicationListEntryResolutionProjection resolution2 =
                mock(ApplicationListEntryResolutionProjection.class);

        when(resolution1.getEntryId()).thenReturn(entryId);
        when(resolution2.getEntryId()).thenReturn(entryId);

        when(resolution1.getResolutionCode()).thenReturn(mock(ResolutionCode.class));
        when(resolution2.getResolutionCode()).thenReturn(mock(ResolutionCode.class));

        when(applicationListEntryRepository.findResolutionCodesByEntryIds(anyList()))
                .thenReturn(List.of(resolution1, resolution2));

        when(applicationListEntryMapStructMapper.toResultCodeGetSummaryDto(any()))
                .thenReturn(new ResultCodeGetSummaryDto());

        PagingWrapper wrapper = PagingWrapper.of(List.of(), mockPage);

        EntryPage response = service.search(filterDto, wrapper);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(1, response.getContent().size());
        Assertions.assertTrue(response.getContent().getFirst().getIsResulted());
        Assertions.assertEquals(2, response.getContent().getFirst().getResulted().size());
    }

    @Test
    void testSearch_emptyEntries_returnsEmptyContentList() {
        EntryGetFilterDto filterDto = new EntryGetFilterDto();
        filterDto.setStatus(ApplicationListStatus.OPEN);

        Pageable mockPage = mock(Pageable.class);
        when(mockPage.getPageNumber()).thenReturn(0);
        PagingWrapper wrapper = PagingWrapper.of(List.of(), mockPage);

        Page<ApplicationListEntryGetSummaryProjection> resultPage =
                new PageImpl<>(List.of(), mockPage, 0);

        when(applicationListEntryMapStructMapper.toStatus(ApplicationListStatus.OPEN))
                .thenReturn(Status.OPEN);

        when(applicationListEntryRepository.searchForGetSummary(
                        isNull(),
                        anyBoolean(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        isNull(),
                        isNull(),
                        isNull(),
                        any(Pageable.class)))
                .thenReturn(resultPage);

        EntryPage response = service.search(filterDto, wrapper);

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getContent());
        Assertions.assertEquals(0, response.getContent().size());
    }

    @Test
    void testCreateApplicationEntry() {

        AppListTestData appListTestData = new AppListTestData();
        ApplicationCodeTestData applicationCodeTestData = new ApplicationCodeTestData();

        AppListEntryTestData appListEntryTestData = new AppListEntryTestData();

        ApplicationList appList = appListTestData.someComplete();
        ApplicationListEntry applicationListEntry = appListEntryTestData.someComplete();
        ApplicationCode code = applicationCodeTestData.someComplete();

        applicationListEntry.setId(-1L);
        appList.setId(-1L);
        code.setId(-1L);

        StandardApplicantTestData standardApplicantTestData = new StandardApplicantTestData();

        StandardApplicant sa = standardApplicantTestData.someComplete();

        sa.setId(-1L);

        FeeTestData feeTestData = new FeeTestData();
        Fee fee = feeTestData.someComplete();
        fee.setOffsite(false);
        fee.setId(-2L);

        FeeTestData feeTestDataOffsite = new FeeTestData();
        Fee feeOffsite = feeTestDataOffsite.someComplete();
        feeOffsite.setOffsite(true);
        feeOffsite.setId(-3L);

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();
        entryCreateDto.setHasOffsiteFee(true);

        AppListEntryFeeStatusTestData appListEntryFeeStatusTestData =
                new AppListEntryFeeStatusTestData();
        List<AppListEntryFeeStatus> statusLst = new ArrayList<>();
        List<AppListEntryOfficial> officialLst = new ArrayList<>();

        // generate fees for each of payload fee
        for (FeeStatus feeStatus : entryCreateDto.getFeeStatuses()) {
            AppListEntryFeeStatus appStatus = appListEntryFeeStatusTestData.someComplete();

            when(applicationListEntryEntityMapper.toFeeStatus(feeStatus, applicationListEntry))
                    .thenReturn(appStatus);

            appStatus.setId(-1L);
            when(appListEntryFeeStatusRepository.save(appStatus)).thenReturn(appStatus);
            statusLst.add(appStatus);
        }

        AppListEntryOfficialTestData officialTestData = new AppListEntryOfficialTestData();

        // generate official for each of payload fee
        for (Official appOfficial : entryCreateDto.getOfficials()) {
            AppListEntryOfficial official = officialTestData.someComplete();

            when(applicationListEntryEntityMapper.toOfficial(appOfficial, applicationListEntry))
                    .thenReturn(official);

            official.setId(-1L);
            when(appListEntryOfficialRepository.save(official)).thenReturn(official);
            officialLst.add(official);
        }

        TemplateSubstitution templateSubstitution = new TemplateSubstitution();
        templateSubstitution.setKey("Applicant officer");
        templateSubstitution.setValue("off");

        TemplateSubstitution templateSubstitution2 = new TemplateSubstitution();
        templateSubstitution2.setKey("Applicant officer1");
        templateSubstitution2.setValue("off1");

        TemplateSubstitution templateSubstitution3 = new TemplateSubstitution();
        templateSubstitution3.setKey("Applicant officer2");
        templateSubstitution3.setValue("off2");

        entryCreateDto.setWordingFields(
                List.of(templateSubstitution, templateSubstitution2, templateSubstitution3));
        code.setFeeReference("CO1.1");
        code.setWording(
                "Test template {TEXT|Applicant officer|10} and second template {TEXT|Applicant officer1|10} and third"
                        + "template {TEXT|Applicant officer2|10}");

        NameAddressTestData nameAddressTestData = new NameAddressTestData();

        NameAddress applicant = nameAddressTestData.somePerson();
        NameAddress respondent = nameAddressTestData.someOrganisation();

        when(applicationCodeRepository.findByCodeAndDate(
                        eq(entryCreateDto.getApplicationCode()), notNull()))
                .thenReturn(List.of(code));
        when(applicationListEntryEntityMapper.toApplicationListEntry(
                        eq(entryCreateDto),
                        notNull(),
                        eq(sa),
                        eq(applicant),
                        eq(respondent),
                        eq(code),
                        eq(appList),
                        eq(YesOrNo.NO)))
                .thenReturn(applicationListEntry);

        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(UUID.randomUUID())
                        .data(entryCreateDto)
                        .build();

        when(applicationListRepository.findByUuid(payload.getId()))
                .thenReturn(Optional.of(appList));
        when(applicantMapper.toApplicant(entryCreateDto.getApplicant())).thenReturn(applicant);

        when(applicantMapper.toRespondent(entryCreateDto.getRespondent())).thenReturn(respondent);

        when(nameAddressRepository.save(applicant)).thenReturn(applicant);
        when(nameAddressRepository.save(respondent)).thenReturn(respondent);
        when(applicationListEntryRepository.save(applicationListEntry))
                .thenReturn(applicationListEntry);

        FeePair pair = new FeePair(fee, feeOffsite);

        // setup validation success response containing all validated data
        success =
                CreateApplicationEntryValidationSuccess.builder()
                        .wordingSentence(WordingTemplateSentence.with(code.getWording()))
                        .fee(pair)
                        .applicationCode(code)
                        .sa(sa)
                        .applicationList(appList)
                        .build();

        AppListEntryFeeId appListFee = new AppListEntryFeeId();
        appListFee.setAppListEntryId(applicationListEntry.getId());
        appListFee.setFeeId(pair.mainFee().getId());

        AppListEntryFeeId offsiteAppListFee = new AppListEntryFeeId();
        offsiteAppListFee.setAppListEntryId(applicationListEntry.getId());
        offsiteAppListFee.setFeeId(pair.offsiteFee().getId());

        when(appListEntryFeeRepository.save(appListFee)).thenReturn(appListFee);
        when(appListEntryFeeRepository.save(offsiteAppListFee)).thenReturn(appListFee);

        // dummy the mapping of the response

        EntryGetDetailDto entryGetDetailDto =
                Instancio.of(EntryGetDetailDto.class).withSettings(settings).create();
        when(applicationListEntryMapStructMapper.toEntryGetDetailDto(
                        applicationListEntry, statusLst, pair, officialLst, sa))
                .thenReturn(entryGetDetailDto);

        // run the test
        var response = service.createEntry(payload);

        ArgumentCaptor<AppListEntryFeeId> captor = ArgumentCaptor.forClass(AppListEntryFeeId.class);
        verify(appListEntryFeeRepository, times(2)).save(captor.capture());

        // now assert the response is mapped correctly
        Assertions.assertEquals(entryGetDetailDto, response.getPayload());
        Assertions.assertNotNull(response.getEtag());

        ArgumentCaptor<NameAddress> appCaptorName = ArgumentCaptor.forClass(NameAddress.class);

        // verify that the applicant and respondent are saved
        verify(nameAddressRepository, times(2)).save(appCaptorName.capture());

        // verify app list entry is saved
        ArgumentCaptor<ApplicationListEntry> appListEntryCaptor =
                ArgumentCaptor.forClass(ApplicationListEntry.class);

        verify(applicationListEntryRepository).save(appListEntryCaptor.capture());
        Assertions.assertEquals(applicationListEntry, appListEntryCaptor.getValue());

        // verify that the fee status is saved
        ArgumentCaptor<AppListEntryFeeStatus> appListStatusCaptor =
                ArgumentCaptor.forClass(AppListEntryFeeStatus.class);
        verify(appListEntryFeeStatusRepository, times(entryCreateDto.getFeeStatuses().size()))
                .save(appListStatusCaptor.capture());

        // verify that the official is saved
        ArgumentCaptor<AppListEntryOfficial> appListOfficialCaptor =
                ArgumentCaptor.forClass(AppListEntryOfficial.class);
        verify(appListEntryOfficialRepository, times(entryCreateDto.getOfficials().size()))
                .save(appListOfficialCaptor.capture());

        Assertions.assertEquals(-1, captor.getAllValues().get(0).getAppListEntryId());
        Assertions.assertEquals(appListFee.getFeeId(), captor.getAllValues().get(0).getFeeId());

        Assertions.assertEquals(-1, captor.getAllValues().get(1).getAppListEntryId());
        Assertions.assertEquals(
                offsiteAppListFee.getFeeId(), captor.getAllValues().get(1).getFeeId());

        Assertions.assertEquals(applicant, appCaptorName.getAllValues().get(0));
        Assertions.assertEquals(respondent, appCaptorName.getAllValues().get(1));

        for (int i = 0; i < statusLst.size(); i++) {
            Assertions.assertEquals(statusLst.get(i), appListStatusCaptor.getAllValues().get(i));
        }

        for (int i = 0; i < officialLst.size(); i++) {
            Assertions.assertEquals(
                    officialLst.get(i), appListOfficialCaptor.getAllValues().get(i));
        }
    }

    @Test
    void testBulkCreateCreatesInitialFeeStatusWhenFeeResolved() {
        ApplicationList appList = new ApplicationList();
        appList.setId(1L);

        ApplicationListEntry applicationListEntry = new ApplicationListEntry();
        applicationListEntry.setId(2L);
        applicationListEntry.setUuid(UUID.randomUUID());
        applicationListEntry.setVersion(1L);

        ApplicationCode code = new ApplicationCode();
        code.setId(3L);
        code.setCode("AD99001");
        code.setWording("Request to copy documents");

        EntryCreateDto entryCreateDto = new EntryCreateDto();
        entryCreateDto.setApplicationCode("AD99001");
        entryCreateDto.setStandardApplicantCode("APP001");
        entryCreateDto.setWordingFields(List.of());
        entryCreateDto.setFeeStatuses(null);
        entryCreateDto.setOfficials(null);
        entryCreateDto.setHasOffsiteFee(false);

        Fee fee = new Fee();
        fee.setId(4L);
        fee.setVersion(1L);
        FeePair pair = new FeePair(fee, null);
        StandardApplicant sa = new StandardApplicant();

        success =
                CreateApplicationEntryValidationSuccess.builder()
                        .wordingSentence(WordingTemplateSentence.with(code.getWording()))
                        .fee(pair)
                        .applicationCode(code)
                        .sa(sa)
                        .applicationList(appList)
                        .build();

        when(applicationListEntryEntityMapper.toApplicationListEntry(
                        eq(entryCreateDto),
                        eq(code.getWording()),
                        eq(sa),
                        isNull(),
                        isNull(),
                        eq(code),
                        eq(appList),
                        eq(YesOrNo.YES)))
                .thenReturn(applicationListEntry);
        when(applicationListEntryRepository.save(applicationListEntry))
                .thenReturn(applicationListEntry);
        when(appListEntrySequenceMappingRepository.findByAlIdForUpdate(appList.getId()))
                .thenReturn(Optional.empty());
        when(appListEntrySequenceMappingRepository.save(any(AppListEntrySequenceMapping.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(appListEntryFeeStatusRepository.save(any(AppListEntryFeeStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(appListEntryFeeRepository.save(any(AppListEntryFeeId.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EntryGetDetailDto entryGetDetailDto = new EntryGetDetailDto();
        when(applicationListEntryMapStructMapper.toEntryGetDetailDto(
                        eq(applicationListEntry), anyList(), eq(pair), anyList(), eq(sa)))
                .thenReturn(entryGetDetailDto);
        when(appListEntryOfficialRepository.getOfficialByEntryUuid(applicationListEntry.getUuid()))
                .thenReturn(List.of());
        when(appListEntryFeeStatusRepository.getFeeStatusByEntryUuid(
                        applicationListEntry.getUuid()))
                .thenReturn(List.of());
        when(appListEntryFeeRepository.getFeeForEntryId(applicationListEntry.getId()))
                .thenReturn(List.of(fee));

        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(UUID.randomUUID())
                        .data(entryCreateDto)
                        .build();

        MatchResponse<EntryGetDetailDto> response = service.createBulkEntry(payload);

        Assertions.assertEquals(entryGetDetailDto, response.getPayload());

        ArgumentCaptor<AppListEntryFeeStatus> feeStatusCaptor =
                ArgumentCaptor.forClass(AppListEntryFeeStatus.class);
        verify(appListEntryFeeStatusRepository).save(feeStatusCaptor.capture());

        AppListEntryFeeStatus savedFeeStatus = feeStatusCaptor.getValue();
        Assertions.assertEquals(applicationListEntry, savedFeeStatus.getAppListEntry());
        Assertions.assertEquals(FeeStatusType.DUE, savedFeeStatus.getAlefsFeeStatus());
        Assertions.assertNull(savedFeeStatus.getAlefsPaymentReference());
        Assertions.assertEquals(
                LocalDate.of(2025, Month.OCTOBER, 7), savedFeeStatus.getAlefsFeeStatusDate());
        Assertions.assertNotNull(savedFeeStatus.getAlefsStatusCreationDate());
    }

    @Test
    void testCreateEntryAllocatesSequenceWhenNoMapping() {
        AppListTestData appListTestData = new AppListTestData();
        ApplicationCodeTestData applicationCodeTestData = new ApplicationCodeTestData();
        AppListEntryTestData appListEntryTestData = new AppListEntryTestData();

        ApplicationList appList = appListTestData.someComplete();
        ApplicationListEntry applicationListEntry = appListEntryTestData.someComplete();
        ApplicationCode code = applicationCodeTestData.someComplete();

        applicationListEntry.setId(1L);
        appList.setId(1L);
        code.setId(1L);

        StandardApplicantTestData standardApplicantTestData = new StandardApplicantTestData();
        StandardApplicant sa = standardApplicantTestData.someComplete();
        sa.setId(1L);

        FeeTestData feeTestData = new FeeTestData();
        Fee fee = feeTestData.someComplete();
        fee.setOffsite(false);
        fee.setId(2L);

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();

        AppListEntryFeeStatusTestData appListEntryFeeStatusTestData =
                new AppListEntryFeeStatusTestData();
        for (FeeStatus feeStatus : entryCreateDto.getFeeStatuses()) {
            AppListEntryFeeStatus appStatus = appListEntryFeeStatusTestData.someComplete();
            when(applicationListEntryEntityMapper.toFeeStatus(feeStatus, applicationListEntry))
                    .thenReturn(appStatus);
            appStatus.setId(-1L);
            when(appListEntryFeeStatusRepository.save(appStatus)).thenReturn(appStatus);
        }

        AppListEntryOfficialTestData officialTestData = new AppListEntryOfficialTestData();
        for (Official appOfficial : entryCreateDto.getOfficials()) {
            var official = officialTestData.someComplete();
            when(applicationListEntryEntityMapper.toOfficial(appOfficial, applicationListEntry))
                    .thenReturn(official);
            official.setId(-1L);
            when(appListEntryOfficialRepository.save(official)).thenReturn(official);
        }

        Fee offsiteFee = feeTestData.someComplete();
        offsiteFee.setOffsite(true);
        offsiteFee.setId(3L);

        // wording substitution and application code lookup
        TemplateSubstitution t1 = new TemplateSubstitution();
        t1.setKey("Applicant officer");
        t1.setValue("off");
        entryCreateDto.setWordingFields(List.of(t1));
        code.setWording("Test template {TEXT|Applicant officer|10}");

        NameAddressTestData nameAddressTestData = new NameAddressTestData();
        NameAddress applicant = nameAddressTestData.somePerson();
        NameAddress respondent = nameAddressTestData.someOrganisation();

        when(applicationListEntryEntityMapper.toApplicationListEntry(
                        eq(entryCreateDto),
                        notNull(),
                        eq(sa),
                        eq(applicant),
                        eq(respondent),
                        eq(code),
                        eq(appList),
                        eq(YesOrNo.NO)))
                .thenReturn(applicationListEntry);

        when(applicantMapper.toApplicant(entryCreateDto.getApplicant())).thenReturn(applicant);
        when(applicantMapper.toRespondent(entryCreateDto.getRespondent())).thenReturn(respondent);
        when(nameAddressRepository.save(respondent)).thenReturn(respondent);
        when(applicationListEntryRepository.save(applicationListEntry))
                .thenReturn(applicationListEntry);

        FeePair pair = new FeePair(new Fee(), new Fee());

        success =
                CreateApplicationEntryValidationSuccess.builder()
                        .wordingSentence(WordingTemplateSentence.with(code.getWording()))
                        .fee(pair)
                        .applicationCode(code)
                        .sa(sa)
                        .applicationList(appList)
                        .build();

        EntryGetDetailDto entryGetDetailDto =
                Instancio.of(EntryGetDetailDto.class).withSettings(settings).create();
        when(applicationListEntryMapStructMapper.toEntryGetDetailDto(
                        eq(applicationListEntry), anyList(), eq(pair), anyList(), eq(sa)))
                .thenReturn(entryGetDetailDto);

        // simulate no existing mapping
        Long alId = appList.getId();
        when(appListEntrySequenceMappingRepository.findById(alId)).thenReturn(Optional.empty());

        // capture mapping saved
        ArgumentCaptor<AppListEntrySequenceMapping> mappingCaptor =
                ArgumentCaptor.forClass(AppListEntrySequenceMapping.class);
        when(appListEntrySequenceMappingRepository.save(mappingCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(UUID.randomUUID())
                        .data(entryCreateDto)
                        .build();

        // run
        service.createEntry(payload);

        // assertions

        // application list entry saved and sequence set to 1
        ArgumentCaptor<ApplicationListEntry> appListEntryCaptor =
                ArgumentCaptor.forClass(ApplicationListEntry.class);
        verify(applicationListEntryRepository).save(appListEntryCaptor.capture());
        Assertions.assertEquals((short) 1, appListEntryCaptor.getValue().getSequenceNumber());

        // mapping saved with aleLastSequence == 1 and alId == alId
        AppListEntrySequenceMapping savedMapping = mappingCaptor.getValue();
        Assertions.assertEquals(alId, savedMapping.getAlId());
        Assertions.assertEquals(1, savedMapping.getAleLastSequence());
    }

    @Test
    void testCreateEntryIncrementsExistingSequenceMapping() {
        AppListTestData appListTestData = new AppListTestData();
        ApplicationCodeTestData applicationCodeTestData = new ApplicationCodeTestData();
        AppListEntryTestData appListEntryTestData = new AppListEntryTestData();

        ApplicationList appList = appListTestData.someComplete();
        ApplicationListEntry applicationListEntry = appListEntryTestData.someComplete();
        ApplicationCode code = applicationCodeTestData.someComplete();

        applicationListEntry.setId(1L);
        appList.setId(1L);
        code.setId(1L);

        StandardApplicantTestData standardApplicantTestData = new StandardApplicantTestData();
        StandardApplicant sa = standardApplicantTestData.someComplete();
        sa.setId(1L);

        FeeTestData feeTestData = new FeeTestData();
        Fee fee = feeTestData.someComplete();
        fee.setOffsite(false);
        fee.setId(2L);

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();

        AppListEntryFeeStatusTestData appListEntryFeeStatusTestData =
                new AppListEntryFeeStatusTestData();
        for (FeeStatus feeStatus : entryCreateDto.getFeeStatuses()) {
            AppListEntryFeeStatus appStatus = appListEntryFeeStatusTestData.someComplete();
            when(applicationListEntryEntityMapper.toFeeStatus(feeStatus, applicationListEntry))
                    .thenReturn(appStatus);
            appStatus.setId(-1L);
            when(appListEntryFeeStatusRepository.save(appStatus)).thenReturn(appStatus);
        }

        AppListEntryOfficialTestData officialTestData = new AppListEntryOfficialTestData();
        for (Official appOfficial : entryCreateDto.getOfficials()) {
            var official = officialTestData.someComplete();
            when(applicationListEntryEntityMapper.toOfficial(appOfficial, applicationListEntry))
                    .thenReturn(official);
            official.setId(-1L);
            when(appListEntryOfficialRepository.save(official)).thenReturn(official);
        }

        TemplateSubstitution t1 = new TemplateSubstitution();
        t1.setKey("Applicant officer");
        t1.setValue("off");
        entryCreateDto.setWordingFields(List.of(t1));
        code.setWording("Test template {TEXT|Applicant officer|10}");

        NameAddressTestData nameAddressTestData = new NameAddressTestData();
        NameAddress applicant = nameAddressTestData.somePerson();
        NameAddress respondent = nameAddressTestData.someOrganisation();

        when(applicationListEntryEntityMapper.toApplicationListEntry(
                        eq(entryCreateDto),
                        notNull(),
                        eq(sa),
                        eq(applicant),
                        eq(respondent),
                        eq(code),
                        eq(appList),
                        eq(YesOrNo.NO)))
                .thenReturn(applicationListEntry);

        when(applicantMapper.toApplicant(entryCreateDto.getApplicant())).thenReturn(applicant);
        when(applicantMapper.toRespondent(entryCreateDto.getRespondent())).thenReturn(respondent);
        when(nameAddressRepository.save(respondent)).thenReturn(respondent);
        when(applicationListEntryRepository.save(applicationListEntry))
                .thenReturn(applicationListEntry);

        FeePair pair = new FeePair(new Fee(), new Fee());

        success =
                CreateApplicationEntryValidationSuccess.builder()
                        .wordingSentence(WordingTemplateSentence.with(code.getWording()))
                        .fee(pair)
                        .applicationCode(code)
                        .sa(sa)
                        .applicationList(appList)
                        .build();

        AppListEntryFeeId appListFee = new AppListEntryFeeId();
        appListFee.setAppListEntryId(applicationListEntry.getId());
        appListFee.setFeeId(fee.getId());

        ArgumentCaptor<AppListEntryFeeId> feeIdCaptor =
                ArgumentCaptor.forClass(AppListEntryFeeId.class);
        when(appListEntryFeeRepository.save(feeIdCaptor.capture())).thenReturn(appListFee);

        EntryGetDetailDto entryGetDetailDto =
                Instancio.of(EntryGetDetailDto.class).withSettings(settings).create();
        when(applicationListEntryMapStructMapper.toEntryGetDetailDto(
                        eq(applicationListEntry), anyList(), eq(pair), anyList(), eq(sa)))
                .thenReturn(entryGetDetailDto);

        // Existing mapping scenario
        Long alId = appList.getId();
        AppListEntrySequenceMapping existing =
                AppListEntrySequenceMapping.builder().alId(alId).aleLastSequence(5).build();
        when(appListEntrySequenceMappingRepository.findByAlIdForUpdate(alId))
                .thenReturn(Optional.of(existing));

        ArgumentCaptor<AppListEntrySequenceMapping> mappingCaptor =
                ArgumentCaptor.forClass(AppListEntrySequenceMapping.class);
        when(appListEntrySequenceMappingRepository.save(mappingCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(UUID.randomUUID())
                        .data(entryCreateDto)
                        .build();

        // run
        MatchResponse<EntryGetDetailDto> response = service.createEntry(payload);

        // assertions
        Assertions.assertEquals(entryGetDetailDto, response.getPayload());
        Assertions.assertNotNull(response.getEtag());

        // application list entry saved and sequence set to 6 (5 + 1)
        ArgumentCaptor<ApplicationListEntry> appListEntryCaptor =
                ArgumentCaptor.forClass(ApplicationListEntry.class);
        verify(applicationListEntryRepository).save(appListEntryCaptor.capture());
        Assertions.assertEquals((short) 6, appListEntryCaptor.getValue().getSequenceNumber());
        Assertions.assertEquals(6, existing.getAleLastSequence());
    }

    @Test
    void testToEntryGetDetailDto() {
        ApplicationListEntry applicationListEntry = new AppListEntryTestData().someComplete();
        ApplicationList applicationList = new AppListTestData().someComplete();

        getEntryValidationSuccess =
                GetEntryValidationSuccess.builder()
                        .applicationListEntry(applicationListEntry)
                        .applicationList(applicationList)
                        .build();

        applicationListEntry.getEntryFeeIds().clear();

        // setup the fee
        Long feeId = 1L;
        AppListEntryFeeId entry = new AppListEntryFeeId();
        entry.setFeeId(feeId);
        applicationListEntry.getEntryFeeIds().add(entry);

        Fee fee = new FeeTestData().someComplete();
        fee.setOffsite(true);
        when(feeRepository.findByIdsBetweenDate(notNull(), notNull())).thenReturn(List.of(fee));

        EntryGetDetailDto entryGetDetailDto = new EntryGetDetailDto();
        when(applicationListEntryMapStructMapper.toEntryGetDetailDto(applicationListEntry, true))
                .thenReturn(entryGetDetailDto);

        PayloadGetEntryInList payload =
                PayloadGetEntryInList.builder()
                        .listId(UUID.randomUUID())
                        .entryId(UUID.randomUUID())
                        .build();

        // test
        MatchResponse<EntryGetDetailDto> matchResponse =
                service.getApplicationListEntryDetail(payload);

        // assert
        Assertions.assertEquals(entryGetDetailDto, matchResponse.getPayload());
        Assertions.assertNotNull(matchResponse.getEtag());
    }

    @Test
    void testToEntryGetDetailDtoNoFees() {
        ApplicationListEntry applicationListEntry = new AppListEntryTestData().someComplete();
        ApplicationList applicationList = new AppListTestData().someComplete();

        getEntryValidationSuccess =
                GetEntryValidationSuccess.builder()
                        .applicationListEntry(applicationListEntry)
                        .applicationList(applicationList)
                        .build();

        applicationListEntry.getEntryFeeIds().clear();

        EntryGetDetailDto entryGetDetailDto = new EntryGetDetailDto();
        when(applicationListEntryMapStructMapper.toEntryGetDetailDto(applicationListEntry, false))
                .thenReturn(entryGetDetailDto);

        PayloadGetEntryInList payload =
                PayloadGetEntryInList.builder()
                        .listId(UUID.randomUUID())
                        .entryId(UUID.randomUUID())
                        .build();

        // test
        MatchResponse<EntryGetDetailDto> matchResponse =
                service.getApplicationListEntryDetail(payload);

        // assert
        Assertions.assertEquals(entryGetDetailDto, matchResponse.getPayload());
        Assertions.assertNotNull(matchResponse.getEtag());

        // no fees were found or called for
        verify(feeRepository, never()).findByIdsBetweenDate(notNull(), notNull());
    }

    @Test
    void
            givenClosedListReadRequest_whenGetApplicationListEntryDetailFromClosedList_thenMapsEntryAndReturnsEtag() {
        ApplicationListEntry applicationListEntry = new AppListEntryTestData().someComplete();
        ApplicationList applicationList = new AppListTestData().someComplete();

        getEntryValidationSuccess =
                GetEntryValidationSuccess.builder()
                        .applicationListEntry(applicationListEntry)
                        .applicationList(applicationList)
                        .build();

        applicationListEntry.getEntryFeeIds().clear();

        EntryGetDetailDto entryGetDetailDto = new EntryGetDetailDto();
        when(applicationListEntryMapStructMapper.toEntryGetDetailDto(applicationListEntry, false))
                .thenReturn(entryGetDetailDto);

        PayloadGetEntryInList payload =
                PayloadGetEntryInList.builder()
                        .listId(UUID.randomUUID())
                        .entryId(UUID.randomUUID())
                        .build();

        MatchResponse<EntryGetDetailDto> matchResponse =
                service.getApplicationListEntryDetailFromClosedList(payload);

        Assertions.assertEquals(entryGetDetailDto, matchResponse.getPayload());
        Assertions.assertNotNull(matchResponse.getEtag());
        verify(getEntryFromClosedListValidator).validate(eq(payload), any());
        verify(getEntryValidator, never()).validate(eq(payload), any());
        verify(applicationListEntryMapStructMapper)
                .toEntryGetDetailDto(applicationListEntry, false);
    }

    @Test
    void testGetApplicationListEntries_success() {
        ApplicationList applicationList = new AppListTestData().someComplete();

        when(applicationListRepository.findByUuid(applicationList.getUuid()))
                .thenReturn(Optional.of(applicationList));

        ApplicationListEntryGetSummaryProjection applicationListEntryGetSummaryProjection =
                mock(ApplicationListEntryGetSummaryProjection.class);

        Long entryId = 1L;
        when(applicationListEntryGetSummaryProjection.getId()).thenReturn(entryId);

        when(applicationListEntryGetSummaryProjection.getApplicationOrganisation())
                .thenReturn("org1");
        when(applicationListEntryGetSummaryProjection.getApplicantSurname()).thenReturn("surname");
        when(applicationListEntryGetSummaryProjection.getAnameAddress())
                .thenReturn(new NameAddress());
        when(applicationListEntryGetSummaryProjection.getRnameAddress())
                .thenReturn(new NameAddress());
        when(applicationListEntryGetSummaryProjection.getDateOfAl())
                .thenReturn(CURRENT_BUSINESS_DATE);

        when(applicationListEntryGetSummaryProjection.getAccountReference()).thenReturn("accref");
        when(applicationListEntryGetSummaryProjection.getCjaCode()).thenReturn("cjacode");
        when(applicationListEntryGetSummaryProjection.getCourtCode()).thenReturn("courtcode");
        when(applicationListEntryGetSummaryProjection.getLegislation()).thenReturn("leg");
        when(applicationListEntryGetSummaryProjection.getTitle()).thenReturn("title");

        when(applicationListEntryGetSummaryProjection.getRespondentSurname())
                .thenReturn("ressurname");
        when(applicationListEntryGetSummaryProjection.getFeeRequired()).thenReturn(YesOrNo.NO);
        when(applicationListEntryGetSummaryProjection.getStatus()).thenReturn(Status.OPEN);

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        EntryApplicationListGetFilterDto entryGetFilterDto =
                Instancio.of(EntryApplicationListGetFilterDto.class)
                        .withSettings(settings)
                        .create();

        Pageable mockPage = mock(Pageable.class);
        when(mockPage.getPageNumber()).thenReturn(1);

        Page<ApplicationListEntryGetSummaryProjection> dbPage =
                new PageImpl<>(List.of(applicationListEntryGetSummaryProjection), mockPage, 1);

        when(applicationListEntryRepository.searchForGetSummary(
                        applicationList.getUuid(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        entryGetFilterDto.getApplicantName(),
                        null,
                        null,
                        null,
                        null,
                        entryGetFilterDto.getRespondentName(),
                        entryGetFilterDto.getRespondentPostcode(),
                        entryGetFilterDto.getAccountReference(),
                        entryGetFilterDto.getApplicationTitle(),
                        entryGetFilterDto.getResulted(),
                        entryGetFilterDto.getFeeRequired(),
                        entryGetFilterDto.getSequenceNumber(),
                        mockPage))
                .thenReturn(dbPage);

        EntryGetSummaryDto summaryDto = new EntryGetSummaryDto();
        summaryDto.setResulted(new ArrayList<>());
        summaryDto.setIsResulted(false);

        when(applicationListEntryMapStructMapper.toEntrySummary(any())).thenReturn(summaryDto);
        when(applicationListEntryMapStructMapper.toApplicationListEntry(
                        any(PayloadGetEntryInList.class),
                        any(EntryApplicationListGetFilterDto.class)))
                .thenReturn(new ApplicationListEntry());

        when(applicationListEntryMapStructMapper.toResultCodeGetSummaryDto(any()))
                .thenReturn(new ResultCodeGetSummaryDto());

        PagingWrapper wrapper = PagingWrapper.of(List.of(), mockPage);

        PayloadGetEntryInList payloadGetEntryInList =
                PayloadGetEntryInList.builder().listId(applicationList.getUuid()).build();

        ApplicationListEntryResolutionProjection resolutionProjection =
                mock(ApplicationListEntryResolutionProjection.class);

        when(resolutionProjection.getEntryId()).thenReturn(entryId);

        when(resolutionProjection.getResolutionCode()).thenReturn(mock(ResolutionCode.class));

        when(applicationListEntryRepository.findResolutionCodesByEntryIds(anyList()))
                .thenReturn(List.of(resolutionProjection));

        // test
        EntryPage response =
                service.getApplicationListEntries(
                        payloadGetEntryInList, wrapper, entryGetFilterDto);

        // assert
        Assertions.assertNotNull(response);
        Assertions.assertEquals(1, response.getContent().size());
        Assertions.assertEquals(1, response.getContent().getFirst().getResulted().size());
        Assertions.assertTrue(response.getContent().getFirst().getIsResulted());

        // The read endpoint now goes through the audit service, so the mapper must build the
        // audit surrogate from the path parameter and query-string filter.
        verify(applicationListEntryMapStructMapper)
                .toApplicationListEntry(payloadGetEntryInList, entryGetFilterDto);
    }

    @Test
    void testGetApplicationListEntryIds_success() {
        ApplicationList applicationList = new AppListTestData().someComplete();

        when(applicationListRepository.findByUuid(applicationList.getUuid()))
                .thenReturn(Optional.of(applicationList));

        EntryApplicationListGetFilterDto entryGetFilterDto = new EntryApplicationListGetFilterDto();
        entryGetFilterDto.setApplicantName("  Applicant Match  ");
        entryGetFilterDto.setAccountReference("  ACC-123  ");
        entryGetFilterDto.setResulted("  RC1  ");
        entryGetFilterDto.setSequenceNumber(7);

        List<UUID> expectedIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        when(applicationListEntryRepository.searchForGetSummaryIds(
                        applicationList.getUuid(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Applicant Match",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "ACC-123",
                        null,
                        "RC1",
                        null,
                        7))
                .thenReturn(expectedIds);

        PayloadGetEntryInList payloadGetEntryInList =
                PayloadGetEntryInList.builder().listId(applicationList.getUuid()).build();

        when(applicationListEntryMapStructMapper.toApplicationListEntry(
                        any(PayloadGetEntryInList.class),
                        any(EntryApplicationListGetFilterDto.class)))
                .thenReturn(new ApplicationListEntry());

        EntryIdsDto response =
                service.getApplicationListEntryIds(payloadGetEntryInList, entryGetFilterDto);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(expectedIds, response.getIds());

        verify(applicationListEntryMapStructMapper)
                .toApplicationListEntry(any(PayloadGetEntryInList.class), any());
    }

    @Test
    void testGetApplicationListEntries_buildsAuditEntityFromPayloadAndFilter() {
        // Arrange a simple successful search so we can focus on whether the service builds the
        // correct audit payload for the read operation.
        val applicationList = new AppListTestData().someComplete();
        when(applicationListRepository.findByUuid(applicationList.getUuid()))
                .thenReturn(Optional.of(applicationList));

        val entryGetFilterDto = new EntryApplicationListGetFilterDto();
        entryGetFilterDto.setApplicantName("Applicant Audit Org");
        entryGetFilterDto.setRespondentName("Respondent Audit Org");
        entryGetFilterDto.setRespondentPostcode("ZZ1 1ZZ");
        entryGetFilterDto.setAccountReference("ACC-123");
        entryGetFilterDto.setApplicationTitle("Read audit application title");
        entryGetFilterDto.setFeeRequired(Boolean.TRUE);
        entryGetFilterDto.setSequenceNumber(7);

        val mockPage = mock(Pageable.class);
        when(mockPage.getPageNumber()).thenReturn(0);
        val wrapper = PagingWrapper.of(List.of(), mockPage);
        val dbPage = new PageImpl<ApplicationListEntryGetSummaryProjection>(List.of(), mockPage, 0);

        when(applicationListEntryRepository.searchForGetSummary(
                        applicationList.getUuid(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        entryGetFilterDto.getApplicantName(),
                        null,
                        null,
                        null,
                        null,
                        entryGetFilterDto.getRespondentName(),
                        entryGetFilterDto.getRespondentPostcode(),
                        entryGetFilterDto.getAccountReference(),
                        entryGetFilterDto.getApplicationTitle(),
                        entryGetFilterDto.getResulted(),
                        entryGetFilterDto.getFeeRequired(),
                        entryGetFilterDto.getSequenceNumber(),
                        mockPage))
                .thenReturn(dbPage);

        val payloadGetEntryInList =
                PayloadGetEntryInList.builder().listId(applicationList.getUuid()).build();
        val auditEntity = new ApplicationListEntry();

        when(applicationListEntryMapStructMapper.toApplicationListEntry(
                        payloadGetEntryInList, entryGetFilterDto))
                .thenReturn(auditEntity);

        // Act by calling the service through the same public method the controller uses.
        val response =
                service.getApplicationListEntries(
                        payloadGetEntryInList, wrapper, entryGetFilterDto);

        // Assert the business response still comes back, and the mapper is asked for the exact
        // payload/filter pair that should be written to DATA_AUDIT.
        Assertions.assertNotNull(response);
        verify(applicationListEntryMapStructMapper)
                .toApplicationListEntry(payloadGetEntryInList, entryGetFilterDto);
    }

    @Test
    void testGetApplicationListEntries_normalisesStringFiltersBeforeSearch() {
        val applicationList = new AppListTestData().someComplete();
        when(applicationListRepository.findByUuid(applicationList.getUuid()))
                .thenReturn(Optional.of(applicationList));

        val entryGetFilterDto = new EntryApplicationListGetFilterDto();
        entryGetFilterDto.setApplicantName(" Applicant Audit Org ");
        entryGetFilterDto.setRespondentName(" Respondent Audit Org ");
        entryGetFilterDto.setRespondentPostcode(" ZZ1 1ZZ ");
        entryGetFilterDto.setAccountReference("   ");
        entryGetFilterDto.setApplicationTitle(" Read audit application title ");
        entryGetFilterDto.setResulted(" RC1 ");

        val mockPage = mock(Pageable.class);
        when(mockPage.getPageNumber()).thenReturn(0);
        val wrapper = PagingWrapper.of(List.of(), mockPage);
        val dbPage = new PageImpl<ApplicationListEntryGetSummaryProjection>(List.of(), mockPage, 0);

        when(applicationListEntryRepository.searchForGetSummary(
                        applicationList.getUuid(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Applicant Audit Org",
                        null,
                        null,
                        null,
                        null,
                        "Respondent Audit Org",
                        "ZZ1 1ZZ",
                        null,
                        "Read audit application title",
                        "RC1",
                        entryGetFilterDto.getFeeRequired(),
                        entryGetFilterDto.getSequenceNumber(),
                        mockPage))
                .thenReturn(dbPage);

        val payloadGetEntryInList =
                PayloadGetEntryInList.builder().listId(applicationList.getUuid()).build();
        val auditEntity = new ApplicationListEntry();

        when(applicationListEntryMapStructMapper.toApplicationListEntry(
                        payloadGetEntryInList, entryGetFilterDto))
                .thenReturn(auditEntity);

        val response =
                service.getApplicationListEntries(
                        payloadGetEntryInList, wrapper, entryGetFilterDto);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("Applicant Audit Org", entryGetFilterDto.getApplicantName());
        Assertions.assertEquals("Respondent Audit Org", entryGetFilterDto.getRespondentName());
        Assertions.assertEquals("ZZ1 1ZZ", entryGetFilterDto.getRespondentPostcode());
        Assertions.assertNull(entryGetFilterDto.getAccountReference());
        Assertions.assertEquals(
                "Read audit application title", entryGetFilterDto.getApplicationTitle());
        Assertions.assertEquals("RC1", entryGetFilterDto.getResulted());
        verify(applicationListEntryMapStructMapper)
                .toApplicationListEntry(payloadGetEntryInList, entryGetFilterDto);
    }

    @Test
    void testGetApplicationListEntries_emptyEntries_success() {
        ApplicationList applicationList = new AppListTestData().someComplete();

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        EntryApplicationListGetFilterDto entryGetFilterDto =
                Instancio.of(EntryApplicationListGetFilterDto.class)
                        .withSettings(settings)
                        .create();

        when(applicationListRepository.findByUuid(applicationList.getUuid()))
                .thenReturn(Optional.of(applicationList));

        Pageable mockPage = mock(Pageable.class);
        when(mockPage.getPageNumber()).thenReturn(1);
        PagingWrapper wrapper = PagingWrapper.of(List.of(), mockPage);

        Page<ApplicationListEntryGetSummaryProjection> dbPage =
                new PageImpl<>(List.of(), mockPage, 0);

        when(applicationListEntryRepository.searchForGetSummary(
                        applicationList.getUuid(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        entryGetFilterDto.getApplicantName(),
                        null,
                        null,
                        null,
                        null,
                        entryGetFilterDto.getRespondentName(),
                        entryGetFilterDto.getRespondentPostcode(),
                        entryGetFilterDto.getAccountReference(),
                        entryGetFilterDto.getApplicationTitle(),
                        entryGetFilterDto.getResulted(),
                        entryGetFilterDto.getFeeRequired(),
                        entryGetFilterDto.getSequenceNumber(),
                        mockPage))
                .thenReturn(dbPage);

        PayloadGetEntryInList payloadGetEntryInList =
                PayloadGetEntryInList.builder().listId(applicationList.getUuid()).build();

        // test
        EntryPage response =
                service.getApplicationListEntries(
                        payloadGetEntryInList, wrapper, entryGetFilterDto);

        // assert
        Assertions.assertNotNull(response);
        Assertions.assertEquals(0, response.getContent().size());
    }

    @Test
    void move_resequencesEntries_whenValidRequest() {
        val sourceListId = UUID.randomUUID();
        val sourceList = new ApplicationList();
        sourceList.setId(10L);
        sourceList.setUuid(sourceListId);

        val targetList = new ApplicationList();
        targetList.setId(20L);
        targetList.setUuid(UUID.randomUUID());

        val entryId1 = UUID.randomUUID();

        val entry1 = new ApplicationListEntry();
        entry1.setId(101L);
        entry1.setUuid(entryId1);
        entry1.setApplicationList(sourceList);
        entry1.setSequenceNumber((short) 2);
        entry1.setVersion(0L);

        val entryId2 = UUID.randomUUID();
        val entry2 = new ApplicationListEntry();
        entry2.setId(102L);
        entry2.setUuid(entryId2);
        entry2.setApplicationList(sourceList);
        entry2.setSequenceNumber((short) 1);
        entry2.setVersion(5L);

        val dto = new MoveEntriesDto();
        dto.setTargetListId(targetList.getUuid());
        dto.setEntryIds(Set.of(entryId1, entryId2));

        val validationSuccess = new MoveEntriesValidationSuccess();
        validationSuccess.setTargetList(targetList);
        moveEntriesValidator.setSuccess(validationSuccess);

        when(applicationListEntryRepository.findByUuidsInSourceList(eq(sourceListId), anySet()))
                .thenReturn(List.of(entry1, entry2));

        val mapping =
                AppListEntrySequenceMapping.builder()
                        .alId(targetList.getId())
                        .aleLastSequence(2)
                        .build();
        when(appListEntrySequenceMappingRepository.findByAlIdForUpdate(targetList.getId()))
                .thenReturn(Optional.of(mapping));

        when(applicationListEntryRepository.save(any(ApplicationListEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, ApplicationListEntry.class));

        ArgumentCaptor<ApplicationListEntry> savedEntryCaptor =
                ArgumentCaptor.forClass(ApplicationListEntry.class);

        service.move(sourceListId, dto);

        verify(applicationListEntryRepository).findByUuidsInSourceList(eq(sourceListId), anySet());
        verify(applicationListEntryRepository, times(2)).save(savedEntryCaptor.capture());

        List<ApplicationListEntry> savedEntries = savedEntryCaptor.getAllValues();
        Assertions.assertEquals(2, savedEntries.size());
        Assertions.assertSame(entry2, savedEntries.get(0));
        Assertions.assertSame(entry1, savedEntries.get(1));
        Assertions.assertSame(targetList, savedEntries.get(0).getApplicationList());
        Assertions.assertSame(targetList, savedEntries.get(1).getApplicationList());
        Assertions.assertEquals((short) 3, savedEntries.get(0).getSequenceNumber());
        Assertions.assertEquals((short) 4, savedEntries.get(1).getSequenceNumber());
        Assertions.assertEquals(4, mapping.getAleLastSequence());
    }

    @Test
    void move_throws_whenSomeRequestedEntriesAreMissingFromSourceList() {
        val sourceListId = UUID.randomUUID();
        val sourceList = new ApplicationList();
        sourceList.setId(10L);
        sourceList.setUuid(sourceListId);

        val targetList = new ApplicationList();
        targetList.setId(20L);
        targetList.setUuid(UUID.randomUUID());

        val entryId1 = UUID.randomUUID();

        val entry1 = new ApplicationListEntry();
        entry1.setId(101L);
        entry1.setUuid(entryId1);
        entry1.setApplicationList(sourceList);
        entry1.setVersion(0L);

        val entryId2 = UUID.randomUUID();
        val dto = new MoveEntriesDto();
        dto.setTargetListId(targetList.getUuid());
        dto.setEntryIds(Set.of(entryId1, entryId2));

        val validationSuccess = new MoveEntriesValidationSuccess();
        validationSuccess.setTargetList(targetList);
        moveEntriesValidator.setSuccess(validationSuccess);

        when(applicationListEntryRepository.findByUuidsInSourceList(eq(sourceListId), anySet()))
                .thenReturn(List.of(entry1));

        assertThatThrownBy(() -> service.move(sourceListId, dto))
                .isInstanceOf(AppRegistryException.class)
                .satisfies(
                        ex -> {
                            AppRegistryException appEx = (AppRegistryException) ex;
                            Assertions.assertEquals(
                                    ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST, appEx.getCode());
                        });

        verify(applicationListEntryRepository).findByUuidsInSourceList(eq(sourceListId), anySet());
        verify(applicationListEntryRepository, never()).save(any(ApplicationListEntry.class));
    }

    @Test
    void bulkUpdateFees_appendsStatusesForValidatedEntries() {
        val listId = UUID.randomUUID();
        val applicationList = openApplicationList(listId);

        val entryId1 = UUID.randomUUID();
        final var entry1 = applicationListEntry(applicationList, entryId1, 101L, (short) 2);
        val existingStatus1 = new AppListEntryFeeStatus();
        existingStatus1.setId(201L);

        val entryId2 = UUID.randomUUID();
        val entry2 = applicationListEntry(applicationList, entryId2, 102L, (short) 1);
        val existingStatus2 = new AppListEntryFeeStatus();
        existingStatus2.setId(202L);

        final var dto = bulkFeesUpdateDto(Set.of(entryId1, entryId2), PaymentStatus.PAID, false);

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuidsInSourceList(eq(listId), anySet()))
                .thenReturn(List.of(entry1, entry2));
        when(appListEntryFeeStatusRepository.getFeeStatusByEntryUuid(entryId1))
                .thenReturn(List.of(existingStatus1));
        when(appListEntryFeeStatusRepository.getFeeStatusByEntryUuid(entryId2))
                .thenReturn(List.of(existingStatus2));
        when(appListEntryFeeRepository.getOffsiteEntryFeesForEntry(entry1.getId()))
                .thenReturn(List.of());
        when(appListEntryFeeRepository.getOffsiteEntryFeesForEntry(entry2.getId()))
                .thenReturn(List.of());
        stubFeeStatusSave();

        final BulkUpdateResponseDto response = service.bulkUpdateFees(listId, dto);

        ArgumentCaptor<AppListEntryFeeStatus> statusCaptor =
                ArgumentCaptor.forClass(AppListEntryFeeStatus.class);
        verify(appListEntryFeeStatusRepository, never()).delete(existingStatus1);
        verify(appListEntryFeeStatusRepository, never()).delete(existingStatus2);
        verify(appListEntryFeeStatusRepository, times(2)).save(statusCaptor.capture());

        List<AppListEntryFeeStatus> savedStatuses = statusCaptor.getAllValues();
        Assertions.assertEquals(
                List.of(102L, 101L),
                savedStatuses.stream().map(status -> status.getAppListEntry().getId()).toList());
        Assertions.assertTrue(
                savedStatuses.stream()
                        .allMatch(status -> status.getAlefsFeeStatus() == FeeStatusType.PAID));
        Assertions.assertTrue(
                savedStatuses.stream()
                        .allMatch(
                                status ->
                                        LocalDate.of(2025, Month.OCTOBER, 7)
                                                .equals(status.getAlefsFeeStatusDate())));
        Assertions.assertTrue(
                savedStatuses.stream()
                        .allMatch(status -> "PAY-001".equals(status.getAlefsPaymentReference())));
        Assertions.assertTrue(
                savedStatuses.stream()
                        .allMatch(status -> status.getAlefsStatusCreationDate() != null));
        Assertions.assertEquals(2, response.getTotalCount());
        Assertions.assertEquals(2, response.getUpdatedCount());
        Assertions.assertEquals(BulkUpdateResponseDto.StatusEnum.SUCCEEDED, response.getStatus());
        Assertions.assertEquals(
                1.0,
                meterRegistry
                        .get(BULK_FEE_UPDATE_REQUESTS_METRIC)
                        .tag(METRIC_STATUS_TAG, "succeeded")
                        .counter()
                        .count());
        Assertions.assertEquals(
                1L,
                meterRegistry
                        .get(BULK_FEE_UPDATE_DURATION_METRIC)
                        .tag(METRIC_STATUS_TAG, "succeeded")
                        .timer()
                        .count());
        Assertions.assertEquals(
                1L,
                meterRegistry
                        .get(BULK_FEE_UPDATE_ENTRIES_METRIC)
                        .tag(METRIC_STATUS_TAG, "succeeded")
                        .summary()
                        .count());
        Assertions.assertEquals(
                2.0,
                meterRegistry
                        .get(BULK_FEE_UPDATE_ENTRIES_METRIC)
                        .tag(METRIC_STATUS_TAG, "succeeded")
                        .summary()
                        .totalAmount());
    }

    @Test
    void bulkUpdateFees_appendsAllProvidedFeeDetails() {
        val entryId = UUID.randomUUID();
        val existingStatus = new AppListEntryFeeStatus();
        existingStatus.setId(201L);
        val dto = new BulkFeesUpdateDto();
        dto.setEntryIds(Set.of(entryId));
        dto.setFeeDetails(
                List.of(
                        bulkFeeDetails(PaymentStatus.PAID, "PAY-001", false),
                        bulkFeeDetails(PaymentStatus.REMITTED, "PAY-002", false)));

        val listId = UUID.randomUUID();
        val applicationList = openApplicationList(listId);
        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));
        val entry = applicationListEntry(applicationList, entryId, 101L, (short) 1);
        when(applicationListEntryRepository.findByUuidsInSourceList(eq(listId), anySet()))
                .thenReturn(List.of(entry));
        when(appListEntryFeeStatusRepository.getFeeStatusByEntryUuid(entryId))
                .thenReturn(List.of(existingStatus));
        when(appListEntryFeeRepository.getOffsiteEntryFeesForEntry(entry.getId()))
                .thenReturn(List.of());
        stubFeeStatusSave();

        service.bulkUpdateFees(listId, dto);

        ArgumentCaptor<AppListEntryFeeStatus> statusCaptor =
                ArgumentCaptor.forClass(AppListEntryFeeStatus.class);
        verify(appListEntryFeeStatusRepository, never()).delete(existingStatus);
        verify(appListEntryFeeStatusRepository, times(2)).save(statusCaptor.capture());

        List<AppListEntryFeeStatus> savedStatuses = statusCaptor.getAllValues();
        Assertions.assertEquals(
                List.of(FeeStatusType.PAID, FeeStatusType.REMITTED),
                savedStatuses.stream().map(AppListEntryFeeStatus::getAlefsFeeStatus).toList());
        Assertions.assertEquals(
                List.of("PAY-001", "PAY-002"),
                savedStatuses.stream()
                        .map(AppListEntryFeeStatus::getAlefsPaymentReference)
                        .toList());
        Assertions.assertTrue(
                savedStatuses.stream().allMatch(status -> entry.equals(status.getAppListEntry())));
    }

    @Test
    void bulkUpdateFees_recordsFailedMetricWhenValidationFails() {
        val listId = UUID.randomUUID();
        val dto = bulkFeesUpdateDto(Set.of(UUID.randomUUID()), PaymentStatus.PAID, false);

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(
                AppRegistryException.class, () -> service.bulkUpdateFees(listId, dto));

        Assertions.assertEquals(
                1.0,
                meterRegistry
                        .get(BULK_FEE_UPDATE_REQUESTS_METRIC)
                        .tag(METRIC_STATUS_TAG, "failed")
                        .counter()
                        .count());
        Assertions.assertEquals(
                1L,
                meterRegistry
                        .get(BULK_FEE_UPDATE_DURATION_METRIC)
                        .tag(METRIC_STATUS_TAG, "failed")
                        .timer()
                        .count());
        Assertions.assertNull(
                meterRegistry
                        .find(BULK_FEE_UPDATE_ENTRIES_METRIC)
                        .tag(METRIC_STATUS_TAG, "failed")
                        .summary());
        verify(appListEntryFeeStatusRepository, never()).save(any(AppListEntryFeeStatus.class));
    }

    @Test
    void bulkUpdateFees_updatesEntriesAtOperationalLimit() {
        val listId = UUID.randomUUID();
        val applicationList = openApplicationList(listId);
        List<ApplicationListEntry> entries =
                IntStream.range(0, 500)
                        .mapToObj(
                                index ->
                                        applicationListEntry(
                                                applicationList,
                                                UUID.randomUUID(),
                                                1000L + index,
                                                (short) index))
                        .toList();
        Set<UUID> entryIds =
                entries.stream().map(ApplicationListEntry::getUuid).collect(Collectors.toSet());
        final var dto = bulkFeesUpdateDto(entryIds, PaymentStatus.PAID, false);

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuidsInSourceList(eq(listId), anySet()))
                .thenReturn(entries);
        when(appListEntryFeeStatusRepository.getFeeStatusByEntryUuid(any(UUID.class)))
                .thenReturn(List.of());
        when(appListEntryFeeRepository.getOffsiteEntryFeesForEntry(any(Long.class)))
                .thenReturn(List.of());
        stubFeeStatusSave();

        BulkUpdateResponseDto response = service.bulkUpdateFees(listId, dto);

        Assertions.assertEquals(500, response.getTotalCount());
        Assertions.assertEquals(500, response.getUpdatedCount());
        Assertions.assertEquals(BulkUpdateResponseDto.StatusEnum.SUCCEEDED, response.getStatus());
        verify(applicationListEntryRepository).findByUuidsInSourceList(eq(listId), anySet());
        verify(appListEntryFeeStatusRepository, times(500)).save(any(AppListEntryFeeStatus.class));
        verify(feeRepository, never()).findOffsite(any(LocalDate.class));
    }

    @Test
    void bulkUpdateFees_createsOffsiteFeeMappingWhenRequestedAndMissing() {
        val listId = UUID.randomUUID();
        val applicationList = openApplicationList(listId);
        val entryId = UUID.randomUUID();
        final var entry = applicationListEntry(applicationList, entryId, 101L, (short) 1);
        final var dto = bulkFeesUpdateDto(Set.of(entryId), PaymentStatus.PAID, true);
        val offsiteFee = new Fee();
        offsiteFee.setId(301L);
        offsiteFee.setOffsite(true);

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuidsInSourceList(eq(listId), anySet()))
                .thenReturn(List.of(entry));
        when(appListEntryFeeStatusRepository.getFeeStatusByEntryUuid(entryId))
                .thenReturn(List.of());
        when(appListEntryFeeRepository.getOffsiteEntryFeesForEntry(entry.getId()))
                .thenReturn(List.of());
        when(feeRepository.findOffsite(LocalDate.of(2025, Month.OCTOBER, 7)))
                .thenReturn(List.of(offsiteFee));
        when(appListEntryFeeRepository.save(any(AppListEntryFeeId.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        stubFeeStatusSave();

        service.bulkUpdateFees(listId, dto);

        ArgumentCaptor<AppListEntryFeeId> feeMappingCaptor =
                ArgumentCaptor.forClass(AppListEntryFeeId.class);
        verify(appListEntryFeeRepository).save(feeMappingCaptor.capture());
        Assertions.assertEquals(entry.getId(), feeMappingCaptor.getValue().getAppListEntryId());
        Assertions.assertEquals(offsiteFee.getId(), feeMappingCaptor.getValue().getFeeId());
    }

    @Test
    void bulkUpdateFees_reusesActiveOffsiteFeeWhenMultipleMappingsAreMissing() {
        val listId = UUID.randomUUID();
        val applicationList = openApplicationList(listId);
        val entryId1 = UUID.randomUUID();
        final var entry1 = applicationListEntry(applicationList, entryId1, 101L, (short) 2);
        val entryId2 = UUID.randomUUID();
        final var entry2 = applicationListEntry(applicationList, entryId2, 102L, (short) 1);
        final var dto = bulkFeesUpdateDto(Set.of(entryId1, entryId2), PaymentStatus.PAID, true);
        val offsiteFee = new Fee();
        offsiteFee.setId(301L);
        offsiteFee.setOffsite(true);

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuidsInSourceList(eq(listId), anySet()))
                .thenReturn(List.of(entry1, entry2));
        when(appListEntryFeeStatusRepository.getFeeStatusByEntryUuid(any(UUID.class)))
                .thenReturn(List.of());
        when(appListEntryFeeRepository.getOffsiteEntryFeesForEntry(any(Long.class)))
                .thenReturn(List.of());
        when(feeRepository.findOffsite(LocalDate.of(2025, Month.OCTOBER, 7)))
                .thenReturn(List.of(offsiteFee));
        when(appListEntryFeeRepository.save(any(AppListEntryFeeId.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        stubFeeStatusSave();

        service.bulkUpdateFees(listId, dto);

        ArgumentCaptor<AppListEntryFeeId> feeMappingCaptor =
                ArgumentCaptor.forClass(AppListEntryFeeId.class);
        verify(feeRepository).findOffsite(LocalDate.of(2025, Month.OCTOBER, 7));
        verify(appListEntryFeeRepository, times(2)).save(feeMappingCaptor.capture());
        Assertions.assertEquals(
                Set.of(entry1.getId(), entry2.getId()),
                feeMappingCaptor.getAllValues().stream()
                        .map(AppListEntryFeeId::getAppListEntryId)
                        .collect(Collectors.toSet()));
        Assertions.assertTrue(
                feeMappingCaptor.getAllValues().stream()
                        .allMatch(mapping -> offsiteFee.getId().equals(mapping.getFeeId())));
    }

    @Test
    void bulkUpdateFees_preservesExistingOffsiteFeeMappingWhenNotRequested() {
        val listId = UUID.randomUUID();
        val applicationList = openApplicationList(listId);
        val entryId = UUID.randomUUID();
        val entry = applicationListEntry(applicationList, entryId, 101L, (short) 1);
        final var dto = bulkFeesUpdateDto(Set.of(entryId), PaymentStatus.PAID, false);
        val existingOffsiteMapping = new AppListEntryFeeId();
        existingOffsiteMapping.setAppListEntryId(entry.getId());
        existingOffsiteMapping.setFeeId(301L);

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuidsInSourceList(eq(listId), anySet()))
                .thenReturn(List.of(entry));
        when(appListEntryFeeStatusRepository.getFeeStatusByEntryUuid(entryId))
                .thenReturn(List.of());
        when(appListEntryFeeRepository.getOffsiteEntryFeesForEntry(entry.getId()))
                .thenReturn(List.of(existingOffsiteMapping));
        stubFeeStatusSave();

        service.bulkUpdateFees(listId, dto);

        verify(appListEntryFeeRepository, never()).delete(existingOffsiteMapping);
        verify(appListEntryFeeRepository, never()).flush();
        verify(appListEntryFeeRepository, never()).save(any(AppListEntryFeeId.class));
    }

    private ApplicationList openApplicationList(UUID listId) {
        val applicationList = new ApplicationList();
        applicationList.setId(10L);
        applicationList.setUuid(listId);
        applicationList.setStatus(Status.OPEN);
        return applicationList;
    }

    private ApplicationListEntry applicationListEntry(
            ApplicationList applicationList, UUID entryId, Long id, short sequenceNumber) {
        val entry = new ApplicationListEntry();
        entry.setId(id);
        entry.setUuid(entryId);
        entry.setSequenceNumber(sequenceNumber);
        entry.setApplicationList(applicationList);
        return entry;
    }

    private BulkFeesUpdateDto bulkFeesUpdateDto(
            Set<UUID> entryIds, PaymentStatus paymentStatus, boolean hasOffsiteFee) {
        val dto = new BulkFeesUpdateDto();
        dto.setEntryIds(entryIds);
        dto.setFeeDetails(List.of(bulkFeeDetails(paymentStatus, "PAY-001", hasOffsiteFee)));
        return dto;
    }

    private BulkFeeDetailsDto bulkFeeDetails(
            PaymentStatus paymentStatus, String paymentReference, boolean hasOffsiteFee) {
        val feeDetails = new BulkFeeDetailsDto();
        feeDetails.setPaymentStatus(paymentStatus);
        feeDetails.setStatusDate(LocalDate.of(2025, Month.OCTOBER, 7));
        feeDetails.setPaymentReference(paymentReference);
        feeDetails.setHasOffsiteFee(hasOffsiteFee);
        return feeDetails;
    }

    private void stubFeeStatusSave() {
        when(appListEntryFeeStatusRepository.save(any(AppListEntryFeeStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void replaceOfficials_replacesOfficialsForAllEntries() {
        val listId = UUID.randomUUID();

        val applicationList = new ApplicationList();
        applicationList.setId(10L);
        applicationList.setUuid(listId);
        applicationList.setStatus(Status.OPEN);

        val entryId1 = UUID.randomUUID();
        val entry1 = new ApplicationListEntry();
        entry1.setId(101L);
        entry1.setUuid(entryId1);
        entry1.setSequenceNumber((short) 2);
        entry1.setApplicationList(applicationList);

        val entryId2 = UUID.randomUUID();
        val entry2 = new ApplicationListEntry();
        entry2.setId(102L);
        entry2.setUuid(entryId2);
        entry2.setSequenceNumber((short) 1);
        entry2.setApplicationList(applicationList);

        val existingOfficial1 = new AppListEntryOfficial();
        existingOfficial1.setId(201L);
        existingOfficial1.setAppListEntry(entry1);
        val existingOfficial2 = new AppListEntryOfficial();
        existingOfficial2.setId(202L);
        existingOfficial2.setAppListEntry(entry2);

        val official = new Official();
        official.setType(OfficialType.MAGISTRATE);
        official.setTitle("Mr");
        official.setForename("Ada");
        official.setSurname("Lovelace");

        val dto = new BulkOfficialsUpdateDto();
        dto.setEntryIds(List.of(entryId1, entryId2));
        dto.setOfficials(List.of(official));

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuidsInSourceList(eq(listId), anySet()))
                .thenReturn(List.of(entry1, entry2));
        when(appListEntryOfficialRepository.getOfficialByEntryUuid(entryId1))
                .thenReturn(List.of(existingOfficial1));
        when(appListEntryOfficialRepository.getOfficialByEntryUuid(entryId2))
                .thenReturn(List.of(existingOfficial2));
        when(applicationListEntryEntityMapper.toOfficial(any(Official.class), any()))
                .thenAnswer(
                        invocation -> {
                            val entity = new AppListEntryOfficial();
                            entity.setAppListEntry(invocation.getArgument(1));
                            return entity;
                        });

        service.replaceOfficials(listId, dto);

        verify(appListEntryOfficialRepository).delete(existingOfficial1);
        verify(appListEntryOfficialRepository).delete(existingOfficial2);
        verify(appListEntryOfficialRepository, times(2)).save(any(AppListEntryOfficial.class));
    }

    @Test
    void replaceOfficials_throwsBeforeWriting_whenSomeRequestedEntriesAreMissingFromSourceList() {
        val listId = UUID.randomUUID();

        val applicationList = new ApplicationList();
        applicationList.setId(10L);
        applicationList.setUuid(listId);
        applicationList.setStatus(Status.OPEN);

        val entryId1 = UUID.randomUUID();
        val entry1 = new ApplicationListEntry();
        entry1.setId(101L);
        entry1.setUuid(entryId1);
        entry1.setSequenceNumber((short) 1);
        entry1.setApplicationList(applicationList);

        final UUID entryId2 = UUID.randomUUID();
        val official = new Official();
        official.setType(OfficialType.MAGISTRATE);
        official.setTitle("Mr");
        official.setForename("Ada");
        official.setSurname("Lovelace");

        val dto = new BulkOfficialsUpdateDto();
        dto.setEntryIds(List.of(entryId1, entryId2));
        dto.setOfficials(List.of(official));

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuidsInSourceList(eq(listId), anySet()))
                .thenReturn(List.of(entry1));

        assertThatThrownBy(() -> service.replaceOfficials(listId, dto))
                .isInstanceOf(AppRegistryException.class)
                .satisfies(
                        ex -> {
                            AppRegistryException appEx = (AppRegistryException) ex;
                            Assertions.assertEquals(
                                    ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST, appEx.getCode());
                        });

        verify(appListEntryOfficialRepository, never()).delete(any(AppListEntryOfficial.class));
        verify(appListEntryOfficialRepository, never()).save(any(AppListEntryOfficial.class));
    }

    @Test
    void move_returns404_whenSourceListDoesNotExist() {
        doThrow(
                        new AppRegistryException(
                                ApplicationListError.SOURCE_LIST_NOT_FOUND,
                                "No source application list found for UUID"))
                .when(moveEntriesValidator)
                .validate(any(MoveEntriesPayload.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();
        UUID sourceListId = UUID.randomUUID();

        assertThatThrownBy(() -> service.move(sourceListId, dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
    }

    @Test
    void move_returns404_whenTargetListDoesNotExist() {
        doThrow(
                        new AppRegistryException(
                                ApplicationListError.TARGET_LIST_NOT_FOUND,
                                "No target application list found for UUID"))
                .when(moveEntriesValidator)
                .validate(any(MoveEntriesPayload.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(UUID.randomUUID());
        UUID sourceListId = UUID.randomUUID();

        assertThatThrownBy(() -> service.move(sourceListId, dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
    }

    @Test
    void move_returns400_whenSourceListNotOpen() {
        doThrow(
                        new AppRegistryException(
                                ApplicationListError.INVALID_LIST_STATUS, "Source list not open"))
                .when(moveEntriesValidator)
                .validate(any(MoveEntriesPayload.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();
        UUID sourceListId = UUID.randomUUID();

        assertThatThrownBy(() -> service.move(sourceListId, dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @Test
    void move_returns400_whenTargetListNotOpen() {
        doThrow(
                        new AppRegistryException(
                                ApplicationListError.INVALID_LIST_STATUS, "Target list not open"))
                .when(moveEntriesValidator)
                .validate(any(MoveEntriesPayload.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(UUID.randomUUID());
        dto.setEntryIds(Set.of(UUID.randomUUID()));
        UUID sourceListId = UUID.randomUUID();

        assertThatThrownBy(() -> service.move(sourceListId, dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @Test
    void move_returns400_whenEntryIdsNull() {
        doThrow(
                        new AppRegistryException(
                                ApplicationListError.ENTRY_NOT_PROVIDED, "No entry IDs provided"))
                .when(moveEntriesValidator)
                .validate(any(MoveEntriesPayload.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(UUID.randomUUID());
        dto.setEntryIds(null);
        UUID sourceListId = UUID.randomUUID();

        assertThatThrownBy(() -> service.move(sourceListId, dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @Test
    void move_returns400_whenEntryIdsEmpty() {
        doThrow(
                        new AppRegistryException(
                                ApplicationListError.ENTRY_NOT_PROVIDED, "No entry IDs provided"))
                .when(moveEntriesValidator)
                .validate(any(MoveEntriesPayload.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setEntryIds(Set.of());
        UUID sourceListId = UUID.randomUUID();

        assertThatThrownBy(() -> service.move(sourceListId, dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @Test
    void move_returns400_whenEntryDoesNotExist() {
        doThrow(
                        new AppRegistryException(
                                ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST,
                                "No application list entry found"))
                .when(moveEntriesValidator)
                .validate(any(MoveEntriesPayload.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setEntryIds(Set.of(UUID.randomUUID()));
        UUID sourceListId = UUID.randomUUID();

        assertThatThrownBy(() -> service.move(sourceListId, dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @Test
    void move_returns400_whenEntryNotInSourceList() {
        doThrow(
                        new AppRegistryException(
                                ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST,
                                "Application list entry does not belong to source list"))
                .when(moveEntriesValidator)
                .validate(any(MoveEntriesPayload.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setEntryIds(Set.of(UUID.randomUUID()));
        UUID sourceListId = UUID.randomUUID();

        assertThatThrownBy(() -> service.move(sourceListId, dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @Test
    void testUpdateClosedListWithAppend() {
        // setup payload with a note to be applied
        EntryUpdateClosedDto entryUpdateClosedDto = new EntryUpdateClosedDto();
        entryUpdateClosedDto.setAdditionalNotes("additional notes");

        ApplicationListEntry applicationListEntry = new ApplicationListEntry();

        // set the initial note that already exists
        String note = "note";
        applicationListEntry.setNotes(note);
        applicationListEntry.setId(1000L);
        applicationListEntry.setVersion(232L);

        // dummy the success of the validator
        updateClosedEntriesValidator.setSuccess(
                new UpdateApplicationEntryClosedValidationSuccess(
                        new ApplicationList(), applicationListEntry));

        ArgumentCaptor<ApplicationListEntry> captorEntry =
                ArgumentCaptor.forClass(ApplicationListEntry.class);

        PayloadForUpdateClosedEntry payload =
                new PayloadForUpdateClosedEntry(
                        entryUpdateClosedDto, UUID.randomUUID(), UUID.randomUUID());

        // run test
        service.updateClosedEntry(payload);

        // now verify what has happened
        verify(applicationListEntryRepository).save(captorEntry.capture());
        Assertions.assertEquals(
                note + " " + entryUpdateClosedDto.getAdditionalNotes(),
                captorEntry.getValue().getNotes());
    }

    @Test
    void testUpdateClosedListWithNullExistingNote() {
        EntryUpdateClosedDto entryUpdateClosedDto = new EntryUpdateClosedDto();
        entryUpdateClosedDto.setAdditionalNotes("additional notes");

        ApplicationListEntry applicationListEntry = new ApplicationListEntry();
        applicationListEntry.setId(1000L);
        applicationListEntry.setVersion(232L);

        updateClosedEntriesValidator.setSuccess(
                new UpdateApplicationEntryClosedValidationSuccess(
                        new ApplicationList(), applicationListEntry));

        ArgumentCaptor<ApplicationListEntry> captorEntry =
                ArgumentCaptor.forClass(ApplicationListEntry.class);

        PayloadForUpdateClosedEntry payload =
                new PayloadForUpdateClosedEntry(
                        entryUpdateClosedDto, UUID.randomUUID(), UUID.randomUUID());

        service.updateClosedEntry(payload);

        verify(applicationListEntryRepository).save(captorEntry.capture());
        Assertions.assertEquals(
                entryUpdateClosedDto.getAdditionalNotes(), captorEntry.getValue().getNotes());
    }

    @Test
    void givenUpdateClosedListWithEmptyAdditionalNotesWhenExistingNoteThenLeavesNotesUnchanged() {
        EntryUpdateClosedDto entryUpdateClosedDto = new EntryUpdateClosedDto();
        entryUpdateClosedDto.setAdditionalNotes("");

        ApplicationListEntry applicationListEntry = new ApplicationListEntry();
        String existingNotes = "note";
        applicationListEntry.setNotes(existingNotes);
        applicationListEntry.setId(1000L);
        applicationListEntry.setVersion(232L);

        updateClosedEntriesValidator.setSuccess(
                new UpdateApplicationEntryClosedValidationSuccess(
                        new ApplicationList(), applicationListEntry));

        ArgumentCaptor<ApplicationListEntry> captorEntry =
                ArgumentCaptor.forClass(ApplicationListEntry.class);

        PayloadForUpdateClosedEntry payload =
                new PayloadForUpdateClosedEntry(
                        entryUpdateClosedDto, UUID.randomUUID(), UUID.randomUUID());

        service.updateClosedEntry(payload);

        verify(applicationListEntryRepository).save(captorEntry.capture());
        Assertions.assertEquals(existingNotes, captorEntry.getValue().getNotes());
    }

    @Test
    void givenUpdateClosedListWithEmptyAdditionalNotesWhenNoExistingNoteThenLeavesNotesNull() {
        EntryUpdateClosedDto entryUpdateClosedDto = new EntryUpdateClosedDto();
        entryUpdateClosedDto.setAdditionalNotes("");

        ApplicationListEntry applicationListEntry = new ApplicationListEntry();
        applicationListEntry.setId(1000L);
        applicationListEntry.setVersion(232L);

        updateClosedEntriesValidator.setSuccess(
                new UpdateApplicationEntryClosedValidationSuccess(
                        new ApplicationList(), applicationListEntry));

        ArgumentCaptor<ApplicationListEntry> captorEntry =
                ArgumentCaptor.forClass(ApplicationListEntry.class);

        PayloadForUpdateClosedEntry payload =
                new PayloadForUpdateClosedEntry(
                        entryUpdateClosedDto, UUID.randomUUID(), UUID.randomUUID());

        service.updateClosedEntry(payload);

        verify(applicationListEntryRepository).save(captorEntry.capture());
        Assertions.assertNull(captorEntry.getValue().getNotes());
    }

    @Test
    void givenUpdateClosedListWhenCombinedNotesExceedLimitThenThrowsBadRequestAndDoesNotSave() {
        EntryUpdateClosedDto entryUpdateClosedDto = new EntryUpdateClosedDto();
        entryUpdateClosedDto.setAdditionalNotes("additional notes");

        ApplicationListEntry applicationListEntry = new ApplicationListEntry();
        String existingNotes = "a".repeat(3990);
        applicationListEntry.setNotes(existingNotes);
        applicationListEntry.setId(1000L);
        applicationListEntry.setVersion(232L);

        updateClosedEntriesValidator.setSuccess(
                new UpdateApplicationEntryClosedValidationSuccess(
                        new ApplicationList(), applicationListEntry));

        PayloadForUpdateClosedEntry payload =
                new PayloadForUpdateClosedEntry(
                        entryUpdateClosedDto, UUID.randomUUID(), UUID.randomUUID());

        assertThatThrownBy(() -> service.updateClosedEntry(payload))
                .isInstanceOf(AppRegistryException.class)
                .satisfies(
                        ex -> {
                            AppRegistryException appEx = (AppRegistryException) ex;
                            Assertions.assertEquals(
                                    AppListEntryError.NOTES_TOO_LONG, appEx.getCode());
                            Assertions.assertEquals(
                                    org.springframework.http.HttpStatus.BAD_REQUEST,
                                    appEx.getCode().getCode().getHttpCode());
                        });

        verify(applicationListEntryRepository, never()).save(any(ApplicationListEntry.class));
        Assertions.assertEquals(existingNotes, applicationListEntry.getNotes());
    }

    @Test
    void deleteEntrySuccess() {
        ApplicationListEntry applicationListEntry = new ApplicationListEntry();

        // set the success payload that the validator has validated.
        deleteEntryValidator.success = new DeleteEntryValidationSuccess(applicationListEntry);

        // now make the call to delete
        PayloadForDeleteEntry payloadForDeleteEntry =
                new PayloadForDeleteEntry(UUID.randomUUID(), UUID.randomUUID());
        service.deleteEntry(payloadForDeleteEntry);

        // ensure that we called save and that we set the soft deleted state to true
        Assertions.assertTrue(applicationListEntry.isDeleted());
        verify(applicationListEntryRepository).save(applicationListEntry);
    }

    @Test
    void givenBulkImportHasSucceeded_whenGetApplicationListEntriesByJobId_returnsEntries() {
        UUID jobId = UUID.randomUUID();

        when(jobExistanceValidator.validate(eq(jobId), any()))
                .thenAnswer(
                        invocation -> {
                            @SuppressWarnings("unchecked")
                            val validateFunction =
                                    (BiFunction<UUID, JobSuccess, JobStatusResponse>)
                                            invocation.getArgument(1);

                            return validateFunction.apply(jobId, new JobSuccess());
                        });

        val asyncJobAppListEntry = new AsyncJobsAppListEntry();
        asyncJobAppListEntry.setAppListEntryId(UUID.randomUUID());
        asyncJobAppListEntry.setAsyncJobId(jobId);

        List<AsyncJobsAppListEntry> entries = createAsyncJobAppListEntries(jobId, 3);

        when(asyncJobAppListEntryRepository.findByAsyncJobId(jobId)).thenReturn(entries);

        List<UUID> actualEntries = service.getApplicationListEntriesByJobId(jobId);

        Assertions.assertEquals(3, actualEntries.size());
        verify(asyncJobAppListEntryRepository, times(1)).findByAsyncJobId(jobId);

        List<UUID> expectedEntries =
                entries.stream().map(AsyncJobsAppListEntry::getAppListEntryId).toList();

        for (UUID entryId : actualEntries) {
            Assertions.assertTrue(
                    expectedEntries.contains(entryId),
                    "Entry ID " + entryId + " should be in the expected entries list");
        }
    }

    @Test
    void givenBulkImportHasNotSucceeded_whenGetApplicationListEntriesByJobId_returnsEmptyList() {
        UUID jobId = UUID.randomUUID();

        when(jobExistanceValidator.validate(eq(jobId), any()))
                .thenAnswer(
                        invocation -> {
                            @SuppressWarnings("unchecked")
                            val validateFunction =
                                    (BiFunction<UUID, JobSuccess, JobStatusResponse>)
                                            invocation.getArgument(1);

                            val success = new JobSuccess();
                            return validateFunction.apply(jobId, success);
                        });

        when(asyncJobAppListEntryRepository.findByAsyncJobId(jobId)).thenReturn(List.of());

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> service.getApplicationListEntriesByJobId(jobId));

        Assertions.assertEquals(JobError.JOB_DOES_NOT_EXIST_OR_NOT_FOR_USER, exception.getCode());
        Assertions.assertEquals(
                "No entries found for jobId: %s".formatted(jobId), exception.getMessage());
    }

    @Test
    void bulkImport_withJobId_shouldSaveAsyncJobAppListEntry() {
        UUID listId = UUID.randomUUID();
        val applicationList = openApplicationList(listId);

        when(applicationListRepository.findByUuid(applicationList.getUuid()))
                .thenReturn(Optional.of(applicationList));

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();
        entryCreateDto.setApplicant(Instancio.of(Applicant.class).withSettings(settings).create());
        entryCreateDto.setWordingFields(null);

        ApplicationCode code = new ApplicationCode();
        code.setWording("Test Wording");

        // Now make the call to createBulkEntry
        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(applicationList.getUuid())
                        .data(entryCreateDto)
                        .build();

        when(applicantMapper.toApplicant(payload.getData().getApplicant()))
                .thenReturn(Instancio.of(NameAddress.class).withSettings(settings).create());
        when(applicantMapper.toRespondent(payload.getData().getRespondent()))
                .thenReturn(Instancio.of(NameAddress.class).withSettings(settings).create());
        when(nameAddressRepository.save(any()))
                .thenReturn(Instancio.of(NameAddress.class).withSettings(settings).create());
        when(appListEntryFeeStatusRepository.save(any()))
                .thenReturn(
                        Instancio.of(AppListEntryFeeStatus.class).withSettings(settings).create());
        when(appListEntryOfficialRepository.save(any()))
                .thenReturn(
                        Instancio.of(AppListEntryOfficial.class).withSettings(settings).create());

        val entryId = UUID.randomUUID();
        val entry = applicationListEntry(applicationList, entryId, 101L, (short) 2);
        success =
                CreateApplicationEntryValidationSuccess.builder()
                        .wordingSentence(WordingTemplateSentence.with(code.getWording()))
                        .fee(null)
                        .applicationCode(code)
                        .sa(new StandardApplicant())
                        .applicationList(applicationList)
                        .build();

        entry.setVersion(1L);
        when(applicationListEntryRepository.save(any())).thenReturn(entry);

        when(applicationListEntryEntityMapper.toApplicationListEntry(
                        eq(entryCreateDto),
                        notNull(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        eq(YesOrNo.YES)))
                .thenReturn(entry);

        EntryGetDetailDto entryGetDetailDto = new EntryGetDetailDto();
        entryGetDetailDto.setHasOffsiteFee(false);

        when(applicationListEntryMapStructMapper.toEntryGetDetailDto(
                        any(), anyList(), any(), any(), any()))
                .thenReturn(entryGetDetailDto);

        UUID jobId = UUID.randomUUID();
        service.bulkImport(payload, jobId, success);

        // Ensure that we called save on asyncJobAppListEntryRepository with the correct values
        ArgumentCaptor<AsyncJobsAppListEntry> captor =
                ArgumentCaptor.forClass(AsyncJobsAppListEntry.class);
        verify(asyncJobAppListEntryRepository).save(captor.capture());

        AsyncJobsAppListEntry savedEntity = captor.getValue();
        Assertions.assertEquals(jobId, savedEntity.getAsyncJobId());
        Assertions.assertEquals(entryId, savedEntity.getAppListEntryId());
    }

    @Test
    void createBulkEntry_withoutJobId_shouldNotSaveAsyncJobAppListEntry() {
        UUID listId = UUID.randomUUID();
        val applicationList = openApplicationList(listId);

        when(applicationListRepository.findByUuid(applicationList.getUuid()))
                .thenReturn(Optional.of(applicationList));

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();
        entryCreateDto.setApplicant(Instancio.of(Applicant.class).withSettings(settings).create());
        entryCreateDto.setWordingFields(null);

        ApplicationCode code = new ApplicationCode();
        code.setWording("Test Wording");

        // Now make the call to createBulkEntry
        PayloadForCreate<EntryCreateDto> payload =
                PayloadForCreate.<EntryCreateDto>builder()
                        .id(applicationList.getUuid())
                        .data(entryCreateDto)
                        .build();

        when(applicantMapper.toApplicant(payload.getData().getApplicant()))
                .thenReturn(Instancio.of(NameAddress.class).withSettings(settings).create());
        when(applicantMapper.toRespondent(payload.getData().getRespondent()))
                .thenReturn(Instancio.of(NameAddress.class).withSettings(settings).create());
        when(nameAddressRepository.save(any()))
                .thenReturn(Instancio.of(NameAddress.class).withSettings(settings).create());
        when(appListEntryFeeStatusRepository.save(any()))
                .thenReturn(
                        Instancio.of(AppListEntryFeeStatus.class).withSettings(settings).create());
        when(appListEntryOfficialRepository.save(any()))
                .thenReturn(
                        Instancio.of(AppListEntryOfficial.class).withSettings(settings).create());

        val entryId = UUID.randomUUID();
        val entry = applicationListEntry(applicationList, entryId, 101L, (short) 2);
        success =
                CreateApplicationEntryValidationSuccess.builder()
                        .wordingSentence(WordingTemplateSentence.with(code.getWording()))
                        .fee(null)
                        .applicationCode(code)
                        .sa(new StandardApplicant())
                        .applicationList(applicationList)
                        .build();

        entry.setVersion(1L);
        when(applicationListEntryRepository.save(any())).thenReturn(entry);

        when(applicationListEntryEntityMapper.toApplicationListEntry(
                        eq(entryCreateDto),
                        notNull(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        eq(YesOrNo.YES)))
                .thenReturn(entry);

        EntryGetDetailDto entryGetDetailDto = new EntryGetDetailDto();
        entryGetDetailDto.setHasOffsiteFee(false);

        when(applicationListEntryMapStructMapper.toEntryGetDetailDto(
                        any(), anyList(), any(), any(), any()))
                .thenReturn(entryGetDetailDto);

        service.createBulkEntry(payload);
        verify(asyncJobAppListEntryRepository, times(0)).save(any());
    }

    class DummyCreateApplicationEntryValidator extends CreateApplicationEntryValidator {

        public DummyCreateApplicationEntryValidator(
                ApplicationListRepository applicationListRepository,
                ApplicationCodeRepository applicationCodeRepository,
                ApplicationFeeService feeService,
                BusinessDateProvider businessDateProvider,
                StandardApplicantRepository standardApplicantRepository) {
            super(
                    applicationListRepository,
                    applicationCodeRepository,
                    feeService,
                    businessDateProvider,
                    standardApplicantRepository);
        }

        @Override
        public <R> R validate(
                PayloadForCreate<EntryCreateDto> validatable,
                BiFunction<
                                PayloadForCreate<EntryCreateDto>,
                                CreateApplicationEntryValidationSuccess,
                                R>
                        validateSuccess) {
            return validateSuccess.apply(validatable, success);
        }
    }

    class DummyBulkCreateApplicationEntryValidator extends BulkCreateApplicationEntryValidator {

        public DummyBulkCreateApplicationEntryValidator(
                ApplicationListRepository applicationListRepository,
                ApplicationCodeRepository applicationCodeRepository,
                ApplicationFeeService feeService,
                BusinessDateProvider businessDateProvider,
                StandardApplicantRepository standardApplicantRepository) {
            super(
                    applicationListRepository,
                    applicationCodeRepository,
                    feeService,
                    businessDateProvider,
                    standardApplicantRepository);
        }

        @Override
        public <R> R validate(
                PayloadForCreate<EntryCreateDto> validatable,
                BiFunction<
                                PayloadForCreate<EntryCreateDto>,
                                CreateApplicationEntryValidationSuccess,
                                R>
                        validateSuccess) {
            return validateSuccess.apply(validatable, success);
        }
    }

    class DummyAuditOperationService implements AuditOperationService {

        @Override
        public <T, E extends Keyable> T processAudit(
                E oldValue,
                AuditOperation auditType,
                Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution) {
            Optional<AuditableResult<T, E>> optional =
                    execution.apply(
                            new CompleteEvent(
                                    new StartEvent(
                                            AppListAuditOperation.CREATE_APP_LIST,
                                            UUID.randomUUID().toString(),
                                            null),
                                    "result",
                                    null));
            return optional.map(AuditableResult::getResultingValue).orElse(null);
        }

        @Override
        public <T, E extends Keyable> T processAudit(
                AuditOperation auditType,
                Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution) {
            return processAudit(null, auditType, execution);
        }
    }

    class DummyUpdateApplicationEntryValidator extends UpdateApplicationEntryValidator {
        public DummyUpdateApplicationEntryValidator(
                ApplicationListRepository applicationListRepository,
                ApplicationCodeRepository applicationCodeRepository,
                ApplicationFeeService feeService,
                BusinessDateProvider businessDateProvider,
                StandardApplicantRepository standardApplicantRepository,
                ApplicationListEntryRepository applicationListEntryRepository,
                AppListEntryFeeStatusRepository appListEntryFeeStatusRepository) {
            super(
                    applicationListRepository,
                    applicationCodeRepository,
                    feeService,
                    businessDateProvider,
                    standardApplicantRepository,
                    applicationListEntryRepository,
                    appListEntryFeeStatusRepository);
        }

        @Override
        public <R> R validate(
                PayloadForUpdateEntry validatable,
                BiFunction<PayloadForUpdateEntry, UpdateApplicationEntryValidationSuccess, R>
                        validateSuccess) {
            return validateSuccess.apply(validatable, updateSuccess);
        }
    }

    class DummyGetApplicationEntryValidator extends GetApplicationEntryValidator {
        public DummyGetApplicationEntryValidator(
                ApplicationListRepository applicationListRepository,
                ApplicationListEntryRepository applicationListEntryRepository) {
            super(applicationListEntryRepository, applicationListRepository);
        }

        @Override
        public <R> R validate(
                PayloadGetEntryInList validatable,
                BiFunction<PayloadGetEntryInList, GetEntryValidationSuccess, R> validateSuccess) {
            return validateSuccess.apply(validatable, getEntryValidationSuccess);
        }
    }

    class DummyGetApplicationEntryFromClosedListValidator
            extends GetApplicationEntryFromClosedListValidator {
        public DummyGetApplicationEntryFromClosedListValidator(
                ApplicationListRepository applicationListRepository,
                ApplicationListEntryRepository applicationListEntryRepository) {
            super(applicationListEntryRepository, applicationListRepository);
        }

        @Override
        public <R> R validate(
                PayloadGetEntryInList validatable,
                BiFunction<PayloadGetEntryInList, GetEntryValidationSuccess, R> validateSuccess) {
            return validateSuccess.apply(validatable, getEntryValidationSuccess);
        }
    }

    static class DummyGetApplicationListEntriesValidator
            extends GetApplicationListEntriesValidator {
        public DummyGetApplicationListEntriesValidator(
                ApplicationListRepository applicationListRepository) {
            super(applicationListRepository);
        }

        @Override
        public <R> R validate(
                PayloadGetEntryInList validatable,
                BiFunction<PayloadGetEntryInList, ApplicationList, R> validateSuccess) {
            return validateSuccess.apply(validatable, new ApplicationList());
        }
    }

    static class DummyMoveEntriesValidator extends MoveEntriesValidator {

        private MoveEntriesValidationSuccess success;

        public DummyMoveEntriesValidator(ApplicationListRepository applicationListRepository) {
            super(applicationListRepository);
        }

        void setSuccess(MoveEntriesValidationSuccess success) {
            this.success = success;
        }

        @Override
        public <R> R validate(
                MoveEntriesPayload payload,
                java.util.function.BiFunction<MoveEntriesPayload, MoveEntriesValidationSuccess, R>
                        createSupplier) {

            return createSupplier.apply(payload, success);
        }
    }

    static class DummyUpdateClosedEntriesValidator extends UpdateClosedApplicationEntryValidator {

        private UpdateApplicationEntryClosedValidationSuccess success;

        public DummyUpdateClosedEntriesValidator(
                ApplicationListRepository applicationListRepository,
                ApplicationListEntryRepository applicationListEntryRepository) {
            super(applicationListEntryRepository, applicationListRepository);
        }

        public void setSuccess(UpdateApplicationEntryClosedValidationSuccess success) {
            this.success = success;
        }

        @Override
        public <R> R validate(
                PayloadForUpdateClosedEntry payload,
                java.util.function.BiFunction<
                                PayloadForUpdateClosedEntry,
                                UpdateApplicationEntryClosedValidationSuccess,
                                R>
                        createSupplier) {

            return createSupplier.apply(payload, success);
        }
    }

    class DummyDeleteEntryValidator extends DeleteApplicationListEntryValidator {
        private DeleteEntryValidationSuccess success;

        public DummyDeleteEntryValidator(
                ApplicationListRepository applicationListRepository,
                ApplicationListEntryRepository applicationListEntryRepository) {
            super(applicationListRepository, applicationListEntryRepository);
        }

        @Override
        public <R> R validate(
                PayloadForDeleteEntry validatable,
                BiFunction<PayloadForDeleteEntry, DeleteEntryValidationSuccess, R>
                        validateSuccess) {
            return validateSuccess.apply(validatable, success);
        }
    }

    @Test
    void given_filter_when_getEntryIds_then_return_ids() {
        EntryGetFilterDto filterDto = new EntryGetFilterDto();
        filterDto.setStatus(ApplicationListStatus.OPEN);
        filterDto.setCourtCode("COURT1");
        filterDto.setCjaCode("CJA1");
        filterDto.setApplicantOrganisation("Applicant Org");
        filterDto.setApplicantSurname("ApplicantSurname");
        filterDto.setStandardApplicantCode("STD1");
        filterDto.setRespondentOrganisation("Respondent Org");
        filterDto.setRespondentSurname("RespondentSurname");
        filterDto.setRespondentPostcode("AB1 2CD");
        filterDto.setAccountReference("ACC123");
        filterDto.setApplicationTitle("Title");

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        when(applicationListEntryMapStructMapper.toStatus(ApplicationListStatus.OPEN))
                .thenReturn(Status.OPEN);
        when(applicationListEntryRepository.searchForGetSummaryIds(
                        null,
                        false,
                        null,
                        "COURT1",
                        null,
                        "CJA1",
                        "Applicant Org",
                        "ApplicantSurname",
                        null,
                        "STD1",
                        Status.OPEN,
                        "Respondent Org",
                        "RespondentSurname",
                        null,
                        "AB1 2CD",
                        "ACC123",
                        "Title",
                        null,
                        null,
                        null))
                .thenReturn(List.of(id1, id2));

        EntryIdsDto response = service.getEntryIds(filterDto);

        Assertions.assertEquals(List.of(id1, id2), response.getIds());
        verify(applicationListEntryRepository)
                .searchForGetSummaryIds(
                        null,
                        false,
                        null,
                        "COURT1",
                        null,
                        "CJA1",
                        "Applicant Org",
                        "ApplicantSurname",
                        null,
                        "STD1",
                        Status.OPEN,
                        "Respondent Org",
                        "RespondentSurname",
                        null,
                        "AB1 2CD",
                        "ACC123",
                        "Title",
                        null,
                        null,
                        null);
    }

    @Test
    void given_nullFilter_when_getEntryIds_then_use_empty_filter() {
        UUID id = UUID.randomUUID();

        when(applicationListEntryMapStructMapper.toStatus((ApplicationListStatus) null))
                .thenReturn(null);
        when(applicationListEntryRepository.searchForGetSummaryIds(
                        null, false, null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null))
                .thenReturn(List.of(id));

        EntryIdsDto response = service.getEntryIds(null);

        Assertions.assertEquals(List.of(id), response.getIds());
    }

    @Test
    void given_idsSelection_when_bulkActionPreview_then_return_submitted_ids_and_counts() {
        ReflectionTestUtils.setField(service, "bulkActionPreviewGlobalLimit", 2);
        UUID firstEntryId = UUID.randomUUID();
        UUID secondEntryId = UUID.randomUUID();
        ApplicationListEntryGetSummaryProjection firstProjection =
                bulkActionPreviewProjection(firstEntryId, 1L);
        ApplicationListEntryGetSummaryProjection secondProjection =
                bulkActionPreviewProjection(secondEntryId, 2L);
        final EntryGetSummaryDto firstSummary = stubEntrySummary(firstProjection, firstEntryId);
        final EntryGetSummaryDto secondSummary = stubEntrySummary(secondProjection, secondEntryId);

        stubBulkActionPreviewSummaryPage(2, firstProjection, secondProjection);
        when(applicationListEntryRepository.findResolutionCodesByEntryIds(anyList()))
                .thenReturn(List.of());

        BulkActionPreviewResponseDto response =
                service.bulkActionPreview(bulkActionPreviewRequest(firstEntryId, secondEntryId));

        Assertions.assertEquals(BulkActionType.UPDATE_NOTES, response.getAction());
        Assertions.assertEquals(2, response.getLimit());
        Assertions.assertEquals(2, response.getSelectedCount());
        Assertions.assertEquals(2, response.getEligibleCount());
        Assertions.assertEquals(0, response.getIneligibleCount());
        Assertions.assertEquals(List.of(firstEntryId, secondEntryId), response.getEntryIds());
        Assertions.assertEquals(List.of(firstSummary, secondSummary), response.getEntries());
    }

    @Test
    void given_idsSelectionAboveLimit_when_bulkActionPreview_then_throw_exceeds_limit() {
        ReflectionTestUtils.setField(service, "bulkActionPreviewGlobalLimit", 1);

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                service.bulkActionPreview(
                                        bulkActionPreviewRequest(
                                                UUID.randomUUID(), UUID.randomUUID())));

        Assertions.assertEquals(
                AppListEntryError.BULK_ACTION_SELECTION_EXCEEDS_LIMIT, exception.getCode());
    }

    @Test
    void given_filterSelection_when_bulkActionPreview_then_return_matching_ids_and_entry_context() {
        ReflectionTestUtils.setField(service, "bulkActionPreviewGlobalLimit", 2);
        UUID entryId = UUID.randomUUID();
        ApplicationListEntryGetSummaryProjection projection =
                bulkActionPreviewProjection(entryId, 1L);
        final EntryGetSummaryDto summary = stubEntrySummary(projection, entryId);
        EntryGetFilterDto filter = new EntryGetFilterDto();
        filter.setApplicantSurname("Smith");
        filter.setStatus(ApplicationListStatus.OPEN);

        when(applicationListEntryMapStructMapper.toStatus(ApplicationListStatus.OPEN))
                .thenReturn(Status.OPEN);
        stubBulkActionPreviewSummaryPage(1, projection);
        when(applicationListEntryRepository.findResolutionCodesByEntryIds(anyList()))
                .thenReturn(List.of());

        BulkActionPreviewResponseDto response =
                service.bulkActionPreview(
                        bulkActionPreviewFilterRequest(filter, List.of("date,desc"), List.of()));

        Assertions.assertEquals(BulkActionType.UPDATE_NOTES, response.getAction());
        Assertions.assertEquals(2, response.getLimit());
        Assertions.assertEquals(1, response.getSelectedCount());
        Assertions.assertEquals(1, response.getEligibleCount());
        Assertions.assertEquals(0, response.getIneligibleCount());
        Assertions.assertEquals(List.of(entryId), response.getEntryIds());
        Assertions.assertEquals(List.of(summary), response.getEntries());
    }

    @Test
    void
            given_filterSelectionWithExclusions_when_bulkActionPreview_then_pass_exclusions_to_search() {
        ReflectionTestUtils.setField(service, "bulkActionPreviewGlobalLimit", 2);
        UUID excludedEntryId = UUID.randomUUID();
        List<UUID> excludedEntryIds = List.of(excludedEntryId);

        stubBulkActionPreviewSummaryPage(0);
        when(applicationListEntryRepository.findResolutionCodesByEntryIds(anyList()))
                .thenReturn(List.of());

        BulkActionPreviewResponseDto response =
                service.bulkActionPreview(
                        bulkActionPreviewFilterRequest(
                                new EntryGetFilterDto(), List.of(), excludedEntryIds));

        Assertions.assertEquals(0, response.getSelectedCount());
        Assertions.assertEquals(List.of(), response.getEntryIds());
        Assertions.assertEquals(List.of(), response.getEntries());
        verify(applicationListEntryRepository)
                .searchForBulkActionPreviewSummary(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        eq(false),
                        anyList(),
                        eq(true),
                        eq(excludedEntryIds),
                        any(Pageable.class));
    }

    @Test
    void given_filterSelectionAboveLimit_when_bulkActionPreview_then_throw_exceeds_limit() {
        ReflectionTestUtils.setField(service, "bulkActionPreviewGlobalLimit", 1);
        ApplicationListEntryGetSummaryProjection projection =
                bulkActionPreviewProjection(UUID.randomUUID(), 1L);

        stubBulkActionPreviewSummaryPage(2, projection);

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                service.bulkActionPreview(
                                        bulkActionPreviewFilterRequest(
                                                new EntryGetFilterDto(), List.of(), List.of())));

        Assertions.assertEquals(
                AppListEntryError.BULK_ACTION_SELECTION_EXCEEDS_LIMIT, exception.getCode());
        verify(applicationListEntryMapStructMapper, never()).toEntrySummary(any());
    }

    @Test
    void given_idsSelection_when_repositoryReturnsDifferentOrder_then_preserve_selected_order() {
        ReflectionTestUtils.setField(service, "bulkActionPreviewGlobalLimit", 2);
        UUID firstEntryId = UUID.randomUUID();
        UUID secondEntryId = UUID.randomUUID();
        ApplicationListEntryGetSummaryProjection firstProjection =
                bulkActionPreviewProjection(firstEntryId, 1L);
        ApplicationListEntryGetSummaryProjection secondProjection =
                bulkActionPreviewProjection(secondEntryId, 2L);
        final EntryGetSummaryDto firstSummary = stubEntrySummary(firstProjection, firstEntryId);
        final EntryGetSummaryDto secondSummary = stubEntrySummary(secondProjection, secondEntryId);

        stubBulkActionPreviewSummaryPage(2, secondProjection, firstProjection);
        when(applicationListEntryRepository.findResolutionCodesByEntryIds(anyList()))
                .thenReturn(List.of());

        BulkActionPreviewResponseDto response =
                service.bulkActionPreview(bulkActionPreviewRequest(firstEntryId, secondEntryId));

        Assertions.assertEquals(List.of(firstEntryId, secondEntryId), response.getEntryIds());
        Assertions.assertEquals(List.of(firstSummary, secondSummary), response.getEntries());
    }

    @Test
    void given_previewEntriesHaveResultCodes_when_bulkActionPreview_then_set_resulted_context() {
        ReflectionTestUtils.setField(service, "bulkActionPreviewGlobalLimit", 1);
        UUID entryId = UUID.randomUUID();
        ApplicationListEntryGetSummaryProjection projection =
                bulkActionPreviewProjection(entryId, 1L);
        final EntryGetSummaryDto summary = stubEntrySummary(projection, entryId);
        final ApplicationListEntryResolutionProjection resolutionProjection =
                mock(ApplicationListEntryResolutionProjection.class);
        final ResolutionCode resolutionCode = mock(ResolutionCode.class);
        final ResultCodeGetSummaryDto resultCode = new ResultCodeGetSummaryDto();

        when(resolutionProjection.getEntryId()).thenReturn(1L);
        when(resolutionProjection.getResolutionCode()).thenReturn(resolutionCode);
        when(applicationListEntryRepository.findResolutionCodesByEntryIds(anyList()))
                .thenReturn(List.of(resolutionProjection));
        when(applicationListEntryMapStructMapper.toResultCodeGetSummaryDto(resolutionCode))
                .thenReturn(resultCode);
        stubBulkActionPreviewSummaryPage(1, projection);

        BulkActionPreviewResponseDto response =
                service.bulkActionPreview(bulkActionPreviewRequest(entryId));

        Assertions.assertEquals(
                List.of(resultCode), response.getEntries().getFirst().getResulted());
        Assertions.assertTrue(response.getEntries().getFirst().getIsResulted());
        Assertions.assertSame(summary, response.getEntries().getFirst());
    }

    @Test
    void given_resultSelectedClosedEntries_when_bulkActionPreview_then_returnOnlyOpenEntries() {
        ReflectionTestUtils.setField(service, "bulkActionPreviewGlobalLimit", 2);
        UUID openEntryId = UUID.randomUUID();
        UUID closedEntryId = UUID.randomUUID();
        ApplicationListEntryGetSummaryProjection openProjection =
                bulkActionPreviewProjection(openEntryId, 1L);
        ApplicationListEntryGetSummaryProjection closedProjection =
                bulkActionPreviewProjection(closedEntryId, 2L);
        final EntryGetSummaryDto openSummary =
                stubEntrySummary(openProjection, openEntryId, ApplicationListStatus.OPEN);
        final EntryGetSummaryDto closedSummary =
                stubEntrySummary(closedProjection, closedEntryId, ApplicationListStatus.CLOSED);

        stubBulkActionPreviewSummaryPage(2, openProjection, closedProjection);
        when(applicationListEntryRepository.findResolutionCodesByEntryIds(anyList()))
                .thenReturn(List.of());

        BulkActionPreviewResponseDto response =
                service.bulkActionPreview(
                        bulkActionPreviewRequest(
                                BulkActionType.RESULT_SELECTED, openEntryId, closedEntryId));

        Assertions.assertEquals(BulkActionType.RESULT_SELECTED, response.getAction());
        Assertions.assertEquals(2, response.getSelectedCount());
        Assertions.assertEquals(1, response.getEligibleCount());
        Assertions.assertEquals(1, response.getIneligibleCount());
        Assertions.assertEquals(List.of(openEntryId), response.getEntryIds());
        Assertions.assertEquals(List.of(openSummary), response.getEntries());
        Assertions.assertFalse(response.getEntries().contains(closedSummary));
    }

    @Test
    void given_resultSelectedOpenResultedEntry_when_bulkActionPreview_then_remainEligible() {
        ReflectionTestUtils.setField(service, "bulkActionPreviewGlobalLimit", 1);
        UUID entryId = UUID.randomUUID();
        ApplicationListEntryGetSummaryProjection projection =
                bulkActionPreviewProjection(entryId, 1L);
        final EntryGetSummaryDto summary =
                stubEntrySummary(projection, entryId, ApplicationListStatus.OPEN);
        final ApplicationListEntryResolutionProjection resolutionProjection =
                mock(ApplicationListEntryResolutionProjection.class);
        final ResolutionCode resolutionCode = mock(ResolutionCode.class);
        final ResultCodeGetSummaryDto resultCode = new ResultCodeGetSummaryDto();

        when(resolutionProjection.getEntryId()).thenReturn(1L);
        when(resolutionProjection.getResolutionCode()).thenReturn(resolutionCode);
        when(applicationListEntryRepository.findResolutionCodesByEntryIds(anyList()))
                .thenReturn(List.of(resolutionProjection));
        when(applicationListEntryMapStructMapper.toResultCodeGetSummaryDto(resolutionCode))
                .thenReturn(resultCode);
        stubBulkActionPreviewSummaryPage(1, projection);

        BulkActionPreviewResponseDto response =
                service.bulkActionPreview(
                        bulkActionPreviewRequest(BulkActionType.RESULT_SELECTED, entryId));

        Assertions.assertEquals(1, response.getSelectedCount());
        Assertions.assertEquals(1, response.getEligibleCount());
        Assertions.assertEquals(0, response.getIneligibleCount());
        Assertions.assertEquals(List.of(entryId), response.getEntryIds());
        Assertions.assertEquals(List.of(summary), response.getEntries());
        Assertions.assertTrue(response.getEntries().getFirst().getIsResulted());
    }

    @Test
    void given_updateNotesClosedEntries_when_bulkActionPreview_then_allEntriesRemainEligible() {
        ReflectionTestUtils.setField(service, "bulkActionPreviewGlobalLimit", 1);
        UUID entryId = UUID.randomUUID();
        ApplicationListEntryGetSummaryProjection projection =
                bulkActionPreviewProjection(entryId, 1L);
        final EntryGetSummaryDto summary =
                stubEntrySummary(projection, entryId, ApplicationListStatus.CLOSED);

        stubBulkActionPreviewSummaryPage(1, projection);
        when(applicationListEntryRepository.findResolutionCodesByEntryIds(anyList()))
                .thenReturn(List.of());

        BulkActionPreviewResponseDto response =
                service.bulkActionPreview(bulkActionPreviewRequest(entryId));

        Assertions.assertEquals(BulkActionType.UPDATE_NOTES, response.getAction());
        Assertions.assertEquals(1, response.getSelectedCount());
        Assertions.assertEquals(1, response.getEligibleCount());
        Assertions.assertEquals(0, response.getIneligibleCount());
        Assertions.assertEquals(List.of(entryId), response.getEntryIds());
        Assertions.assertEquals(List.of(summary), response.getEntries());
    }

    @Test
    void given_validPayload_when_updateEntry_then_save_and_return_response() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();

        EntryUpdateDto dto = new EntryUpdateDto();
        dto.setWordingFields(List.of());

        final PayloadForUpdateEntry payload = new PayloadForUpdateEntry(dto, listId, entryId);

        ApplicationListEntry applicationListEntry = new ApplicationListEntry();
        applicationListEntry.setUuid(entryId);
        applicationListEntry.setId(123L);
        applicationListEntry.setVersion(1L);

        ApplicationList applicationList = new ApplicationList();
        applicationList.setUuid(listId);

        WordingTemplateSentence wordingTemplateSentence = mock(WordingTemplateSentence.class);
        SubstitutedSentence substitutedSentence = mock(SubstitutedSentence.class);
        when(wordingTemplateSentence.substitute(anyList())).thenReturn(substitutedSentence);
        when(substitutedSentence.getSubstitutedString()).thenReturn("wording");

        updateSuccess =
                new UpdateApplicationEntryValidationSuccess(
                        wordingTemplateSentence,
                        new ApplicationCode(),
                        null,
                        null,
                        applicationList,
                        applicationListEntry);

        when(applicationListEntryRepository.save(any(ApplicationListEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        EntryGetDetailDto entryGetDetailDto = new EntryGetDetailDto();

        when(applicationListEntryMapStructMapper.toEntryGetDetailDto(
                        eq(applicationListEntry), anyList(), eq(null), anyList(), eq(null)))
                .thenReturn(entryGetDetailDto);

        MatchResponse<EntryGetDetailDto> response = service.updateEntry(payload);

        Assertions.assertNotNull(response);
        verify(applicationListEntryRepository, atLeastOnce()).save(applicationListEntry);
    }

    private List<AsyncJobsAppListEntry> createAsyncJobAppListEntries(UUID jobId, int count) {
        return IntStream.range(0, count)
                .mapToObj(
                        i -> {
                            AsyncJobsAppListEntry entry = new AsyncJobsAppListEntry();
                            entry.setAsyncJobId(jobId);
                            entry.setAppListEntryId(UUID.randomUUID());
                            return entry;
                        })
                .toList();
    }

    private BulkActionPreviewRequestDto bulkActionPreviewRequest(UUID... entryIds) {
        return bulkActionPreviewRequest(BulkActionType.UPDATE_NOTES, entryIds);
    }

    private BulkActionPreviewRequestDto bulkActionPreviewRequest(
            BulkActionType action, UUID... entryIds) {
        return new BulkActionPreviewRequestDto()
                .action(action)
                .selection(
                        new BulkActionSelectionDto()
                                .selectionType(BulkActionSelectionType.IDS)
                                .entryIds(List.of(entryIds)));
    }

    private BulkActionPreviewRequestDto bulkActionPreviewFilterRequest(
            EntryGetFilterDto filter, List<String> sort, List<UUID> excludedEntryIds) {
        return new BulkActionPreviewRequestDto()
                .action(BulkActionType.UPDATE_NOTES)
                .selection(
                        new BulkActionSelectionDto()
                                .selectionType(BulkActionSelectionType.FILTER)
                                .filter(filter)
                                .sort(sort)
                                .excludedEntryIds(excludedEntryIds));
    }

    private void stubBulkActionPreviewSummaryPage(
            long totalElements, ApplicationListEntryGetSummaryProjection... projections) {
        when(applicationListEntryRepository.searchForBulkActionPreviewSummary(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        anyBoolean(),
                        anyList(),
                        anyBoolean(),
                        anyList(),
                        any(Pageable.class)))
                .thenReturn(
                        new PageImpl<>(List.of(projections), Pageable.unpaged(), totalElements));
    }

    private ApplicationListEntryGetSummaryProjection bulkActionPreviewProjection(
            UUID entryId, Long id) {
        ApplicationListEntryGetSummaryProjection projection =
                mock(ApplicationListEntryGetSummaryProjection.class);
        when(projection.getUuid()).thenReturn(entryId.toString());
        when(projection.getId()).thenReturn(id);
        return projection;
    }

    private EntryGetSummaryDto stubEntrySummary(
            ApplicationListEntryGetSummaryProjection projection, UUID entryId) {
        return stubEntrySummary(projection, entryId, ApplicationListStatus.OPEN);
    }

    private EntryGetSummaryDto stubEntrySummary(
            ApplicationListEntryGetSummaryProjection projection,
            UUID entryId,
            ApplicationListStatus status) {
        EntryGetSummaryDto summary = new EntryGetSummaryDto().id(entryId).status(status);
        when(applicationListEntryMapStructMapper.toEntrySummary(projection)).thenReturn(summary);
        return summary;
    }
}
