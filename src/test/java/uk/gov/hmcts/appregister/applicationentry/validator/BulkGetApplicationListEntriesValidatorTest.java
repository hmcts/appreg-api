package uk.gov.hmcts.appregister.applicationentry.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.model.EntryToList;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.BulkGetApplicationListEntriesRequestDto;

class BulkGetApplicationListEntriesValidatorTest {
    private final ApplicationListRepository applicationListRepository =
            mock(ApplicationListRepository.class);
    private final ApplicationListEntryRepository applicationListEntryRepository =
            mock(ApplicationListEntryRepository.class);
    private final BulkGetApplicationListEntriesValidator validator =
            new BulkGetApplicationListEntriesValidator(
                    applicationListRepository, applicationListEntryRepository);

    @Test
    void givenNullRequest_whenValidate_thenThrowsListIdsRequired() {
        var exception = assertThrows(AppRegistryException.class, () -> validator.validate(null));

        assertThat(exception.getCode()).isEqualTo(ApplicationListError.LIST_IDS_REQUIRED);
    }

    @Test
    void givenDuplicateListIds_whenValidate_thenThrowsListIdsMustBeUnique() {
        UUID listId = UUID.randomUUID();
        var request =
                new BulkGetApplicationListEntriesRequestDto().listIds(List.of(listId, listId));
        var exception = assertThrows(AppRegistryException.class, () -> validator.validate(request));

        assertThat(exception.getCode()).isEqualTo(ApplicationListError.LIST_IDS_MUST_BE_UNIQUE);
    }

    @Test
    void givenTooManyListIds_whenValidate_thenThrowsListIdsLimitExceeded() {
        var listIds = IntStream.range(0, 2001).mapToObj(index -> UUID.randomUUID()).toList();
        var request = new BulkGetApplicationListEntriesRequestDto().listIds(listIds);

        var exception = assertThrows(AppRegistryException.class, () -> validator.validate(request));

        assertThat(exception.getCode()).isEqualTo(ApplicationListError.LIST_IDS_LIMIT_EXCEEDED);
    }

    @Test
    void givenMissingList_whenValidate_thenThrowsListNotFound() {
        UUID listId = UUID.randomUUID();
        when(applicationListRepository.findByUuidIn(List.of(listId))).thenReturn(List.of());
        var request = new BulkGetApplicationListEntriesRequestDto().listIds(List.of(listId));

        var exception = assertThrows(AppRegistryException.class, () -> validator.validate(request));

        assertThat(exception.getCode()).isEqualTo(ApplicationListError.LIST_NOT_FOUND);
    }

    @Test
    void givenDuplicateEntryIds_whenValidate_thenThrowsEntryIdsMustBeUnique() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        when(applicationListRepository.findByUuidIn(List.of(listId)))
                .thenReturn(List.of(applicationList(listId)));
        var request =
                new BulkGetApplicationListEntriesRequestDto()
                        .listIds(List.of(listId))
                        .entryIds(List.of(entryId, entryId));

        var exception = assertThrows(AppRegistryException.class, () -> validator.validate(request));

        assertThat(exception.getCode()).isEqualTo(ApplicationListError.ENTRY_IDS_MUST_BE_UNIQUE);
    }

    @Test
    void givenEntryOutsideSelectedLists_whenValidate_thenThrowsEntryNotInSourceList() {
        UUID listId = UUID.randomUUID();
        UUID otherListId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        when(applicationListRepository.findByUuidIn(List.of(listId)))
                .thenReturn(List.of(applicationList(listId)));
        when(applicationListEntryRepository.findApplicationListForAllEntries(List.of(entryId)))
                .thenReturn(List.of(new EntryToList(entryId, otherListId)));
        var request =
                new BulkGetApplicationListEntriesRequestDto()
                        .listIds(List.of(listId))
                        .entryIds(List.of(entryId));

        var exception = assertThrows(AppRegistryException.class, () -> validator.validate(request));

        assertThat(exception.getCode()).isEqualTo(ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST);
    }

    @Test
    void givenValidRequest_whenValidate_thenCompletes() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        when(applicationListRepository.findByUuidIn(List.of(listId)))
                .thenReturn(List.of(applicationList(listId)));
        when(applicationListEntryRepository.findApplicationListForAllEntries(List.of(entryId)))
                .thenReturn(List.of(new EntryToList(entryId, listId)));

        assertDoesNotThrow(
                () ->
                        validator.validate(
                                new BulkGetApplicationListEntriesRequestDto()
                                        .listIds(List.of(listId))
                                        .entryIds(List.of(entryId))));
    }

    private ApplicationList applicationList(UUID listId) {
        var applicationList = new ApplicationList();
        applicationList.setUuid(listId);
        return applicationList;
    }
}
