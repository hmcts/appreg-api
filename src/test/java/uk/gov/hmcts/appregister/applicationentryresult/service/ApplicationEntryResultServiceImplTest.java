package uk.gov.hmcts.appregister.applicationentryresult.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence.with;

import jakarta.persistence.EntityManager;
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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.applicationentryresult.mapper.ApplicationListEntryResultEntityMapper;
import uk.gov.hmcts.appregister.applicationentryresult.mapper.ApplicationListEntryResultMapper;
import uk.gov.hmcts.appregister.applicationentryresult.model.ListEntryResultDeleteArgs;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForCreateEntryResult;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForUpdateEntryResult;
import uk.gov.hmcts.appregister.applicationentryresult.validator.ApplicationEntryResultCreationValidator;
import uk.gov.hmcts.appregister.applicationentryresult.validator.ApplicationEntryResultDeletionValidator;
import uk.gov.hmcts.appregister.applicationentryresult.validator.ApplicationEntryResultUpdateValidator;
import uk.gov.hmcts.appregister.applicationentryresult.validator.ListEntryResultCreateValidationSuccess;
import uk.gov.hmcts.appregister.applicationentryresult.validator.ListEntryResultDeleteValidationSuccess;
import uk.gov.hmcts.appregister.applicationentryresult.validator.ListEntryResultUpdateValidationSuccess;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.event.StartEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.concurrency.MatchProvider;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.concurrency.MatchService;
import uk.gov.hmcts.appregister.common.concurrency.MatchServiceImpl;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;
import uk.gov.hmcts.appregister.generated.model.ResultGetDto;
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

@ExtendWith(MockitoExtension.class)
public class ApplicationEntryResultServiceImplTest {

    @Mock private ApplicationListRepository applicationListRepository;
    @Mock private ApplicationListEntryRepository applicationListEntryRepository;
    @Mock private AppListEntryResolutionRepository appListEntryResolutionRepository;
    @Mock private ResolutionCodeRepository resolutionCodeRepository;
    @Mock private AuditOperationLifecycleListener auditOperationLifecycleListener;
    @Mock private ApplicationListEntryResultMapper applicationListEntryResultMapper;
    @Mock private ApplicationListEntryResultEntityMapper applicationListEntryResultEntityMapper;
    @Mock private EntityManager entityManager;
    @Mock private UserProvider userProvider;

    @Spy
    private DummyApplicationEntryResultDeletionValidator deletionValidator =
            new DummyApplicationEntryResultDeletionValidator(
                    applicationListRepository,
                    applicationListEntryRepository,
                    resolutionCodeRepository,
                    appListEntryResolutionRepository);

    @Spy
    private DummyApplicationEntryResultCreationValidator creationValidator =
            new DummyApplicationEntryResultCreationValidator(
                    applicationListRepository,
                    applicationListEntryRepository,
                    resolutionCodeRepository);

    @Spy
    private DummyApplicationEntryResultUpdateValidator updateValidator =
            new DummyApplicationEntryResultUpdateValidator(
                    applicationListRepository,
                    applicationListEntryRepository,
                    resolutionCodeRepository,
                    appListEntryResolutionRepository);

    @Spy
    private final AuditOperationService auditOperationService = new DummyAuditOperationService();

    // A null match provider that returns a null etag
    private static final MatchProvider NULL_MATCH_PROVIDER = () -> null;

    @Spy private MatchService matchService = new MatchServiceImpl(NULL_MATCH_PROVIDER);

