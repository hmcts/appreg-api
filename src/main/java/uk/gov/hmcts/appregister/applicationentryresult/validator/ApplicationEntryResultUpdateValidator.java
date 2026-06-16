package uk.gov.hmcts.appregister.applicationentryresult.validator;

import java.util.Optional;
import java.util.UUID;
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
        validateEntryResult(validatable);
    }

    @Override
    protected ListEntryResultUpdateValidationSuccess getResult(
            ResolutionCode code,
            WordingTemplateSentence wordingTemplateCollection,
            ApplicationList applicationList,
            ApplicationListEntry applicationListEntry,
            PayloadForUpdateEntryResult payload) {
        AppListEntryResolution appListEntryResult = validateEntryResult(payload);
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

    private AppListEntryResolution validateEntryResult(PayloadForUpdateEntryResult validatable) {
        Optional<AppListEntryResolution> entryResult =
                appListEntryResultRepository.findByUuid(validatable.getResultId());

        if (entryResult.isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListEntryResultError.APPLICATION_ENTRY_RESULT_DOES_NOT_EXIST,
                    "The application entry result %s does not exist"
                            .formatted(validatable.getResultId()));
        }

        entryResult =
                appListEntryResultRepository.findByUuidAndApplicationList_Uuid(
                        validatable.getResultId(), validatable.getEntryId());
        if (entryResult.isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListEntryResultError.APPLICATION_ENTRY_RESULT_NOT_WITHIN_ENTRY,
                    ("The application entry result %s does not belong to application list %s and"
                                    + " application list entry %s")
                            .formatted(
                                    validatable.getResultId(),
                                    getApplicationListUuid(validatable),
                                    validatable.getEntryId()));
        }

        log.debug("application list entry result is found {}", validatable.getResultId());
        return entryResult.get();
    }
}
