package uk.gov.hmcts.appregister.applicationentry.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.model.EntryToList;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntryBulkActionPreviewRequestDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntryBulkActionSelectionDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionPreviewRequestDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionSelectionDto;
import uk.gov.hmcts.appregister.generated.model.BulkActionSelectionType;
import uk.gov.hmcts.appregister.generated.model.BulkActionType;

class BulkActionPreviewValidatorTest {

    private final ApplicationListEntryRepository applicationListEntryRepository =
            mock(ApplicationListEntryRepository.class);
    private final BulkActionPreviewValidator validator =
            new BulkActionPreviewValidator(applicationListEntryRepository);

    @Test
    void givenNullRequest_whenValidate_thenThrowsBulkActionRequired() {
        AppRegistryException exception =
                assertThrows(AppRegistryException.class, () -> validator.validate(null));

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.BULK_ACTION_REQUIRED);
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
    void givenIdsSelectionWithNullEntryId_whenValidate_thenThrowsEntryIdsRequired() {
        var request =
                validRequest(
                        new BulkActionSelectionDto()
                                .selectionType(BulkActionSelectionType.IDS)
                                .entryIds(Collections.singletonList(null)));

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
    void givenApplicationListIdsSelectionWithOwnedEntryIds_whenValidate_thenCompletes() {
        UUID listId = UUID.randomUUID();
        UUID firstEntryId = UUID.randomUUID();
        UUID secondEntryId = UUID.randomUUID();
        when(applicationListEntryRepository.findApplicationListForAllEntries(
                        List.of(firstEntryId, secondEntryId)))
                .thenReturn(
                        List.of(
                                new EntryToList(firstEntryId, listId),
                                new EntryToList(secondEntryId, listId)));

        assertDoesNotThrow(
                () ->
                        validator.validateApplicationListEntryBulkActionPreview(
                                listId,
                                validApplicationListRequest(
                                        applicationListIdsSelection(firstEntryId, secondEntryId))));
    }

    @Test
    void givenApplicationListIdsSelectionWithWrongListEntry_whenValidate_thenThrowsNotAccessible() {
        UUID listId = UUID.randomUUID();
        UUID validEntryId = UUID.randomUUID();
        UUID otherListEntryId = UUID.randomUUID();
        UUID otherListId = UUID.randomUUID();
        when(applicationListEntryRepository.findApplicationListForAllEntries(
                        List.of(validEntryId, otherListEntryId)))
                .thenReturn(
                        List.of(
                                new EntryToList(validEntryId, listId),
                                new EntryToList(otherListEntryId, otherListId)));

        AppRegistryException exception =
                assertThrows(
                        AppRegistryException.class,
                        () ->
                                validator.validateApplicationListEntryBulkActionPreview(
                                        listId,
                                        validApplicationListRequest(
                                                applicationListIdsSelection(
                                                        validEntryId, otherListEntryId))));

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.ENTRY_NOT_ACCESSIBLE_FOR_LIST);
        assertThat(exception.getDetails().get("invalid_entry_ids"))
                .contains(otherListEntryId.toString());
    }

    @Test
    void givenApplicationListIdsSelectionWithMissingEntry_whenValidate_thenThrowsNotInSourceList() {
        UUID listId = UUID.randomUUID();
        UUID validEntryId = UUID.randomUUID();
        UUID missingEntryId = UUID.randomUUID();
        when(applicationListEntryRepository.findApplicationListForAllEntries(
                        List.of(validEntryId, missingEntryId)))
                .thenReturn(List.of(new EntryToList(validEntryId, listId)));

        AppRegistryException exception =
                assertThrows(
                        AppRegistryException.class,
                        () ->
                                validator.validateApplicationListEntryBulkActionPreview(
                                        listId,
                                        validApplicationListRequest(
                                                applicationListIdsSelection(
                                                        validEntryId, missingEntryId))));

        assertThat(exception.getCode()).isEqualTo(ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST);
        assertThat(exception.getDetails().get("invalid_entry_ids"))
                .contains(missingEntryId.toString());
    }

    @Test
    void givenApplicationListFilterSelection_whenValidate_thenDoesNotValidateEntryOwnership() {
        validator.validateApplicationListEntryBulkActionPreview(
                UUID.randomUUID(), validApplicationListRequest(applicationListFilterSelection()));

        verify(applicationListEntryRepository, never()).findApplicationListForAllEntries(anyList());
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

    private ApplicationListEntryBulkActionPreviewRequestDto validApplicationListRequest(
            ApplicationListEntryBulkActionSelectionDto selection) {
        return new ApplicationListEntryBulkActionPreviewRequestDto()
                .action(BulkActionType.UPDATE_FEE_DETAILS)
                .selection(selection);
    }

    private BulkActionSelectionDto filterSelection() {
        return new BulkActionSelectionDto().selectionType(BulkActionSelectionType.FILTER);
    }

    private ApplicationListEntryBulkActionSelectionDto applicationListIdsSelection(
            UUID... entryIds) {
        return new ApplicationListEntryBulkActionSelectionDto()
                .selectionType(BulkActionSelectionType.IDS)
                .entryIds(List.of(entryIds));
    }

    private ApplicationListEntryBulkActionSelectionDto applicationListFilterSelection() {
        return new ApplicationListEntryBulkActionSelectionDto()
                .selectionType(BulkActionSelectionType.FILTER);
    }
}