    private ApplicationEntryResultServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new ApplicationEntryResultServiceImpl(
                        appListEntryResolutionRepository,
                        deletionValidator,
                        creationValidator,
                        updateValidator,
                        matchService,
                        auditOperationService,
                        List.of(auditOperationLifecycleListener),
                        applicationListEntryResultMapper,
                        applicationListEntryResultEntityMapper,
                        entityManager,
                        userProvider);
    }

    @Test
    void createAnEntryResult() {
        // setup the user that all tests will represent
        when(userProvider.getEmail()).thenReturn("myemail@domain.com");

        Settings settings = Settings.create().set(Keys.BEAN_VALIDATION_ENABLED, true);
        ResultCreateDto resultCreateDto =
                Instancio.of(ResultCreateDto.class).withSettings(settings).create();

        // setup the template to be used
        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setKey("Date of Hearing");
        substitution.setValue("My Substituted Value");

        resultCreateDto.setWordingFields(List.of(substitution));

        // setup the validation success
        ApplicationList applicationList = Mockito.mock(ApplicationList.class);
        ApplicationListEntry applicationListEntry = Mockito.mock(ApplicationListEntry.class);
        ResolutionCode resolutionCode = Mockito.mock(ResolutionCode.class);

        ListEntryResultCreateValidationSuccess success =
                ListEntryResultCreateValidationSuccess.builder()
                        .applicationList(applicationList)
                        .applicationListEntry(applicationListEntry)
                        .resolutionCode(resolutionCode)
                        .wordingSentence(
                                WordingTemplateSentence.with(
                                        "This is a template {TEXT|Date of Hearing|20}"))
                        .build();
        creationValidator.setSuccess(success);

        AppListEntryResolution entryToSave = new AppListEntryResolution();
        entryToSave.setId(23232L);
        entryToSave.setVersion(2L);

        when(applicationListEntryResultEntityMapper.toApplicationListEntryResult(
                        resultCreateDto,
                        "This is a template {My Substituted Value}",
                        resolutionCode,
                        applicationListEntry,
                        "myemail@domain.com"))
                .thenReturn(entryToSave);

        PayloadForCreateEntryResult<ResultCreateDto> payload =
                new PayloadForCreateEntryResult(
                        UUID.randomUUID(), UUID.randomUUID(), resultCreateDto);

        when(appListEntryResolutionRepository.save(entryToSave)).thenReturn(entryToSave);

        // setup the response of the call
        ResultGetDto resultGetDto =
                Instancio.of(ResultGetDto.class).withSettings(settings).create();
        when(applicationListEntryResultMapper.toResultGetDto(entryToSave)).thenReturn(resultGetDto);

        // make the call
        MatchResponse<ResultGetDto> matchResponse = service.create(payload);

        // assert the call
        Assertions.assertNotNull(matchResponse);
        Assertions.assertNotNull(matchResponse.getEtag());
        Assertions.assertEquals(resultGetDto, matchResponse.getPayload());
    }

    @Test
    void delete_validArgs_deletesEntryResult() {
        AppListEntryResolution appListEntryResolution = new AppListEntryResolution();
        appListEntryResolution.setId(1L);
        appListEntryResolution.setVersion(1L);

        var code = mock(ResolutionCode.class);
        var applicationList = mock(ApplicationList.class);
        var applicationListEntry = mock(ApplicationListEntry.class);

        ListEntryResultDeleteValidationSuccess success =
                new ListEntryResultDeleteValidationSuccess(
                        with(""),
                        code,
                        applicationList,
                        applicationListEntry,
                        appListEntryResolution);

        deletionValidator.setSuccess(success);

        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        ListEntryResultDeleteArgs args = new ListEntryResultDeleteArgs(listId, entryId, resultId);

        service.delete(args);

        verify(deletionValidator).validate(any(ListEntryResultDeleteArgs.class), notNull());
        verify(appListEntryResolutionRepository).delete(any(AppListEntryResolution.class));
    }

    @Setter
    static class DummyApplicationEntryResultDeletionValidator
            extends ApplicationEntryResultDeletionValidator {
        private ListEntryResultDeleteValidationSuccess success;

        public DummyApplicationEntryResultDeletionValidator(
                ApplicationListRepository applicationListRepository,
                ApplicationListEntryRepository applicationListEntryRepository,
                ResolutionCodeRepository resolutionCodeRepository,
                AppListEntryResolutionRepository appListEntryResolutionRepository) {
            super(
                    applicationListRepository,
                    applicationListEntryRepository,
                    resolutionCodeRepository,
                    appListEntryResolutionRepository);
        }

        @Override
        public <R> R validate(
                ListEntryResultDeleteArgs args,
                BiFunction<ListEntryResultDeleteArgs, ListEntryResultDeleteValidationSuccess, R>
                        deleteSupplier) {

            return deleteSupplier.apply(args, success);
        }
    }

    @Setter
    static class DummyApplicationEntryResultCreationValidator
            extends ApplicationEntryResultCreationValidator {

        private ListEntryResultCreateValidationSuccess success;

        public DummyApplicationEntryResultCreationValidator(
                ApplicationListRepository applicationListRepository,
                ApplicationListEntryRepository applicationListEntryRepository,
                ResolutionCodeRepository resolutionCodeRepository) {

            super(
                    applicationListRepository,
                    applicationListEntryRepository,
                    resolutionCodeRepository);
        }

        @Override
        public <R> R validate(
                PayloadForCreateEntryResult<ResultCreateDto> validatable,
                BiFunction<
                                PayloadForCreateEntryResult<ResultCreateDto>,
                                ListEntryResultCreateValidationSuccess,
                                R>
                        validateSuccess) {

            return validateSuccess.apply(validatable, success);
        }
    }

    @Setter
    static class DummyApplicationEntryResultUpdateValidator
            extends ApplicationEntryResultUpdateValidator {

        private ListEntryResultUpdateValidationSuccess success;

        public DummyApplicationEntryResultUpdateValidator(
                ApplicationListRepository applicationListRepository,
                ApplicationListEntryRepository applicationListEntryRepository,
                ResolutionCodeRepository resolutionCodeRepository,
                AppListEntryResolutionRepository appListEntryResolutionRepository) {
            super(
                    applicationListRepository,
                    applicationListEntryRepository,
                    resolutionCodeRepository,
                    appListEntryResolutionRepository);
        }

        @Override
        public <R> R validate(
                PayloadForUpdateEntryResult validatable,
                BiFunction<PayloadForUpdateEntryResult, ListEntryResultUpdateValidationSuccess, R>
                        validateSuccess) {
            return validateSuccess.apply(validatable, success);
        }
    }

    static class DummyAuditOperationService implements AuditOperationService {

        @Override
        public <T, E extends Keyable> T processAudit(
                AuditOperation auditType,
                Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution) {

            return processAudit(
                    null, auditType, execution, (AuditOperationLifecycleListener[]) null);
        }

        @Override
        public <T, E extends Keyable> T processAudit(
                E oldValue,
                AuditOperation auditType,
                Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution) {

            return processAudit(
                    oldValue, auditType, execution, (AuditOperationLifecycleListener[]) null);
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

            StartEvent event = new StartEvent(auditType, "test-trace-id", oldValue);
            Optional<AuditableResult<T, E>> result = execution.apply(event);
            return result.map(AuditableResult::getResultingValue).orElse(null);
        }
    }
}
