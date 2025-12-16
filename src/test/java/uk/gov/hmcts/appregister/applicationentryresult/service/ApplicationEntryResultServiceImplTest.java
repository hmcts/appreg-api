package uk.gov.hmcts.appregister.applicationentryresult.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.applicationentryresult.model.ListEntryResultDeleteArgs;
import uk.gov.hmcts.appregister.applicationentryresult.validator.ApplicationEntryResultDeletionValidator;
import uk.gov.hmcts.appregister.applicationentryresult.validator.ListEntryResultDeleteValidationSuccess;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;

@ExtendWith(MockitoExtension.class)
class ApplicationEntryResultServiceImplTest {

    @Mock private ApplicationListRepository applicationListRepository;
    @Mock private ApplicationListEntryRepository applicationListEntryRepository;
    @Mock private AppListEntryResolutionRepository appListEntryResolutionRepository;
    @Mock private uk.gov.hmcts.appregister.common.concurrency.MatchService matchService;
    @Mock private AuditOperationService auditOperationService;
    @Mock private AuditOperationLifecycleListener auditOperationLifecycleListener;

    @Spy
    private DummyApplicationEntryResultDeletionValidator deletionValidator =
            new DummyApplicationEntryResultDeletionValidator(
                    applicationListRepository,
                    applicationListEntryRepository,
                    appListEntryResolutionRepository);

    private ApplicationEntryResultServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new ApplicationEntryResultServiceImpl(
                        appListEntryResolutionRepository,
                        deletionValidator,
                        matchService,
                        auditOperationService,
                        List.of(auditOperationLifecycleListener));
    }

    @Test
    void delete_validArgs_deletesEntryResult() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();

        AppListEntryResolution appListEntryResolution = new AppListEntryResolution();

        ListEntryResultDeleteValidationSuccess success =
                new ListEntryResultDeleteValidationSuccess();
        success.setAppListEntryResult(appListEntryResolution);

        deletionValidator.setSuccess(success);

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
                AppListEntryResolutionRepository appListEntryResolutionRepository) {
            super(
                    applicationListRepository,
                    applicationListEntryRepository,
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
}
