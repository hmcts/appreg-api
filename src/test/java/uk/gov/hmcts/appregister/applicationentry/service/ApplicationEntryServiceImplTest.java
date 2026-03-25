package uk.gov.hmcts.appregister.applicationentry.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryEntityMapper;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryEntityMapperImpl;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapper;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapperImpl;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForUpdateEntry;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadGetEntryInList;
import uk.gov.hmcts.appregister.applicationentry.validator.CreateApplicationEntryValidationSuccess;
import uk.gov.hmcts.appregister.applicationentry.validator.CreateApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.GetApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.GetApplicationListEntriesValidator;
import uk.gov.hmcts.appregister.applicationentry.validator.GetEntryValidationSuccess;
import uk.gov.hmcts.appregister.applicationentry.validator.UpdateApplicationEntryValidationSuccess;
import uk.gov.hmcts.appregister.applicationentry.validator.UpdateApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationlist.audit.AppListAuditOperation;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.applicationlist.validator.MoveEntriesValidationSuccess;
import uk.gov.hmcts.appregister.applicationlist.validator.MoveEntriesValidator;
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
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.AppListEntrySequenceMapping;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.Fee;
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
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NameAddressRepository;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapper;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapperImpl;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryGetSummaryProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryResolutionProjection;
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
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.EntryApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

