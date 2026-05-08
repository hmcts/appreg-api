package uk.gov.hmcts.appregister.applicationentry.validator;

import java.util.Optional;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForDeleteEntry;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.validator.Validator;

/**
 * This class represents a validator for deleting an application list entry. It validates in two
 * ways:-
 *
 * <p>1) Ensures the id exists to be deleted 2) Ensures the id is not already soft deleted
 */
@RequiredArgsConstructor
@Component
public class DeleteApplicationListEntryValidator
        implements Validator<PayloadForDeleteEntry, DeleteEntryValidationSuccess> {
    private final ApplicationListRepository applicationListRepository;
    private final ApplicationListEntryRepository applicationListEntryRepository;

    @Override
    public void validate(PayloadForDeleteEntry uuid) {
        validate(uuid, (req, success) -> null);
    }

    @Override
    public <R> R validate(
            PayloadForDeleteEntry deletionId,
            BiFunction<PayloadForDeleteEntry, DeleteEntryValidationSuccess, R> createSupplier) {

        Optional<ApplicationList> applicationList =
                applicationListRepository.findByUuid(deletionId.getId());

        if (applicationList.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST,
                    "Application list id %s not found".formatted(deletionId));
        }

        Optional<ApplicationListEntry> entry =
                applicationListEntryRepository.findByUuidIncludingDelete(deletionId.getEntryId());

        if (entry.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.LIST_ENTRY_NOT_FOUND,
                    "Application list entry id %s not found".formatted(deletionId.getEntryId()));
        }

        if (entry.get().isDeleted()) {
            throw new AppRegistryException(
                    AppListEntryError.DELETION_ALREADY_IN_DELETABLE_STATE,
                    "Application list entry id %s is in a deletable state"
                            .formatted(deletionId.getEntryId()));
        }

        // ensure the entry belongs to the list
        entry =
                applicationListEntryRepository.findByEntryUuidWithinListUuid(
                        deletionId.getId(), deletionId.getEntryId());
        if (entry.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.ENTRY_IS_NOT_WITHIN_LIST,
                    "The application list entry %s does not exist in the list %s"
                            .formatted(deletionId.getEntryId(), deletionId.getId()));
        }

        // Build success object and pass it into the caller-supplied function
        DeleteEntryValidationSuccess success = new DeleteEntryValidationSuccess(entry.get());
        return createSupplier.apply(deletionId, success);
    }
}
