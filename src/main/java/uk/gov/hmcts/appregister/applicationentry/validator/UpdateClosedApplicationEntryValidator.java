package uk.gov.hmcts.appregister.applicationentry.validator;

import java.util.Optional;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForUpdateClosedEntry;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.validator.Validator;

/**
 * validates that the application entry belongs to the list and the list is closed.
 */
@Component
@RequiredArgsConstructor
public class UpdateClosedApplicationEntryValidator
        implements Validator<
                PayloadForUpdateClosedEntry, UpdateApplicationEntryClosedValidationSuccess> {

    private static final int MAX_NOTES_LENGTH = 4000;

    private final ApplicationListEntryRepository applicationListEntryRepository;

    private final ApplicationListRepository applicationListRepository;

    @Override
    public void validate(PayloadForUpdateClosedEntry validatable) {
        validate(validatable, null);
    }

    @Override
    public <R> R validate(
            PayloadForUpdateClosedEntry validatable,
            BiFunction<
                            PayloadForUpdateClosedEntry,
                            UpdateApplicationEntryClosedValidationSuccess,
                            R>
                    validateSuccess) {

        // validate the list exists
        Optional<ApplicationList> applicationList =
                applicationListRepository.findByUuidIncludingDelete(validatable.getId());
        if (applicationList.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST,
                    "The application list does not exist %s".formatted(validatable.getId()));
        }

        // validate the list entry
        Optional<ApplicationListEntry> entry =
                applicationListEntryRepository.findByUuid(validatable.getEntryId());
        if (entry.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.ENTRY_DOES_NOT_EXIST,
                    "The application entry %s does not exist in application list %s"
                            .formatted(validatable.getEntryId(), validatable.getId()));
        }

        // validate the list entry is within the list
        entry =
                applicationListEntryRepository.findByEntryUuidWithinListUuid(
                        validatable.getId(), validatable.getEntryId());
        if (entry.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.ENTRY_IS_NOT_WITHIN_LIST,
                    "The application list entry does not exist %s".formatted(validatable.getId()));
        }

        // if the status is not closed then error
        if (applicationList.get().getStatus() != Status.CLOSED) {
            throw new AppRegistryException(
                    AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT,
                    "The application list is not closed %s".formatted(validatable.getId()));
        }

        validateCombinedNotesLength(validatable, entry.get());

        if (validateSuccess == null) {
            return null;
        }

        // make the callback to the success callback
        return validateSuccess.apply(
                validatable,
                UpdateApplicationEntryClosedValidationSuccess.builder()
                        .applicationList(applicationList.get())
                        .applicationEntryId(entry.get())
                        .build());
    }

    private void validateCombinedNotesLength(
            PayloadForUpdateClosedEntry validatable, ApplicationListEntry entry) {
        String existingNotes = entry.getNotes() == null ? "" : entry.getNotes();
        String additionalNotes = validatable.getData().getAdditionalNotes();
        int separatorLength = existingNotes.isEmpty() || additionalNotes == null ? 0 : 1;
        int combinedNotesLength =
                existingNotes.length()
                        + separatorLength
                        + (additionalNotes == null ? 0 : additionalNotes.length());

        if (combinedNotesLength > MAX_NOTES_LENGTH) {
            throw new AppRegistryException(
                    AppListEntryError.NOTES_TOO_LONG,
                    "Combined notes length %d exceeds the 4000 character limit"
                            .formatted(combinedNotesLength));
        }
    }
}
