package uk.gov.hmcts.appregister.applicationentryresult.service;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.applicationentryresult.audit.AppListEntryResultAuditOperation;
import uk.gov.hmcts.appregister.applicationentryresult.mapper.ApplicationListEntryResultEntityMapper;
import uk.gov.hmcts.appregister.applicationentryresult.mapper.ApplicationListEntryResultMapper;
import uk.gov.hmcts.appregister.applicationentryresult.model.ListEntryResultDeleteArgs;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForCreateEntryResult;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForUpdateEntryResult;
import uk.gov.hmcts.appregister.applicationentryresult.validator.ApplicationEntryResultCreationValidator;
import uk.gov.hmcts.appregister.applicationentryresult.validator.ApplicationEntryResultDeletionValidator;
import uk.gov.hmcts.appregister.applicationentryresult.validator.ApplicationEntryResultUpdateValidator;
import uk.gov.hmcts.appregister.common.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.common.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.common.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.concurrency.MatchService;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.common.util.BeanUtil;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;
import uk.gov.hmcts.appregister.generated.model.ResultGetDto;

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
    private final ApplicationEntryResultCreationValidator creationValidator;
    private final ApplicationEntryResultUpdateValidator updateValidator;

    // Services
    private final MatchService matchService;

    // Audit
    private final AuditOperationService auditService;
    private final List<AuditOperationLifecycleListener> auditLifecycleListeners;

    // Mappers
    private final ApplicationListEntryResultMapper applicationListEntryResultMapper;
    private final ApplicationListEntryResultEntityMapper applicationListEntryResultEntityMapper;

    // Infrastructure
    private final EntityManager entityManager;
    private final UserProvider userProvider;

    @Override
    @Transactional
    public void delete(ListEntryResultDeleteArgs args) {
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
    }

    @Override
    @Transactional
    public MatchResponse<ResultGetDto> create(
            PayloadForCreateEntryResult<ResultCreateDto> resultCreateDto) {

        MatchResponse<ResultGetDto> getDto =
                creationValidator.validate(
                        resultCreateDto,
                        (payload, success) ->
                                auditService.processAudit(
                                        AppListEntryResultAuditOperation
                                                .CREATE_APP_LIST_ENTRY_RESULT,
                                        req -> {

                                            // save the entry result
                                            AppListEntryResolution listEntryResultEntity =
                                                    applicationListEntryResultEntityMapper
                                                            .toApplicationListEntryResult(
                                                                    payload.getData(),
                                                                    success.getWordingSentence()
                                                                            .substitute(
                                                                                    payload.getData()
                                                                                            .getWordingFields())
                                                                            .getSubstitutedString(),
                                                                    success.getResolutionCode(),
                                                                    success
                                                                            .getApplicationListEntry(),
                                                                    userProvider.getEmail());

                                            listEntryResultEntity.setResolutionOfficer(
                                                    userProvider.getEmail());

                                            listEntryResultEntity =
                                                    refreshEntity(
                                                            repository.save(listEntryResultEntity));
                                            log.debug(
                                                    "Created application list entry result with id: {}",
                                                    listEntryResultEntity.getId());

                                            ResultGetDto resultGetDto =
                                                    applicationListEntryResultMapper.toResultGetDto(
                                                            listEntryResultEntity);

                                            return Optional.of(
                                                    new AuditableResult<>(
                                                            MatchResponse.of(
                                                                    resultGetDto,
                                                                    getKeyablesForCreateUpdateEtag(
                                                                            listEntryResultEntity)),
                                                            listEntryResultEntity));
                                        }));

        return getDto;
    }

    @Override
    @Transactional
    public MatchResponse<ResultGetDto> update(PayloadForUpdateEntryResult updateEntryResult) {
        log.debug("Started: Update Application Entry Result: {}", updateEntryResult);
        log.debug(
                "Updating application entry result with id: {} in entry {} in list {}",
                updateEntryResult.getResultId(),
                updateEntryResult.getEntryId(),
                updateEntryResult.getId());

        // updates the entity and return the etag for matching
        MatchResponse<ResultGetDto> getDto =
                updateValidator.validate(
                        updateEntryResult,
                        (dto, success) -> {
                            // lets check the concurrent match before we process the update
                            return matchService.matchOnRequest(
                                    () -> {
                                        return auditService.processAudit(
                                                BeanUtil.copyBean(success.getAppListEntryResult()),
                                                AppListEntryResultAuditOperation
                                                        .UPDATE_APP_LIST_ENTRY_RESULT,
                                                req -> {

                                                    // save the list entry result
                                                    AppListEntryResolution listEntryResultEntity =
                                                            success.getAppListEntryResult();

                                                    // update the core list data
                                                    applicationListEntryResultEntityMapper
                                                            .toApplicationListEntryResult(
                                                                    updateEntryResult.getData(),
                                                                    success.getWordingSentence()
                                                                            .substitute(
                                                                                    updateEntryResult
                                                                                            .getData()
                                                                                            .getWordingFields())
                                                                            .getSubstitutedString(),
                                                                    success.getResolutionCode(),
                                                                    success
                                                                            .getApplicationListEntry(),
                                                                    userProvider.getEmail(),
                                                                    listEntryResultEntity);

                                                    // save the core list data
                                                    listEntryResultEntity =
                                                            refreshEntity(
                                                                    repository.save(
                                                                            listEntryResultEntity));
                                                    log.debug(
                                                            "Updated application entry result with id: {}",
                                                            listEntryResultEntity.getId());

                                                    ResultGetDto resultGetDto =
                                                            applicationListEntryResultMapper
                                                                    .toResultGetDto(
                                                                            listEntryResultEntity);

                                                    return Optional.of(
                                                            new AuditableResult<>(
                                                                    MatchResponse.of(
                                                                            resultGetDto,
                                                                            getKeyablesForCreateUpdateEtag(
                                                                                    listEntryResultEntity)),
                                                                    success
                                                                            .getAppListEntryResult()));
                                                });
                                    },

                                    // return the latest entities for the entry result read on the
                                    // update
                                    getKeyablesForCreateUpdateEtag(
                                            success.getAppListEntryResult()));
                        });

        log.debug("Finish: Update Application Entry Result: {}", updateEntryResult);

        return getDto;
    }

    /**
     * Reloads the entity so DB-generated fields (e.g. UUID via gen_random_uuid()) are available
     * immediately after save. Calls: - flush(): force the INSERT - refresh(): reselect the row with
     * DB defaults/triggers
     */
    private AppListEntryResolution refreshEntity(AppListEntryResolution entity) {
        entityManager.flush();
        entityManager.refresh(entity);
        return entity;
    }

    /**
     * gets the keyable for the create/update entry result.
     *
     * @param updateEntryResult The entry result that was created or is being updated
     * @return The list of keyables that constitute an etag
     */
    private List<Keyable> getKeyablesForCreateUpdateEtag(AppListEntryResolution updateEntryResult) {
        // create the update etag based on the following details
        List<Keyable> keyables = new ArrayList<>();
        keyables.add(updateEntryResult);
        return keyables;
    }
}
