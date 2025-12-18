package uk.gov.hmcts.appregister.applicationentry.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import uk.gov.hmcts.appregister.applicationentry.validator.CreateApplicationEntryValidationSuccess;
import uk.gov.hmcts.appregister.applicationentry.validator.CreateApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationlist.audit.AppListAuditOperation;
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
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeStatusRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryOfficialRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NameAddressRepository;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapper;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapperImpl;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryGetSummaryProjection;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.data.AppListEntryFeeStatusTestData;
import uk.gov.hmcts.appregister.data.AppListEntryOfficialTestData;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.data.FeeTestData;
import uk.gov.hmcts.appregister.data.NameAddressTestData;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.Official;

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

    @Mock private Clock clock;

    @Mock private ApplicationListEntryMapper mapper;

    private CreateApplicationEntryValidationSuccess success;

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
    private final ApplicationListEntryEntityMapper entryEntityMapper =
            new ApplicationListEntryEntityMapperImpl();

    @Spy private final PageMapper pageMapper = new PageMapper();

    @BeforeEach
    void setUp() {
        service =
                new ApplicationEntryServiceImpl(
                        applicationListEntryRepository,
                        pageMapper,
                        createApplicationEntryValidator,
                        matchService,
                        auditOperationService,
                        appListEntryFeeStatusRepository,
                        nameAddressRepository,
                        appListEntryOfficialRepository,
                        appListEntryFeeRepository,
                        applicationListEntryMapStructMapper,
                        applicantMapper,
                        applicationListEntryEntityMapper,
                        auditLifecycleListeners,
                        entityManager);
    }

    @Test
    public void testSearchForGetSummary() {
        ApplicationListEntryMapper mapStructMapper = new ApplicationListEntryMapperImpl();
        mapStructMapper.setApplicantMapper(new ApplicantMapperImpl());
        service =
                new ApplicationEntryServiceImpl(
                        applicationListEntryRepository,
                        pageMapper,
                        createApplicationEntryValidator,
                        matchService,
                        auditOperationService,
                        appListEntryFeeStatusRepository,
                        nameAddressRepository,
                        appListEntryOfficialRepository,
                        appListEntryFeeRepository,
                        mapStructMapper,
                        applicantMapper,
                        applicationListEntryEntityMapper,
                        auditLifecycleListeners,
                        entityManager);

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
        when(applicationListEntryGetSummaryProjection.getResult()).thenReturn(null);
        when(applicationListEntryGetSummaryProjection.getFeeRequired()).thenReturn(YesOrNo.NO);
        when(applicationListEntryGetSummaryProjection.getStatus()).thenReturn(Status.OPEN);

        Pageable mockPage = mock(Pageable.class);
        when(mockPage.getPageNumber()).thenReturn(1);

        Page<ApplicationListEntryGetSummaryProjection> page =
                new PageImpl<ApplicationListEntryGetSummaryProjection>(
                        List.of(applicationListEntryGetSummaryProjection), mockPage, 1);

        when(applicationListEntryRepository.searchForGetSummary(
                        eq(true),
                        eq(entryGetFilterDto.getDate()),
                        eq(entryGetFilterDto.getCourtCode()),
                        eq(entryGetFilterDto.getOtherLocationDescription()),
                        eq(entryGetFilterDto.getCjaCode()),
                        eq(entryGetFilterDto.getApplicantOrganisation()),
                        eq(entryGetFilterDto.getApplicantSurname()),
                        eq(entryGetFilterDto.getStandardApplicantCode()),
                        eq(Status.fromValue(entryGetFilterDto.getStatus().getValue())),
                        eq(entryGetFilterDto.getRespondentOrganisation()),
                        eq(entryGetFilterDto.getRespondentSurname()),
                        eq(entryGetFilterDto.getRespondentPostcode()),
                        eq(entryGetFilterDto.getAccountReference()),
                        eq(mockPage)))
                .thenReturn(page);

        // execute
        EntryPage entryPage = service.search(entryGetFilterDto, mockPage);

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

        entryCreateDto.setWordingFields(List.of("wording1", "wording2", "wording3"));
        code.setWording(
                "Test template {TEXT|Applicant officer1|10} and second template {TEXT|Applicant officer1|10} and third"
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
}
