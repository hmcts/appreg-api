package uk.gov.hmcts.appregister.applicationentryresult.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.applicationentryresult.audit.AppListEntryResultAuditOperation;
import uk.gov.hmcts.appregister.applicationentryresult.model.ListEntryResultDeleteArgs;
import uk.gov.hmcts.appregister.applicationentryresult.validator.ApplicationEntryResultDeletionValidator;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.concurrency.MatchService;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.util.BeanUtil;

/**
 * Service implementation for managing application list entry results.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ApplicationEntryResultServiceImpl implements ApplicationEntryResultService {
    // Repositories
    private final AppListEntryResolutionRepository repository;

    // Validators
    private final ApplicationEntryResultDeletionValidator deletionValidator;

    // Services
    private final MatchService matchService;

    // Audit
    private final AuditOperationService auditService;
    private final List<AuditOperationLifecycleListener> auditLifecycleListeners;

    @Override
    @Transactional
    public void delete(ListEntryResultDeleteArgs args) {
        log.debug("Start: Deleting Application List Entry Result with id: {}", args.resultId());

        deletionValidator.validate(
                args,
                (id, success) -> {
                    var entity = success.getAppListEntryResult();

                    // Perform an etag match as part of the request. If the client's etag doesn't
                    // match
                    // the current server state, matchService.matchOnRequest(...) should fail/throw
                    // and
                    // the delete will not be applied.
                    matchService.matchOnRequest(
                            () -> {
                                // actual delete happens here (inside supplier)
                                repository.delete(entity);
                                // return a MatchResponse indicating there is no 'new' object
                                // (deleted)
                                // but include the pre-change entity for etag calculation/matching.
                                return MatchResponse.of(null, List.of(entity));
                            },
                            // list of resources to be used for ETag calculation / checking
                            List.of(entity));

                    // Only audit after matchService returned successfully (i.e. match succeeded)
                    auditService.processAudit(
                            BeanUtil.copyBean(entity),
                            AppListEntryResultAuditOperation.DELETE_APP_LIST_ENTRY_RESULT,
                            ev -> Optional.empty(),
                            auditLifecycleListeners.toArray(
                                    new AuditOperationLifecycleListener[0]));

                    return null;
                });

        log.debug("Finish: Deleted Application List Entry Result with id: {}", args.resultId());
    }
}
