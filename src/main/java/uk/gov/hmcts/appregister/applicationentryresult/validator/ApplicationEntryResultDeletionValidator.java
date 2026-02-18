package uk.gov.hmcts.appregister.applicationentryresult.validator;

import java.util.UUID;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.applicationentryresult.model.ListEntryResultDeleteArgs;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;

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

    public ApplicationEntryResultDeletionValidator(
            ApplicationListRepository applicationListRepository,
            ApplicationListEntryRepository applicationListEntryRepository,
            ResolutionCodeRepository resolutionCodeRepository,
            AppListEntryResolutionRepository appListEntryResultRepository) {

        super(applicationListRepository, applicationListEntryRepository, resolutionCodeRepository);
        this.appListEntryResultRepository = appListEntryResultRepository;
    }

    @Override
    public void validate(ListEntryResultDeleteArgs args) {
        validate(args, (a, s) -> null);
        log.debug("Validated deletion for entry result {}", args.resultId());
    }

    @Override
    public <R> R validate(
            ListEntryResultDeleteArgs args,
            BiFunction<ListEntryResultDeleteArgs, ListEntryResultDeleteValidationSuccess, R>
                    createSupplier) {

        return super.validate(args, createSupplier);
    }

    @Override
    protected ListEntryResultDeleteValidationSuccess getResult(
            ResolutionCode code,
            WordingTemplateSentence wordingSentence,
            ApplicationList applicationList,
            ApplicationListEntry applicationListEntry,
            ListEntryResultDeleteArgs dto) {

        AppListEntryResolution appListEntryResult =
                appListEntryResultRepository
                        .findByUuidAndApplicationList_Uuid(dto.resultId(), dto.entryId())
                        .orElseThrow(
                                () ->
                                        new AppRegistryException(
                                                ApplicationListEntryResultError
                                                        .LIST_ENTRY_RESULT_NOT_FOUND,
                                                ("No application list entry result was found for UUID '%s' that"
                                                                + " belongs to the specified entry")
                                                        .formatted(dto.resultId())));

        return new ListEntryResultDeleteValidationSuccess(
                wordingSentence, code, applicationList, applicationListEntry, appListEntryResult);
    }

    @Override
    protected String getResultCode(ListEntryResultDeleteArgs validatable) {
        return null;
    }

    @Override
    protected UUID getApplicationListUuid(ListEntryResultDeleteArgs validatable) {
        return validatable.listId();
    }

    @Override
    protected UUID getApplicationListEntryUuid(ListEntryResultDeleteArgs validatable) {
        return validatable.entryId();
    }
}
