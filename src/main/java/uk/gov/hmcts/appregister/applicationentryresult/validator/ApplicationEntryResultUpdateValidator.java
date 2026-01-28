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
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;

/**
 * Validates the dto for an application entry result update.
 */
@Component
@Slf4j
public class ApplicationEntryResultUpdateValidator
        extends AbstractApplicationEntryResultValidator<
                PayloadForUpdateEntryResult, ListEntryResultUpdateValidationSuccess> {
    private final AppListEntryResolutionRepository appListEntryResolutionRepository;

    public ApplicationEntryResultUpdateValidator(
            ApplicationListRepository applicationListRepository,
            ApplicationListEntryRepository applicationListEntryRepository,
            ResolutionCodeRepository resolutionCodeRepository,
            AppListEntryResolutionRepository appListEntryResolutionRepository) {
        super(applicationListRepository, applicationListEntryRepository, resolutionCodeRepository);
        this.appListEntryResolutionRepository = appListEntryResolutionRepository;
    }

    @Override
    public void validate(PayloadForUpdateEntryResult validatable) {
        validate(validatable, null);
    }

    @Override
    public <R> R validate(
            PayloadForUpdateEntryResult validatable,
            BiFunction<PayloadForUpdateEntryResult, ListEntryResultUpdateValidationSuccess, R>
                    validateSuccess) {

        // Run base validations first
        R result = super.validate(validatable, validateSuccess);

        // Then check that the entry result exists
        Optional<AppListEntryResolution> entryResult =
                appListEntryResolutionRepository.findByUuidAndApplicationList_Uuid(
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

        return result;
    }

    @Override
    protected ListEntryResultUpdateValidationSuccess getResult(
            ResolutionCode code,
            WordingTemplateSentence wordingTemplateCollection,
            ApplicationList applicationList,
            ApplicationListEntry applicationListEntry,
            PayloadForUpdateEntryResult payload) {
        return new ListEntryResultUpdateValidationSuccess(
                wordingTemplateCollection,
                code,
                applicationList,
                applicationListEntry,
                appListEntryResolutionRepository
                        .findByUuidAndApplicationList_Uuid(
                                payload.getResultId(), payload.getEntryId())
                        .get());
    }

    @Override
    protected String getResultCode(PayloadForUpdateEntryResult validatable) {
        return validatable.getData().getResultCode();
    }

    @Override
    protected UUID getApplicationListUuid(PayloadForUpdateEntryResult validatable) {
        return validatable.getId();
    }

    protected UUID getApplicationListEntryUuid(PayloadForUpdateEntryResult validatable) {
        return validatable.getEntryId();
    }
}
