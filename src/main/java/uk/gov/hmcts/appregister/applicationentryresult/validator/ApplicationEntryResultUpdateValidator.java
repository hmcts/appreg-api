package uk.gov.hmcts.appregister.applicationentryresult.validator;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForUpdateEntryResult;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;

/**
 * Validates the dto for an application entry result update.
 */
@Component
@Slf4j
public class ApplicationEntryResultUpdateValidator
        extends AbstractApplicationEntryResultValidator<
                PayloadForUpdateEntryResult, ListEntryResultUpdateValidationSuccess> {

    private final AppListEntryResolutionRepository appListEntryResultRepository;

    public ApplicationEntryResultUpdateValidator(
            ApplicationListRepository applicationListRepository,
            ApplicationListEntryRepository applicationListEntryRepository,
            ResolutionCodeRepository resolutionCodeRepository,
            BusinessDateProvider businessDateProvider,
            AppListEntryResolutionRepository appListEntryResultRepository) {
        super(
                applicationListRepository,
                applicationListEntryRepository,
                resolutionCodeRepository,
                businessDateProvider);
        this.appListEntryResultRepository = appListEntryResultRepository;
    }

    @Override
    public void validate(PayloadForUpdateEntryResult validatable) {
        super.validate(validatable, null);

        Optional<AppListEntryResolution> entryResult =
                appListEntryResultRepository.findByUuidAndApplicationList_Uuid(
                        validatable.getResultId(), validatable.getEntryId());

        if (entryResult.isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListEntryResultError.APPLICATION_ENTRY_RESULT_DOES_NOT_EXIST,
                    ("The application entry result %s does not exist in application list %s and in application list "
                                    + "entry %s")
                            .formatted(
                                    validatable.getResultId(),
                                    getApplicationListUuid(validatable),
                                    validatable.getEntryId()));
        }

        log.debug("application list entry result is found {}", validatable.getResultId());
    }

    @Override
    public <R> R validate(
            PayloadForUpdateEntryResult validatable,
            BiFunction<PayloadForUpdateEntryResult, ListEntryResultUpdateValidationSuccess, R>
                    validateSuccess) {
        // Delegate to base validate which will call getResult(...) to build the success object.
        return super.validate(validatable, validateSuccess);
    }

    @Override
    protected ListEntryResultUpdateValidationSuccess getResult(
            ResolutionCode code,
            WordingTemplateSentence wordingTemplateCollection,
            ApplicationList applicationList,
            ApplicationListEntry applicationListEntry,
            PayloadForUpdateEntryResult payload) {

        AppListEntryResolution appListEntryResult =
                appListEntryResultRepository
                        .findByUuidAndApplicationList_Uuid(
                                payload.getResultId(), payload.getEntryId())
                        .orElseThrow(
                                () ->
                                        new AppRegistryException(
                                                ApplicationListEntryResultError
                                                        .LIST_ENTRY_RESULT_NOT_FOUND,
                                                ("No application list entry result was found for UUID '%s' that"
                                                                + " belongs to the specified entry")
                                                        .formatted(payload.getResultId())));
        return new ListEntryResultUpdateValidationSuccess(
                wordingTemplateCollection,
                code,
                applicationList,
                applicationListEntry,
                appListEntryResult);
    }

    @Override
    protected String getResultCode(PayloadForUpdateEntryResult validatable) {
        return validatable.getData().getResultCode();
    }

    @Override
    protected UUID getApplicationListUuid(PayloadForUpdateEntryResult validatable) {
        return validatable.getId();
    }

    @Override
    protected UUID getApplicationListEntryUuid(PayloadForUpdateEntryResult validatable) {
        return validatable.getEntryId();
    }
}
