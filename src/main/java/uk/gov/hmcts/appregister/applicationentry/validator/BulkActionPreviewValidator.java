package uk.gov.hmcts.appregister.applicationentry.validator;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.validator.Validator;
import uk.gov.hmcts.appregister.generated.model.BulkActionPreviewRequestDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionSelectionDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionSelectionType;

/**
 * Validates bulk action preview semantic rules and enforces global selection limits.
 */
@Component
public class BulkActionPreviewValidator implements Validator<BulkActionPreviewRequestDto, Void> {

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

    public void validateLimit(long selectedCount, int limit) {
        if (selectedCount > limit) {
            throw new AppRegistryException(
                    AppListEntryError.BULK_ACTION_SELECTION_EXCEEDS_LIMIT,
                    "Selected count %d exceeds limit %d".formatted(selectedCount, limit));
        }
    }

    private void validateIdsSelection(BulkActionSelectionDto selection) {
        if (isNullOrEmpty(selection.getEntryIds())
                || selection.getEntryIds().stream().anyMatch(Objects::isNull)) {
            throw new AppRegistryException(
                    AppListEntryError.BULK_ACTION_ENTRY_IDS_REQUIRED,
                    "entryIds must be provided for IDS selection");
        }

        List<UUID> entryIds = selection.getEntryIds();
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
