package uk.gov.hmcts.appregister.applicationentry.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import lombok.Setter;

import org.instancio.Instancio;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapStructMapper;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryMapStructMapperImpl;
import uk.gov.hmcts.appregister.applicationentry.validator.CreateApplicationEntryValidationSuccess;
import uk.gov.hmcts.appregister.applicationentry.validator.CreateApplicationEntryValidator;
import uk.gov.hmcts.appregister.applicationlist.audit.AppListAuditOperation;
import uk.gov.hmcts.appregister.applicationlist.service.ApplicationListServiceImpl;
import uk.gov.hmcts.appregister.applicationlist.service.ApplicationListServiceImplTest;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationUpdateListLocationValidator;
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
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeStatusRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryOfficialRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NameAddressRepository;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.common.model.PayloadForUpdate;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryGetSummaryProjection;
import uk.gov.hmcts.appregister.data.NameAddressTestData;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ApplicationEntryServiceImplTest {

    @Mock private FeeRepository feeRepository;

    @Mock private ApplicationListRepository applicationListRepository;

    @Mock private ApplicationCodeRepository applicationCodeRepository;

    @Mock private ApplicationListEntryRepository applicationListEntryRepository;

    @Mock private StandardApplicantRepository standardApplicantRepository;

    @Mock
    private AppListEntryFeeStatusRepository appListEntryFeeStatusRepository;

    @Mock
    private NameAddressRepository nameAddressRepository;

    @Mock
    private AppListEntryOfficialRepository appListEntryOfficialRepository;

    @Mock
    private AppListEntryFeeRepository appListEntryFeeRepository;

    @Mock
    private Clock clock;

    @Mock
    private ApplicationListEntryMapStructMapper mapper;

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

    @Mock
    private ApplicationListEntryMapStructMapper applicationListEntryMapStructMapper;

    @Mock
    private ApplicationListEntryEntityMapper applicationListEntryEntityMapper;

    @Mock
    private List<AuditOperationLifecycleListener> auditLifecycleListeners;

    private ApplicationEntryService service;

    @Spy
    private DummyCreateApplicationEntryValidator createApplicationEntryValidator
        = new DummyCreateApplicationEntryValidator(applicationListRepository,
                                                   applicationCodeRepository,
                                                   feeRepository,
                                                   clock,
                                                   standardApplicantRepository
                                                   );

    @Spy
    private final ApplicationListEntryEntityMapper entryEntityMapper =
        new ApplicationListEntryEntityMapperImpl();

    @Spy private final PageMapper pageMapper = new PageMapper();

    @BeforeEach
    void setUp() {
        service =
            new ApplicationEntryServiceImpl(
                mapper,
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
                applicationListEntryEntityMapper,
                auditLifecycleListeners);
    }

    @Test
    public void testSearchForGetSummary() {
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);

        EntryGetFilterDto entryGetFilterDto =
                Instancio.of(EntryGetFilterDto.class).withSettings(settings).create();
        ApplicationListEntryGetSummaryProjection applicationListEntryGetSummaryProjection =
                mock(ApplicationListEntryGetSummaryProjection.class);

        when(applicationListEntryGetSummaryProjection.getApplicationOrganisation())
                .thenReturn("org1");
        when(applicationListEntryGetSummaryProjection.getApplicantSurname()).thenReturn("surname");
        when(applicationListEntryGetSummaryProjection.getAnameaddress())
                .thenReturn(new NameAddress());
        when(applicationListEntryGetSummaryProjection.getRnameaddress())
                .thenReturn(new NameAddress());
        when(applicationListEntryGetSummaryProjection.getDateofal()).thenReturn(LocalDate.now());

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
        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        EntryCreateDto entryCreateDto =
            Instancio.of(EntryCreateDto.class)
                .withSettings(settings)
                .create();

        PayloadForCreate<EntryCreateDto> payload
            = PayloadForCreate.<EntryCreateDto>builder().id(UUID.randomUUID()).data(entryCreateDto).build();


        NameAddressTestData nameAddressTestData = new NameAddressTestData();
        NameAddress applicant = nameAddressTestData.someComplete();

        NameAddress respondent = nameAddressTestData.someComplete();

        when(applicationListEntryEntityMapper.mapApplicant(entryCreateDto.getApplicant())).thenReturn(applicant);

        when(applicationListEntryEntityMapper.mapRespondent(entryCreateDto.getRespondent())).thenReturn(respondent);

        MatchResponse<EntryGetDetailDto> response = service.createEntry(payload);

        verify(nameAddressRepository, times(1)).save(applicant);
        verify(nameAddressRepository, times(1)).save(respondent);
    }

    @Setter
    class DummyCreateApplicationEntryValidator extends CreateApplicationEntryValidator {
        private CreateApplicationEntryValidationSuccess success;

        public DummyCreateApplicationEntryValidator(
            ApplicationListRepository applicationListRepository,
            ApplicationCodeRepository applicationCodeRepository,
            FeeRepository feeRepository,
            Clock clock,
            StandardApplicantRepository standardApplicantRepository) {
            super(applicationListRepository, applicationCodeRepository, feeRepository,
                  clock, standardApplicantRepository);
        }

        @Override
        public <R> R validate(PayloadForCreate<EntryCreateDto> validatable, BiFunction<PayloadForCreate<EntryCreateDto>,
            CreateApplicationEntryValidationSuccess, R> validateSuccess) {
            return validateSuccess.apply(validatable, success);
        }
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
}
