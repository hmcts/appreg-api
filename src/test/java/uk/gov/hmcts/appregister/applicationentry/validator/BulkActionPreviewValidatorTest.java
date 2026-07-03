package uk.gov.hmcts.appregister.applicationentry.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.BulkActionPreviewRequestDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionSelectionDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionSelectionType;
import uk.gov.hmcts.appregister.generated.model.BulkActionType;

class BulkActionPreviewValidatorTest {

    private final BulkActionPreviewValidator validator = new BulkActionPreviewValidator();

    @Test
    void givenNullRequest_whenValidate_thenThrowsBulkActionRequired() {
        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(null));

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.BULK_ACTION_REQUIRED);
    }

    @Test
    void givenMissingAction_whenValidate_thenThrowsBulkActionRequired() {
        var request = new BulkActionPreviewRequestDto().selection(filterSelection());

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(request));

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.BULK_ACTION_REQUIRED);
    }

    @Test
    void givenMissingSelection_whenValidate_thenThrowsSelectionRequired() {
        var request = new BulkActionPreviewRequestDto().action(BulkActionType.UPDATE_NOTES);

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(request));

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.BULK_ACTION_SELECTION_REQUIRED);
    }

    @Test
    void givenMissingSelectionType_whenValidate_thenThrowsSelectionTypeRequired() {
        var request =
                new BulkActionPreviewRequestDto()
                        .action(BulkActionType.UPDATE_NOTES)
                        .selection(new BulkActionSelectionDto());

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(request));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_ACTION_SELECTION_TYPE_REQUIRED);
    }

    @Test
    void givenIdsSelectionWithoutEntryIds_whenValidate_thenThrowsEntryIdsRequired() {
        var request =
                validRequest(
                        new BulkActionSelectionDto().selectionType(BulkActionSelectionType.IDS));

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(request));

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.BULK_ACTION_ENTRY_IDS_REQUIRED);
    }

    @Test
    void givenIdsSelectionWithEmptyEntryIds_whenValidate_thenThrowsEntryIdsRequired() {
        var request =
                validRequest(
                        new BulkActionSelectionDto()
                                .selectionType(BulkActionSelectionType.IDS)
                                .entryIds(List.of()));

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(request));

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.BULK_ACTION_ENTRY_IDS_REQUIRED);
    }

    @Test
    void givenIdsSelectionWithDuplicateEntryIds_whenValidate_thenThrowsEntryIdsMustBeUnique() {
        UUID entryId = UUID.randomUUID();
        var request =
                validRequest(
                        new BulkActionSelectionDto()
                                .selectionType(BulkActionSelectionType.IDS)
                                .entryIds(List.of(entryId, entryId)));

        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(request));

        assertThat(exception.getCode()).isEqualTo(ApplicationListError.ENTRY_IDS_MUST_BE_UNIQUE);
    }

    @Test
    void givenFilterSelection_whenValidate_thenCompletes() {
        assertDoesNotThrow(() -> validator.validate(validRequest(filterSelection())));
    }

    @Test
    void givenIdsSelectionWithEntryIds_whenValidate_thenCompletes() {
        var request =
                validRequest(
                        new BulkActionSelectionDto()
                                .selectionType(BulkActionSelectionType.IDS)
                                .entryIds(List.of(UUID.randomUUID())));

        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void givenSelectedCountWithinLimit_whenValidateLimit_thenCompletes() {
        assertDoesNotThrow(() -> validator.validateLimit(2, 2));
    }

    @Test
    void givenSelectedCountAboveLimit_whenValidateLimit_thenThrowsSelectionExceedsLimit() {
        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validateLimit(3, 2));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.BULK_ACTION_SELECTION_EXCEEDS_LIMIT);
    }

    private BulkActionPreviewRequestDto validRequest(BulkActionSelectionDto selection) {
        return new BulkActionPreviewRequestDto()
                .action(BulkActionType.UPDATE_NOTES)
                .selection(selection);
    }

    private BulkActionSelectionDto filterSelection() {
        return new BulkActionSelectionDto().selectionType(BulkActionSelectionType.FILTER);
    }
}
