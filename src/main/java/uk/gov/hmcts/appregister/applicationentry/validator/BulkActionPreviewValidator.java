package uk.gov.hmcts.appregister.applicationentry.validator;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.model.EntryToList;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.validator.Validator;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntryBulkActionPreviewRequestDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntryBulkActionSelectionDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionPreviewRequestDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionSelectionDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionSelectionType;

/**
 * Validates bulk action preview semantic rules and enforces global selection limits.
 */
@Component
public class BulkActionPreviewValidator implements Validator<BulkActionPreviewRequestDto, Void> {
    private final ApplicationListEntryRepository applicationListEntryRepository;

    public BulkActionPreviewValidator(
            ApplicationListEntryRepository applicationListEntryRepository) {
        this.applicationListEntryRepository = applicationListEntryRepository;
    }

    @Override
    public void validate(BulkActionPreviewRequestDto request) {
        if (request == null) {
            throw new AppRegistryException(
                    AppListEntryError.BULK_ACTION_REQUIRED, "Bulk action request is missing");
        }

        BulkActionSelectionDto selection = request.getSelection();

        if (selection.getSelectionType() == BulkActionSelectionType.IDS) {
            validateIdsSelection(selection);
        }
    }

    public void validateApplicationListEntryBulkActionPreview(
            UUID listId, ApplicationListEntryBulkActionPreviewRequestDto request) {
        if (request == null) {
            throw new AppRegistryException(
                    AppListEntryError.BULK_ACTION_REQUIRED, "Bulk action request is missing");
        }

        ApplicationListEntryBulkActionSelectionDto selection = request.getSelection();

        if (selection.getSelectionType() == BulkActionSelectionType.IDS) {
            validateIdsSelection(selection);
            validateApplicationListEntryOwnership(listId, selection.getEntryIds());
        }
    }

    public void validateLimit(long selectedCount, int limit) {
        if (selectedCount > limit) {
            throw new AppRegistryException(
                    AppListEntryError.BULK_ACTION_SELECTION_EXCEEDS_LIMIT,
                    "Selected count %d exceeds limit %d".formatted(selectedCount, limit));
        }
    }

    private void validateIdsSelection(BulkActionSelectionDto selection) {
        validateEntryIds(selection.getEntryIds());
    }

    private void validateIdsSelection(ApplicationListEntryBulkActionSelectionDto selection) {
        validateEntryIds(selection.getEntryIds());
    }

    private void validateApplicationListEntryOwnership(UUID listId, List<UUID> entryIds) {
        List<EntryToList> entryToLists =
                applicationListEntryRepository.findApplicationListForAllEntries(entryIds);

        Set<UUID> knownIds =
                entryToLists.stream().map(EntryToList::entryId).collect(Collectors.toSet());
        Set<UUID> wrongListIds =
                entryToLists.stream()
                        .filter(entryToList -> !listId.equals(entryToList.listId()))
                        .map(EntryToList::entryId)
                        .collect(Collectors.toSet());

        if (!wrongListIds.isEmpty()) {
            throw new AppRegistryException(
                    AppListEntryError.ENTRY_NOT_ACCESSIBLE_FOR_LIST,
                    "One or more entries do not belong to the application list",
                    Map.of("invalid_entry_ids", wrongListIds.toString()));
        }

        Set<UUID> missingIds = new HashSet<>(entryIds);
        missingIds.removeAll(knownIds);
        if (!missingIds.isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST,
                    "One or more entries were not found in the source list",
                    Map.of("invalid_entry_ids", missingIds.toString()));
        }
    }

    private void validateEntryIds(List<UUID> entryIds) {
        if (isNullOrEmpty(entryIds) || entryIds.stream().anyMatch(Objects::isNull)) {
            throw new AppRegistryException(
                    AppListEntryError.BULK_ACTION_ENTRY_IDS_REQUIRED,
                    "entryIds must be provided for IDS selection");
        }

        if (new HashSet<>(entryIds).size() != entryIds.size()) {
            throw new AppRegistryException(
                    ApplicationListError.ENTRY_IDS_MUST_BE_UNIQUE,
                    "Duplicate entry IDs are not allowed");
        }
    }

    private boolean isNullOrEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }
}
