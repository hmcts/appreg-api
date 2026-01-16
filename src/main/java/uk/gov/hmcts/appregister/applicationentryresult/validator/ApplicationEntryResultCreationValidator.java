package uk.gov.hmcts.appregister.applicationentryresult.validator;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForCreateEntryResult;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.common.validator.Validator;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;

/**
 * Validates the dto for an application entry result create.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApplicationEntryResultCreationValidator
        implements Validator<
                PayloadForCreateEntryResult<ResultCreateDto>,
                ListEntryResultCreateValidationSuccess> {
    private final ApplicationListRepository applicationListRepository;
    private final ApplicationListEntryRepository applicationListEntryRepository;
    private final ResolutionCodeRepository resolutionCodeRepository;

    @Override
    public void validate(PayloadForCreateEntryResult<ResultCreateDto> validatable) {
        validate(validatable, (v, r) -> null);
    }

    @Override
    public <R> R validate(
            PayloadForCreateEntryResult<ResultCreateDto> validatable,
            BiFunction<
                            PayloadForCreateEntryResult<ResultCreateDto>,
                            ListEntryResultCreateValidationSuccess,
                            R>
                    validateSuccess) {

        // 1) Parent list must exist + be OPEN + not deleted
        ApplicationList list = validateApplicationList(validatable.getListId());

        // 2) Entry must exist + belong to list + not deleted
        ApplicationListEntry entry = validateEntry(validatable.getEntryId(), list);

        // 3) Result code must exist
        ResolutionCode resolutionCode =
                validateResolutionCode(validatable.getData().getResultCode());

        // 4) Parse wording template (throws if malformed)
        WordingTemplateSentence wordingTemplate =
                WordingTemplateSentence.with(resolutionCode.getWording());

        ListEntryResultCreateValidationSuccess success =
                ListEntryResultCreateValidationSuccess.builder()
                        .applicationListEntry(entry)
                        .resolutionCode(resolutionCode)
                        .wordingSentence(wordingTemplate)
                        .build();

        return validateSuccess.apply(validatable, success);
    }

    private ApplicationList validateApplicationList(UUID listId) {
        Optional<ApplicationList> applicationList =
                applicationListRepository.findByUuidIncludingDelete(listId);

        if (applicationList.isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListEntryResultError.APPLICATION_LIST_DOES_NOT_EXIST,
                    "The application list does not exist %s".formatted(listId));
        }

        if (!applicationList.get().isOpen() || applicationList.get().isDeleted()) {
            throw new AppRegistryException(
                    ApplicationListEntryResultError.APPLICATION_LIST_STATE_IS_INCORRECT_FOR_CREATE,
                    "The application list id %s is not in the correct state or is deleted (status=%s)"
                            .formatted(listId, applicationList.get().getStatus()));
        }

        log.debug("Validated application list {}", listId);
        return applicationList.get();
    }

    private ApplicationListEntry validateEntry(UUID entryId, ApplicationList list) {
        Optional<ApplicationListEntry> entry =
                applicationListEntryRepository.findActiveByUuidAndApplicationListUuid(
                        entryId, list.getUuid());

        if (entry.isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListEntryResultError.APPLICATION_ENTRY_DOES_NOT_EXIST,
                    "No application list entry exists that belongs to the specified list %s"
                            .formatted(entryId));
        }

        log.debug(
                "Validated application list entry {} belongs to list {}", entryId, list.getUuid());
        return entry.get();
    }

    private ResolutionCode validateResolutionCode(String resolutionCode) {
        Optional<ResolutionCode> code =
                resolutionCodeRepository.findByResultCodeIgnoreCase(resolutionCode);

        if (code.isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListEntryResultError.RESOLUTION_CODE_DOES_NOT_EXIST,
                    "No valid resolution code could be found %s".formatted(resolutionCode));
        }

        log.debug("Validated resolution code {}", resolutionCode);
        return code.get();
    }
}
