package uk.gov.hmcts.appregister.applicationentryresult.validator;

import java.util.function.BiFunction;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.applicationentryresult.model.ListEntryResultDeleteArgs;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

/**
 * Validator responsible for ensuring that an application list, list entry, and entry result exist
 * and are in a valid state before a delete operation for an entry result is performed.
 */
@Component
@Slf4j
public class ApplicationEntryResultDeletionValidator
    extends AbstractApplicationEntryResultValidator<
    ListEntryResultDeleteArgs, ListEntryResultDeleteValidationSuccess> {

    private final AppListEntryResolutionRepository appListEntryResultRepository;

    public ApplicationEntryResultDeletionValidator(ApplicationListRepository applicationListRepository,
                                                   ApplicationListEntryRepository applicationListEntryRepository,
                                                   ResolutionCodeRepository resolutionCodeRepository,
                                                   AppListEntryResolutionRepository appListEntryResultRepository) {
        super(applicationListRepository, applicationListEntryRepository, resolutionCodeRepository);
        this.appListEntryResultRepository = appListEntryResultRepository;
    }

    @Override
    public void validate(ListEntryResultDeleteArgs args) {
        super.validate(args, null);

        AppListEntryResolution appListEntryResult =
            appListEntryResultRepository
                .findByUuidAndApplicationList_Uuid(args.resultId(), args.entryId())
                .orElseThrow(
                    () ->
                        new AppRegistryException(
                            ApplicationListEntryResultError.LIST_ENTRY_RESULT_NOT_FOUND,
                            ("No application list entry result was found for UUID '%s' that"
                                + " belongs to the specified entry")
                                .formatted(args.resultId())));

        ListEntryResultDeleteValidationSuccess success = new ListEntryResultDeleteValidationSuccess();
        success.setAppListEntryResult(appListEntryResult);

        log.debug("Validated deletion for entry result {}", args.resultId());
    }

    @Override
    public <R> R validate(
        ListEntryResultDeleteArgs args,
        BiFunction<ListEntryResultDeleteArgs, ListEntryResultDeleteValidationSuccess, R>
            createSupplier) {

        super.validate(args, null);

        // find the AppListEntryResolution for deletion
        AppListEntryResolution appListEntryResult =
            appListEntryResultRepository
                .findByUuidAndApplicationList_Uuid(args.resultId(), args.entryId())
                .orElseThrow(
                    () ->
                        new AppRegistryException(
                            ApplicationListEntryResultError.LIST_ENTRY_RESULT_NOT_FOUND,
                            ("No application list entry result was found for UUID '%s' that"
                                + " belongs to the specified entry")
                                .formatted(args.resultId())));

        ListEntryResultDeleteValidationSuccess success = new ListEntryResultDeleteValidationSuccess();
        success.setAppListEntryResult(appListEntryResult);

        return createSupplier.apply(args, success);
    }

    @Override
    protected ListEntryResultDeleteValidationSuccess getResult(
        uk.gov.hmcts.appregister.common.entity.ResolutionCode code,
        uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence wordingTemplateCollection,
        uk.gov.hmcts.appregister.common.entity.ApplicationList applicationList,
        uk.gov.hmcts.appregister.common.entity.ApplicationListEntry applicationListEntry,
        ListEntryResultDeleteArgs dto) {
        return null;
    }

    @Override
    protected String getResultCode(ListEntryResultDeleteArgs validatable) {
        return null;
    }

    @Override
    protected java.util.UUID getApplicationListUuid(ListEntryResultDeleteArgs validatable) {
        return validatable.listId();
    }

    @Override
    protected java.util.UUID getApplicationListEntryUuid(ListEntryResultDeleteArgs validatable) {
        return validatable.entryId();
    }
}