@Slf4j
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ApplicationEntryServiceImplTest {

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

    @Mock private Clock clock;

    private CreateApplicationEntryValidationSuccess success;

    private UpdateApplicationEntryValidationSuccess updateSuccess;

    private GetEntryValidationSuccess getEntryValidationSuccess;

    // A null match provider that returns a null etag
    private static MatchProvider NULL_MATCH_PROVIDER =
            new MatchProvider() {
                @Override
                public String getEtag() {
                    return null;
                }
            };

    // Services
    @Spy private MatchService matchService = new MatchServiceImpl(NULL_MATCH_PROVIDER);

    // Audit
    @Spy
    private final AuditOperationService auditOperationService = new DummyAuditOperationService();

    @Mock private ApplicationListEntryMapper applicationListEntryMapStructMapper;

    @Mock private ApplicationListEntryEntityMapper applicationListEntryEntityMapper;

    @Mock private List<AuditOperationLifecycleListener> auditLifecycleListeners;

    @Mock private EntityManager entityManager;

    @Mock private ApplicantMapper applicantMapper;

    private ApplicationEntryService service;

    @Spy
    private DummyCreateApplicationEntryValidator createApplicationEntryValidator =
            new DummyCreateApplicationEntryValidator(
                    applicationListRepository,
                    applicationCodeRepository,
                    feeRepository,
                    clock,
                    standardApplicantRepository);

    @Spy
    private DummyMoveEntriesValidator moveEntriesValidator =
            new DummyMoveEntriesValidator(applicationListRepository);

    @Spy
    private final ApplicationListEntryEntityMapper entryEntityMapper =
            new ApplicationListEntryEntityMapperImpl();

    @Spy private final PageMapper pageMapper = new PageMapper();

    @Spy
    private DummyUpdateApplicationEntryValidator updateApplicationEntryValidator =
            new DummyUpdateApplicationEntryValidator(
                    applicationListRepository,
                    applicationCodeRepository,
                    feeRepository,
                    clock,
                    standardApplicantRepository,
                    applicationListEntryRepository);

    @Spy
    private GetApplicationEntryValidator getEntryValidator =
            new DummyGetApplicationEntryValidator(
                    applicationListRepository, applicationListEntryRepository);

    @Spy
    private GetApplicationListEntriesValidator getApplicationListEntriesValidator =
            new DummyGetApplicationListEntriesValidator(applicationListRepository);

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(Clock.systemUTC().getZone());

        service =
                new ApplicationEntryServiceImpl(
                        applicationListEntryRepository,
                        feeRepository,
                        pageMapper,
                        createApplicationEntryValidator,
                        updateApplicationEntryValidator,
                        moveEntriesValidator,
                        matchService,
                        auditOperationService,
                        appListEntryFeeStatusRepository,
                        nameAddressRepository,
                        appListEntryOfficialRepository,
                        appListEntryFeeRepository,
                        standardApplicantRepository,
                        appListEntrySequenceMappingRepository,
                        applicationListEntryMapStructMapper,
                        applicantMapper,
                        applicationListEntryEntityMapper,
                        entityManager,
                        getEntryValidator,
                        getApplicationListEntriesValidator,
                        clock);
    }

    @Test
    public void testSearchForGetSummary() {
        ApplicationListEntryMapper mapStructMapper = new ApplicationListEntryMapperImpl();
        mapStructMapper.setApplicantMapper(new ApplicantMapperImpl());
        service =
                new ApplicationEntryServiceImpl(
                        applicationListEntryRepository,
                        feeRepository,
                        pageMapper,
                        createApplicationEntryValidator,
                        updateApplicationEntryValidator,
                        moveEntriesValidator,
                        matchService,
                        auditOperationService,
                        appListEntryFeeStatusRepository,
                        nameAddressRepository,
                        appListEntryOfficialRepository,
                        appListEntryFeeRepository,
                        standardApplicantRepository,
                        appListEntrySequenceMappingRepository,
                        mapStructMapper,
                        applicantMapper,
                        applicationListEntryEntityMapper,
                        entityManager,
                        getEntryValidator,
                        getApplicationListEntriesValidator,
                        clock);

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
        when(applicationListEntryGetSummaryProjection.getDateOfAl()).thenReturn(LocalDate.now());

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
                        eq(null),
                        eq(true),
                        eq(entryGetFilterDto.getDate()),
                        eq(entryGetFilterDto.getCourtCode()),
                        eq(entryGetFilterDto.getOtherLocationDescription()),
                        eq(entryGetFilterDto.getCjaCode()),
                        eq(entryGetFilterDto.getApplicantOrganisation()),
                        eq(entryGetFilterDto.getApplicantSurname()),
                        eq(null),
                        eq(entryGetFilterDto.getStandardApplicantCode()),
                        eq(Status.fromValue(entryGetFilterDto.getStatus().getValue())),
                        eq(entryGetFilterDto.getRespondentOrganisation()),
                        eq(entryGetFilterDto.getRespondentSurname()),
                        eq(null),
                        eq(entryGetFilterDto.getRespondentPostcode()),
                        eq(entryGetFilterDto.getAccountReference()),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(mockPage)))
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
        fee.setId(-2L);

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();

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
                        eq(appList)))
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

        when(feeRepository.findByReferenceBetweenDateWithOffsite(
                        eq(code.getFeeReference()),
                        notNull(),
                        eq(entryCreateDto.getHasOffsiteFee())))
                .thenReturn(List.of(fee));

        // setup validation success response containing all validated data
        success =
                CreateApplicationEntryValidationSuccess.builder()
                        .wordingSentence(WordingTemplateSentence.with(code.getWording()))
                        .fee(fee)
                        .applicationCode(code)
                        .sa(sa)
                        .applicationList(appList)
                        .build();

        AppListEntryFeeId appListFee = new AppListEntryFeeId();
        appListFee.setAppListEntryId(applicationListEntry.getId());
        appListFee.setFeeId(fee.getId());

        ArgumentCaptor<AppListEntryFeeId> captor = ArgumentCaptor.forClass(AppListEntryFeeId.class);
        when(appListEntryFeeRepository.save(captor.capture())).thenReturn(appListFee);

        // dummy the mapping of the response

        EntryGetDetailDto entryGetDetailDto =
                Instancio.of(EntryGetDetailDto.class).withSettings(settings).create();
        when(applicationListEntryMapStructMapper.toEntryGetDetailDto(
                        applicationListEntry, statusLst, fee, officialLst, sa))
                .thenReturn(entryGetDetailDto);

        // run the test
        MatchResponse<EntryGetDetailDto> response = service.createEntry(payload);

        // now assert the response is mapped correctly
        Assertions.assertEquals(entryGetDetailDto, response.getPayload());
        Assertions.assertNotNull(response.getEtag());

        ArgumentCaptor<NameAddress> appCaptorName = ArgumentCaptor.forClass(NameAddress.class);

        // verify that the applicant and respondent are saved
        verify(nameAddressRepository, times(2)).save(appCaptorName.capture());

        // verify app list entry is saved
        ArgumentCaptor<ApplicationListEntry> appListEntryCaptor =
                ArgumentCaptor.forClass(ApplicationListEntry.class);

        verify(applicationListEntryRepository, times(1)).save(appListEntryCaptor.capture());
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

        Assertions.assertEquals(-1, captor.getValue().getAppListEntryId());
        Assertions.assertEquals(-2, captor.getValue().getFeeId());

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
        fee.setId(2L);

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();

        AppListEntryFeeStatusTestData appListEntryFeeStatusTestData =
                new AppListEntryFeeStatusTestData();
        List<AppListEntryFeeStatus> statusLst = new ArrayList<>();
        for (FeeStatus feeStatus : entryCreateDto.getFeeStatuses()) {
            AppListEntryFeeStatus appStatus = appListEntryFeeStatusTestData.someComplete();
            when(applicationListEntryEntityMapper.toFeeStatus(feeStatus, applicationListEntry))
                    .thenReturn(appStatus);
            appStatus.setId(-1L);
            when(appListEntryFeeStatusRepository.save(appStatus)).thenReturn(appStatus);
            statusLst.add(appStatus);
        }

        AppListEntryOfficialTestData officialTestData = new AppListEntryOfficialTestData();
        List<AppListEntryOfficial> officialLst = new ArrayList<>();
        for (Official appOfficial : entryCreateDto.getOfficials()) {
            AppListEntryOfficial official = officialTestData.someComplete();
            when(applicationListEntryEntityMapper.toOfficial(appOfficial, applicationListEntry))
                    .thenReturn(official);
            official.setId(-1L);
            when(appListEntryOfficialRepository.save(official)).thenReturn(official);
            officialLst.add(official);
        }

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
                        eq(appList)))
                .thenReturn(applicationListEntry);

        when(applicantMapper.toApplicant(entryCreateDto.getApplicant())).thenReturn(applicant);
        when(applicantMapper.toRespondent(entryCreateDto.getRespondent())).thenReturn(respondent);
        when(nameAddressRepository.save(respondent)).thenReturn(respondent);
        when(applicationListEntryRepository.save(applicationListEntry))
                .thenReturn(applicationListEntry);

        success =
                CreateApplicationEntryValidationSuccess.builder()
                        .wordingSentence(WordingTemplateSentence.with(code.getWording()))
                        .fee(fee)
                        .applicationCode(code)
                        .sa(sa)
                        .applicationList(appList)
                        .build();

        EntryGetDetailDto entryGetDetailDto =
                Instancio.of(EntryGetDetailDto.class).withSettings(settings).create();
        when(applicationListEntryMapStructMapper.toEntryGetDetailDto(
                        applicationListEntry, statusLst, fee, officialLst, sa))
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
        MatchResponse<EntryGetDetailDto> response = service.createEntry(payload);

        // assertions

        // application list entry saved and sequence set to 1
        ArgumentCaptor<ApplicationListEntry> appListEntryCaptor =
                ArgumentCaptor.forClass(ApplicationListEntry.class);
        verify(applicationListEntryRepository, times(1)).save(appListEntryCaptor.capture());
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
        fee.setId(2L);

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        EntryCreateDto entryCreateDto =
                Instancio.of(EntryCreateDto.class).withSettings(settings).create();

        AppListEntryFeeStatusTestData appListEntryFeeStatusTestData =
                new AppListEntryFeeStatusTestData();
        List<AppListEntryFeeStatus> statusLst = new ArrayList<>();
        for (FeeStatus feeStatus : entryCreateDto.getFeeStatuses()) {
            AppListEntryFeeStatus appStatus = appListEntryFeeStatusTestData.someComplete();
            when(applicationListEntryEntityMapper.toFeeStatus(feeStatus, applicationListEntry))
                    .thenReturn(appStatus);
            appStatus.setId(-1L);
            when(appListEntryFeeStatusRepository.save(appStatus)).thenReturn(appStatus);
            statusLst.add(appStatus);
        }

        AppListEntryOfficialTestData officialTestData = new AppListEntryOfficialTestData();
        List<AppListEntryOfficial> officialLst = new ArrayList<>();
        for (Official appOfficial : entryCreateDto.getOfficials()) {
            AppListEntryOfficial official = officialTestData.someComplete();
            when(applicationListEntryEntityMapper.toOfficial(appOfficial, applicationListEntry))
                    .thenReturn(official);
            official.setId(-1L);
            when(appListEntryOfficialRepository.save(official)).thenReturn(official);
            officialLst.add(official);
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
                        eq(appList)))
                .thenReturn(applicationListEntry);

        when(applicantMapper.toApplicant(entryCreateDto.getApplicant())).thenReturn(applicant);
        when(applicantMapper.toRespondent(entryCreateDto.getRespondent())).thenReturn(respondent);
        when(nameAddressRepository.save(respondent)).thenReturn(respondent);
        when(applicationListEntryRepository.save(applicationListEntry))
                .thenReturn(applicationListEntry);

        success =
                CreateApplicationEntryValidationSuccess.builder()
                        .wordingSentence(WordingTemplateSentence.with(code.getWording()))
                        .fee(fee)
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
                        applicationListEntry, statusLst, fee, officialLst, sa))
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
        verify(applicationListEntryRepository, times(1)).save(appListEntryCaptor.capture());
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
        verify(feeRepository, times(0)).findByIdsBetweenDate(notNull(), notNull());
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
        when(applicationListEntryGetSummaryProjection.getDateOfAl()).thenReturn(LocalDate.now());

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
                        eq(applicationList.getUuid()),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(entryGetFilterDto.getApplicantName()),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(entryGetFilterDto.getRespondentName()),
                        eq(entryGetFilterDto.getRespondentPostcode()),
                        eq(entryGetFilterDto.getAccountReference()),
                        eq(entryGetFilterDto.getApplicationTitle()),
                        eq(entryGetFilterDto.getFeeRequired()),
                        eq(entryGetFilterDto.getSequenceNumber()),
                        eq(mockPage)))
                .thenReturn(dbPage);

        EntryGetSummaryDto summaryDto = new EntryGetSummaryDto();
        summaryDto.setResulted(new ArrayList<>());
        summaryDto.setIsResulted(false);

        when(applicationListEntryMapStructMapper.toEntrySummary(any())).thenReturn(summaryDto);

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
                        eq(applicationList.getUuid()),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(entryGetFilterDto.getApplicantName()),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(entryGetFilterDto.getRespondentName()),
                        eq(entryGetFilterDto.getRespondentPostcode()),
                        eq(entryGetFilterDto.getAccountReference()),
                        eq(entryGetFilterDto.getApplicationTitle()),
                        eq(entryGetFilterDto.getFeeRequired()),
                        eq(entryGetFilterDto.getSequenceNumber()),
                        eq(mockPage)))
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
    void move_performsBulkUpdate_whenValidRequest() {
        ApplicationList targetList = new ApplicationList();
        targetList.setUuid(UUID.randomUUID());

        // Two entry UUIDs requested
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetList.getUuid());
        dto.setEntryIds(Set.of(id1, id2));

        MoveEntriesValidationSuccess success = new MoveEntriesValidationSuccess();
        success.setTargetList(targetList);
        moveEntriesValidator.setSuccess(success);

        // Mock repository to return rowsUpdated == requested size (2)
        UUID sourceListId = UUID.randomUUID();
        when(applicationListEntryRepository.bulkMoveByUuidAndSourceList(
                        anySet(), eq(targetList), eq(sourceListId)))
                .thenReturn(2);

        // Act - should not throw
        service.move(sourceListId, dto);

        // Verify the bulk update call was invoked once with the same source and target that the
        // service was called with
        verify(applicationListEntryRepository, times(1))
                .bulkMoveByUuidAndSourceList(anySet(), eq(targetList), eq(sourceListId));
    }

    @Test
    void move_throws_whenBulkUpdateAffectsFewerRowsThanRequested() {
        ApplicationList targetList = new ApplicationList();
        targetList.setUuid(UUID.randomUUID());

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(targetList.getUuid());
        dto.setEntryIds(Set.of(id1, id2));

        MoveEntriesValidationSuccess success = new MoveEntriesValidationSuccess();
        success.setTargetList(targetList);
        moveEntriesValidator.setSuccess(success);

        // Simulate DB updated only 1 row even though 2 were requested
        UUID sourceListId = UUID.randomUUID();
        when(applicationListEntryRepository.bulkMoveByUuidAndSourceList(
                        anySet(), eq(targetList), eq(sourceListId)))
                .thenReturn(1);

        assertThatThrownBy(() -> service.move(sourceListId, dto))
                .isInstanceOf(AppRegistryException.class)
                .satisfies(
                        ex ->
                                Assertions.assertEquals(
                                        ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST,
                                        ((AppRegistryException) ex).getCode()));
    }

    @Test
    void move_returns404_whenSourceListDoesNotExist() {
        doThrow(
                        new AppRegistryException(
                                ApplicationListError.SOURCE_LIST_NOT_FOUND,
                                "No source application list found for UUID"))
                .when(moveEntriesValidator)
                .validate(any(MoveEntriesDto.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();

        assertThatThrownBy(() -> service.move(UUID.randomUUID(), dto))
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
                .validate(any(MoveEntriesDto.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(UUID.randomUUID());

        assertThatThrownBy(() -> service.move(UUID.randomUUID(), dto))
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
                .validate(any(MoveEntriesDto.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();

        assertThatThrownBy(() -> service.move(UUID.randomUUID(), dto))
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
                .validate(any(MoveEntriesDto.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setTargetListId(UUID.randomUUID());

        assertThatThrownBy(() -> service.move(UUID.randomUUID(), dto))
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
                .validate(any(MoveEntriesDto.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();

        assertThatThrownBy(() -> service.move(UUID.randomUUID(), dto))
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
                .validate(any(MoveEntriesDto.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setEntryIds(Set.of());

        assertThatThrownBy(() -> service.move(UUID.randomUUID(), dto))
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
                .validate(any(MoveEntriesDto.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setEntryIds(Set.of(UUID.randomUUID()));

        assertThatThrownBy(() -> service.move(UUID.randomUUID(), dto))
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
                .validate(any(MoveEntriesDto.class), any());

        MoveEntriesDto dto = new MoveEntriesDto();
        dto.setEntryIds(Set.of(UUID.randomUUID()));

        assertThatThrownBy(() -> service.move(UUID.randomUUID(), dto))
                .isInstanceOf(AppRegistryException.class)
                .extracting(e -> ((AppRegistryException) e).getCode().getCode().getHttpCode())
                .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @Setter
    class DummyCreateApplicationEntryValidator extends CreateApplicationEntryValidator {

        public DummyCreateApplicationEntryValidator(
                ApplicationListRepository applicationListRepository,
                ApplicationCodeRepository applicationCodeRepository,
                FeeRepository feeRepository,
                Clock clock,
                StandardApplicantRepository standardApplicantRepository) {
            super(
                    applicationListRepository,
                    applicationCodeRepository,
                    feeRepository,
                    clock,
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
            return processAudit(
                    oldValue, auditType, execution, (AuditOperationLifecycleListener) null);
        }

        @Override
        public <T, E extends Keyable> T processAudit(
                AuditOperation auditType,
                Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution) {
            return processAudit(null, auditType, execution);
        }

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

    class DummyUpdateApplicationEntryValidator extends UpdateApplicationEntryValidator {
        public DummyUpdateApplicationEntryValidator(
                ApplicationListRepository applicationListRepository,
                ApplicationCodeRepository applicationCodeRepository,
                FeeRepository feeRepository,
                Clock clock,
                StandardApplicantRepository standardApplicantRepository,
                ApplicationListEntryRepository applicationListEntryRepository) {
            super(
                    applicationListRepository,
                    applicationCodeRepository,
                    feeRepository,
                    clock,
                    standardApplicantRepository,
                    applicationListEntryRepository);
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

    @Setter
    static class DummyMoveEntriesValidator extends MoveEntriesValidator {

        private MoveEntriesValidationSuccess success;

        public DummyMoveEntriesValidator(ApplicationListRepository applicationListRepository) {
            super(applicationListRepository);
        }

        @Override
        public <R> R validate(
                MoveEntriesDto dto,
                java.util.function.BiFunction<MoveEntriesDto, MoveEntriesValidationSuccess, R>
                        createSupplier) {

            return createSupplier.apply(dto, success);
        }

        @Override
        public DummyMoveEntriesValidator withSourceList(UUID id) {
            return this;
        }
    }
}
