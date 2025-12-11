package uk.gov.hmcts.appregister.applicationentry.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadGetEntryInList;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.validator.Validator;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * gets the entry detail validator
 */
@Slf4j
@RequiredArgsConstructor
public class GetEntryValidator implements Validator<PayloadGetEntryInList, GetEntryValidationSuccess> {
    private final ApplicationListEntryRepository applicationListEntryRepository;

    private final ApplicationListRepository applicationListRepository;

    @Override
    public void validate(PayloadGetEntryInList validatable) {
        validate(validatable,(req, success) -> null);
    }

    @Override
    public <R> R validate(PayloadGetEntryInList validatable, BiFunction<PayloadGetEntryInList, GetEntryValidationSuccess, R> validateSuccess) {
        Optional<ApplicationList> applicationList =
            applicationListRepository.findByUuidIncludingDelete(validatable.getId());
        if (applicationList.isEmpty()) {
            throw new AppRegistryException(
                AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST,
                "The application list does not exist %s"
                    .formatted(validatable.getId()));
        }

        // if the state of the application is not open then we cant add an entry
        if (applicationList.get().getStatus() != Status.OPEN || applicationList.get().isDeleted()) {
            throw new AppRegistryException(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT_FOR_CREATE,
                "The application list id %s is not in the correct state or the application list is deleted %s"
                    .formatted(validatable.getId()));
        }

        Optional<ApplicationListEntry> entry =
            applicationListEntryRepository.findByUuid(validatable.getEntryId());
        if (entry.isEmpty()) {
            throw new AppRegistryException(
                AppListEntryError.ENTRY_DOES_NOT_EXIST,
                "The application entry %s does not exist in application list %s"
                    .formatted(
                        validatable.getEntryId(), validatable.getId()));
        }

        log.debug(" application list entry is found {}", validatable.getEntryId());

        entry =
            applicationListEntryRepository.findByEntryUuidWithinListUuid(
                validatable.getId(), validatable.getEntryId());
        if (entry.isEmpty()) {
            throw new AppRegistryException(
                AppListEntryError.ENTRY_IS_NOT_WITHIN_LIST,
                "The application list entry does not exist %s"
                    .formatted(validatable.getEntryId()));
        }

        GetEntryValidationSuccess success = new GetEntryValidationSuccess(entry.get(), applicationList.get());

        return validateSuccess.apply(validatable, success);
    }
}
