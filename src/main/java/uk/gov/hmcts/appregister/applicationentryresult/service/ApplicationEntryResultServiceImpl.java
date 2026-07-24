package uk.gov.hmcts.appregister.applicationentryresult.service;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.applicationentryresult.audit.AppListEntryResultAuditOperation;
import uk.gov.hmcts.appregister.applicationentryresult.audit.ApplicationListEntryResultAudit;
import uk.gov.hmcts.appregister.applicationentryresult.audit.BulkCreateApplicationEntryResultAudit;
import uk.gov.hmcts.appregister.applicationentryresult.audit.BulkDeleteApplicationEntryResultAudit;
import uk.gov.hmcts.appregister.applicationentryresult.mapper.ApplicationListEntryResultEntityMapper;
import uk.gov.hmcts.appregister.applicationentryresult.mapper.ApplicationListEntryResultMapper;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForCreateEntryResult;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForCreateResults;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForUpdateEntryResult;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadGetEntryResultInList;
import uk.gov.hmcts.appregister.applicationentryresult.validator.ApplicationEntryResultCreationValidator;
import uk.gov.hmcts.appregister.applicationentryresult.validator.ApplicationEntryResultGetValidator;
import uk.gov.hmcts.appregister.applicationentryresult.validator.ApplicationEntryResultUpdateValidator;
import uk.gov.hmcts.appregister.applicationentryresult.validator.BulkApplicationEntryResultCreationValidator;
import uk.gov.hmcts.appregister.applicationentryresult.validator.BulkApplicationEntryResultDeletionValidator;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.concurrency.MatchService;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryResultWithResultCodeProjection;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.common.util.BeanUtil;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.model.BulkDeleteResultsDto;
import uk.gov.hmcts.appregister.generated.model.BulkResultDto;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;
import uk.gov.hmcts.appregister.generated.model.ResultGetDto;
import uk.gov.hmcts.appregister.generated.model.ResultPage;

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
    private final ApplicationEntryResultCreationValidator creationValidator;
    private final ApplicationEntryResultUpdateValidator updateValidator;
    private final ApplicationEntryResultGetValidator applicationListGetValidator;
    private final BulkApplicationEntryResultDeletionValidator
            bulkApplicationEntryResultDeletionValidator;
    private final BulkApplicationEntryResultCreationValidator
            bulkApplicationEntryResultCreationValidator;

    // Services
    private final MatchService matchService;

    // Audit
    private final AuditOperationService auditService;

    // Mappers
    private final ApplicationListEntryResultMapper applicationListEntryResultMapper;
    private final ApplicationListEntryResultEntityMapper applicationListEntryResultEntityMapper;
    private final PageMapper pageMapper;

    // Infrastructure
    private final ObjectProvider<ApplicationEntryResultService> selfProvider;
    private final EntityManager entityManager;
    private final UserProvider userProvider;

    @Override
    @Transactional
    public void bulkDelete(BulkDeleteResultsDto bulkDeleteResultsDto) {
        bulkApplicationEntryResultDeletionValidator.validate(
                bulkDeleteResultsDto,
                (request, success) -> {
                    var deletedResults =
                            success.getResults().stream()
                                    .map(
                                            item ->
                                                    BeanUtil.copyBean(
                                                            item.validationSuccess()
                                                                    .getAppListEntryResult()))
                                    .toList();
                    var bulkAuditRequest =
                            new BulkDeleteApplicationEntryResultAudit(
                                    deletedResults.isEmpty()
                                            ? null
                                            : deletedResults.getFirst().getId(),
                                    success.getResults().stream()
                                            .map(item -> item.args().listId())
                                            .toList(),
                                    success.getResults().stream()
                                            .map(item -> item.args().entryId())
                                            .toList(),
                                    deletedResults.size(),
                                    BulkDeleteApplicationEntryResultAudit.formatRequestedResults(
                                            request.getResults()));
                    var bulkAuditResult =
                            new BulkDeleteApplicationEntryResultAudit(
                                    deletedResults.isEmpty()
                                            ? null
                                            : deletedResults.getFirst().getId(),
                                    success.getResults().stream()
                                            .map(item -> item.args().listId())
                                            .toList(),
                                    success.getResults().stream()
                                            .map(item -> item.args().entryId())
                                            .toList(),
                                    deletedResults.size(),
                                    BulkDeleteApplicationEntryResultAudit.formatDeletedResults(
                                            deletedResults));

                    auditService.processAudit(
                            bulkAuditRequest,
                            AppListEntryResultAuditOperation.BULK_DELETE_APP_LIST_ENTRY_RESULT,
                            ignored -> {
                                repository.deleteAllInBatch(
                                        success.getResults().stream()
                                                .map(
                                                        item ->
                                                                item.validationSuccess()
                                                                        .getAppListEntryResult())
                                                .toList());
                                entityManager.flush();
                                return Optional.of(new AuditableResult<>(null, bulkAuditResult));
                            });
                    return null;
                });
    }

    @Override
    @Transactional
    public MatchResponse<ResultGetDto> create(
            PayloadForCreateEntryResult<ResultCreateDto> resultCreateDto) {

        return creationValidator.validate(
                resultCreateDto,
                (payload, success) ->
                        auditService.processAudit(
                                AppListEntryResultAuditOperation.CREATE_APP_LIST_ENTRY_RESULT,
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
                                                            success.getApplicationListEntry(),
                                                            userProvider.getEmail());

                                    listEntryResultEntity.setResolutionOfficer(
                                            userProvider.getEmail());

                                    listEntryResultEntity =
                                            refreshEntity(repository.save(listEntryResultEntity));
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
                                                    ApplicationListEntryResultAudit.from(
                                                            listEntryResultEntity)));
                                }));
    }

    @Override
    @Transactional
    public MatchResponse<ResultGetDto> update(PayloadForUpdateEntryResult updateEntryResult) {
        log.debug(
                "Started update application entry result {} for entry {} in list {}",
                updateEntryResult.getResultId(),
                updateEntryResult.getEntryId(),
                updateEntryResult.getId());

        // updates the entity and return the etag for matching
        MatchResponse<ResultGetDto> getDto =
                updateValidator.validate(
                        updateEntryResult,
                        (dto, success) ->
                                // lets check the concurrent match before we process the update
                                matchService.matchOnRequest(
                                        () ->
                                                auditService.processAudit(
                                                        ApplicationListEntryResultAudit.from(
                                                                BeanUtil.copyBean(
                                                                        success
                                                                                .getAppListEntryResult())),
                                                        AppListEntryResultAuditOperation
                                                                .UPDATE_APP_LIST_ENTRY_RESULT,
                                                        req -> {

                                                            // save the list entry result
                                                            AppListEntryResolution
                                                                    listEntryResultEntity =
                                                                            success
                                                                                    .getAppListEntryResult();

                                                            // update the core list data
                                                            applicationListEntryResultEntityMapper
                                                                    .toApplicationListEntryResult(
                                                                            updateEntryResult
                                                                                    .getData(),
                                                                            success.getWordingSentence()
                                                                                    .substitute(
                                                                                            updateEntryResult
                                                                                                    .getData()
                                                                                                    .getWordingFields())
                                                                                    .getSubstitutedString(),
                                                                            success
                                                                                    .getResolutionCode(),
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
                                                            AppListEntryResolution
                                                                    auditedEntryResult =
                                                                            success
                                                                                    .getAppListEntryResult();
                                                            ApplicationListEntryResultAudit
                                                                    auditEntity =
                                                                            ApplicationListEntryResultAudit
                                                                                    .from(
                                                                                            auditedEntryResult);

                                                            return Optional.of(
                                                                    new AuditableResult<>(
                                                                            MatchResponse.of(
                                                                                    resultGetDto,
                                                                                    getKeyablesForCreateUpdateEtag(
                                                                                            listEntryResultEntity)),
                                                                            auditEntity));
                                                        }),

                                        // return the latest entities for the entry result read on
                                        // the
                                        // update
                                        getKeyablesForCreateUpdateEtag(
                                                success.getAppListEntryResult())));

        log.debug(
                "Finished update application entry result {} for entry {} in list {}",
                updateEntryResult.getResultId(),
                updateEntryResult.getEntryId(),
                updateEntryResult.getId());

        return getDto;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage search(
            PayloadGetEntryResultInList payloadGetEntryResultInList, PagingWrapper pageWrapper) {
        ResultPage resultPage = new ResultPage();

        return applicationListGetValidator.validate(
                payloadGetEntryResultInList,
                (pay, success) ->
                        auditService.processAudit(
                                null,
                                AppListEntryResultAuditOperation.GET_APP_LIST_ENTRY_RESULT,
                                req -> {

                                    // get the list entry result
                                    ApplicationList applicationList = success.getApplicationList();

                                    ApplicationListEntry applicationListEntry =
                                            success.getApplicationListEntry();

                                    // get the page data
                                    Page<ApplicationListEntryResultWithResultCodeProjection>
                                            pageData =
                                                    repository
                                                            .getResolutionDetailsForApplicationListAndEntry(
                                                                    applicationList.getUuid(),
                                                                    applicationListEntry.getUuid(),
                                                                    pageWrapper.getPageable());

                                    resultPage.setContent(new ArrayList<>());

                                    // convert data to response
                                    pageData.forEach(
                                            result ->
                                                    resultPage.addContentItem(
                                                            applicationListEntryResultMapper
                                                                    .toResultGetDto(result)));
                                    pageMapper.toPage(
                                            pageData, resultPage, pageWrapper.getSortStrings());

                                    // generate response for auditing
                                    AppListEntryResolution appListEntryResolution =
                                            new AppListEntryResolution();

                                    applicationListEntryResultEntityMapper
                                            .toApplicationListEntryResult(
                                                    payloadGetEntryResultInList,
                                                    appListEntryResolution);
                                    return Optional.of(
                                            new AuditableResult<>(
                                                    resultPage, appListEntryResolution));
                                }));
    }

    @Override
    @Transactional
    public List<ResultGetDto> bulkCreate(PayloadForCreateResults<BulkResultDto> bulkResultDto) {
        return bulkApplicationEntryResultCreationValidator.validate(
                bulkResultDto,
                (validate, success) -> {
                    var resultPayload = validate.getPayload().getResult();
                    var userEmail = userProvider.getEmail();
                    var entitiesToCreate =
                            new ArrayList<AppListEntryResolution>(success.getResults().size());

                    for (var validatedItem : success.getResults()) {
                        var payload = validatedItem.payload();
                        var createSuccess = validatedItem.validationSuccess();
                        var entity =
                                applicationListEntryResultEntityMapper.toApplicationListEntryResult(
                                        payload.getData(),
                                        createSuccess
                                                .getWordingSentence()
                                                .substitute(payload.getData().getWordingFields())
                                                .getSubstitutedString(),
                                        createSuccess.getResolutionCode(),
                                        createSuccess.getApplicationListEntry(),
                                        userEmail);
                        entity.setResolutionOfficer(userEmail);
                        entitiesToCreate.add(entity);
                    }

                    return auditService
                            .processAudit(
                                    AppListEntryResultAuditOperation
                                            .BULK_CREATE_APP_LIST_ENTRY_RESULT,
                                    ignored -> {
                                        var persistedEntities =
                                                new ArrayList<>(
                                                        repository.saveAll(entitiesToCreate));
                                        entityManager.flush();
                                        persistedEntities.sort(
                                                Comparator.comparing(
                                                        AppListEntryResolution::getId));

                                        var bulkAudit =
                                                new BulkCreateApplicationEntryResultAudit(
                                                        persistedEntities.isEmpty()
                                                                ? null
                                                                : persistedEntities
                                                                        .getFirst()
                                                                        .getId(),
                                                        validate.getListId(),
                                                        success.getResults().stream()
                                                                .map(
                                                                        item ->
                                                                                item.payload()
                                                                                        .getEntryId())
                                                                .toList(),
                                                        persistedEntities.size(),
                                                        resultPayload.getResultCode(),
                                                        BulkCreateApplicationEntryResultAudit
                                                                .formatWordingFields(
                                                                        resultPayload
                                                                                .getWordingFields()),
                                                        BulkCreateApplicationEntryResultAudit
                                                                .formatCreatedResults(
                                                                        persistedEntities));

                                        var resultDtos =
                                                persistedEntities.stream()
                                                        .map(
                                                                applicationListEntryResultMapper
                                                                        ::toResultGetDto)
                                                        .toList();

                                        return Optional.of(
                                                new AuditableResult<>(resultDtos, bulkAudit));
                                    })
                            .stream()
                            .toList();
                });
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
