package uk.gov.hmcts.appregister.applicationentry.validator;

import java.util.Optional;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadGetEntryInList;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.validator.Validator;

/**
 * Gets the application entry validator which checks that the application entry exists within the
 * specified application list and that the application list is in the correct state (open and not
 * deleted).
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class GetApplicationEntryValidator
        implements Validator<PayloadGetEntryInList, GetEntryValidationSuccess> {
    private final ApplicationListEntryRepository applicationListEntryRepository;

    private final ApplicationListRepository applicationListRepository;

    @Override
    public void validate(PayloadGetEntryInList validatable) {
        validate(validatable, (req, success) -> null);
    }

    @Override
    public <R> R validate(
            PayloadGetEntryInList validatable,
            BiFunction<PayloadGetEntryInList, GetEntryValidationSuccess, R> validateSuccess) {

        // check that the application list exists
        Optional<ApplicationList> applicationList =
                applicationListRepository.findByUuidIncludingDelete(validatable.getListId());
        if (applicationList.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST,
                    "The application list does not exist %s".formatted(validatable.getListId()));
        }

        // if the state of the application is  open and not soft deleted
        if (applicationList.get().getStatus() != Status.OPEN || applicationList.get().isDeleted()) {
            throw new AppRegistryException(
                    AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT,
                    "The application list id %s is not in the correct state or the application list is deleted"
                            .formatted(validatable.getListId()));
        }

        // check that the entry exists
        Optional<ApplicationListEntry> entry =
                applicationListEntryRepository.findByUuid(validatable.getEntryId());
        if (entry.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.ENTRY_DOES_NOT_EXIST,
                    "The application entry %s does not exist in application list %s"
                            .formatted(validatable.getEntryId(), validatable.getListId()));
        }

        log.debug(" application list entry is found {}", validatable.getEntryId());

        // check that the entry is within the list
        entry =
                applicationListEntryRepository.findByEntryUuidWithinListUuid(
                        validatable.getListId(), validatable.getEntryId());
        if (entry.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.ENTRY_IS_NOT_WITHIN_LIST,
                    "The application list entry does not exist %s"
                            .formatted(validatable.getEntryId()));
        }

        GetEntryValidationSuccess success =
                new GetEntryValidationSuccess(entry.get(), applicationList.get());

        return validateSuccess.apply(validatable, success);
    }
}
