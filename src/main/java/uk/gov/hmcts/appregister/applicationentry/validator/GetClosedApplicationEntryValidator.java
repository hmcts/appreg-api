package uk.gov.hmcts.appregister.applicationentry.validator;

import java.util.Optional;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadGetEntryInList;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.validator.Validator;

/**
 * Validates that an application list entry can be read from a closed application list.
 */
@Component
@RequiredArgsConstructor
public class GetClosedApplicationEntryValidator
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

        Optional<ApplicationList> applicationList =
                applicationListRepository.findByUuidIncludingDelete(validatable.getListId());
        if (applicationList.isEmpty() || applicationList.get().isDeleted()) {
            throw new AppRegistryException(
                    ApplicationListError.LIST_NOT_FOUND,
                    "The application list with id %s was not found"
                            .formatted(validatable.getListId()));
        }

        if (applicationList.get().getStatus() != Status.CLOSED) {
            throw new AppRegistryException(
                    AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT,
                    "The application list is not closed %s".formatted(validatable.getListId()));
        }

        Optional<ApplicationListEntry> entry =
                applicationListEntryRepository.findByUuid(validatable.getEntryId());
        if (entry.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.LIST_ENTRY_NOT_FOUND,
                    "The application entry %s does not exist".formatted(validatable.getEntryId()));
        }

        entry =
                applicationListEntryRepository.findByEntryUuidWithinListUuid(
                        validatable.getListId(), validatable.getEntryId());
        if (entry.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.ENTRY_IS_NOT_WITHIN_LIST,
                    "The application list entry does not exist %s"
                            .formatted(validatable.getEntryId()));
        }

        return validateSuccess.apply(
                validatable,
                GetEntryValidationSuccess.builder()
                        .applicationList(applicationList.get())
                        .applicationListEntry(entry.get())
                        .build());
    }
}
