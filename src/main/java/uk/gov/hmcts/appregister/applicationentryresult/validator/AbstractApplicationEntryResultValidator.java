package uk.gov.hmcts.appregister.applicationentryresult.validator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.common.validator.Validator;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractApplicationEntryResultValidator<T, O> implements Validator<T, O> {
    private final ApplicationListRepository applicationListRepository;
    private final ApplicationListEntryRepository applicationListEntryRepository;
    private final ResolutionCodeRepository resolutionCodeRepository;

    public void validate(T validatable) {
        validate(validatable, null);
    }

    /**
     * This validator has many rules. The rules are as such:- - The application list must exist and
     * be in the open state - The application list entry must exist and be in the open state - The
     * result code must exist
     *
     * @param validatable The validatable payload
     * @param validateSuccess The success function to call if validation is successful
     */
    public <R> R validate(T validatable, BiFunction<T, O, R> validateSuccess) {

        ApplicationList applicationList = validateParentApplicationList(validatable);

        ApplicationListEntry applicationListEntry = validateParentApplicationListEntry(validatable);

        // validate that the result code exists
        ResolutionCode code = validateResultCode(validatable);

        // parse the wording template and error if not valid
        WordingTemplateSentence wordingTemplateCollection =
                WordingTemplateSentence.with(code.getWording());

        if (validateSuccess != null) {
            return validateSuccess.apply(
                    validatable,
                    getResult(
                            code,
                            wordingTemplateCollection,
                            applicationList,
                            applicationListEntry,
                            validatable));
        }
        return null;
    }

    /**
     * gets the result of the validation.
     *
     * @param code The result code
     * @param wordingTemplateCollection The wording template collection
     * @param applicationListEntry The application list entry
     */
    protected abstract O getResult(
            ResolutionCode code,
            WordingTemplateSentence wordingTemplateCollection,
            ApplicationList applicationList,
            ApplicationListEntry applicationListEntry,
            T dto);

    /**
     * validate the application list for the app list entry result update. Validates that the
     * application list exists
     *
     * @param validatable The validatable payload
     * @return The application list if found
     */
    private ApplicationList validateParentApplicationList(T validatable) {
        Optional<ApplicationList> applicationList =
                applicationListRepository.findByUuidIncludingDelete(
                        getApplicationListUuid(validatable));
        if (applicationList.isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListEntryResultError.APPLICATION_LIST_DOES_NOT_EXIST,
                    "The application list does not exist %s"
                            .formatted(getApplicationListUuid(validatable)));
        }

        // if the state of the application is not open then we cant add an entry
        if (!applicationList.get().isOpen() || applicationList.get().isDeleted()) {
            throw new AppRegistryException(
                    ApplicationListEntryResultError.APPLICATION_LIST_STATE_IS_INCORRECT,
                    "The application list id %s is not in the correct state or the application list is deleted %s"
                            .formatted(
                                    getApplicationListUuid(validatable),
                                    applicationList.get().getStatus()));
        }

        log.debug("Validated application list with id{}", getApplicationListUuid(validatable));

        return applicationList.get();
    }

    /**
     * validate the application list entry for the app list entry result update. Validates that the
     * application list entry exists
     *
     * @param validatable The validatable payload
     * @return The application list entry if found
     */
    private ApplicationListEntry validateParentApplicationListEntry(T validatable) {
        Optional<ApplicationListEntry> entry =
                applicationListEntryRepository.findActiveByUuidAndApplicationListUuid(
                        getApplicationListEntryUuid(validatable),
                        getApplicationListUuid(validatable));

        if (entry.isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListEntryResultError.APPLICATION_ENTRY_DOES_NOT_EXIST,
                    "No application list entry exists that belongs to the specified list %s"
                            .formatted(getApplicationListEntryUuid(validatable)));
        }

        log.debug(
                "Validated application list entry with id{}",
                getApplicationListEntryUuid(validatable));

        return entry.get();
    }

    /**
     * gets the result code.
     *
     * @param validatable The validatable payload
     * @return The result code
     */
    protected abstract String getResultCode(T validatable);

    /**
     * get app list.
     *
     * @param validatable The validatable payload
     * @return The app list id
     */
    protected abstract UUID getApplicationListUuid(T validatable);

    /**
     * get app list entry.
     *
     * @param validatable The validatable payload
     * @return The app list entry id
     */
    protected abstract UUID getApplicationListEntryUuid(T validatable);

    /**
     * validate the provided code against the payload details.
     *
     * @param validatable The dto to validate
     * @return The code if valid
     * @throws AppRegistryException In the event of a failure
     */
    private ResolutionCode validateResultCode(T validatable) {
        // validate that the result code exists
        List<ResolutionCode> list =
                resolutionCodeRepository.findPrioritisingNullEndDate(
                        getResultCode(validatable), PageRequest.of(0, 1));

        Optional<ResolutionCode> code = list.stream().findFirst();

        if (code.isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListEntryResultError.RESOLUTION_CODE_DOES_NOT_EXIST,
                    "No valid resolution code could be found %s".formatted(code));
        }

        log.debug("Validated the result code {}", getResultCode(validatable));
        return code.get();
    }
}
