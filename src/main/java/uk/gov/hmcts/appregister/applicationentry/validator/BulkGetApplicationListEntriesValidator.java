package uk.gov.hmcts.appregister.applicationentry.validator;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.model.EntryToList;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.BulkGetApplicationListEntriesRequestDto;

@Component
@RequiredArgsConstructor
public class BulkGetApplicationListEntriesValidator {
    private static final int MAX_LIST_IDS = 2000;

    private final ApplicationListRepository applicationListRepository;
    private final ApplicationListEntryRepository applicationListEntryRepository;

    public void validate(BulkGetApplicationListEntriesRequestDto request) {
        if (request == null) {
            throw new AppRegistryException(
                    ApplicationListError.LIST_IDS_REQUIRED,
                    "'listIds' must be provided and non-empty");
        }

        val listIds = request.getListIds();
        if (listIds == null || listIds.isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListError.LIST_IDS_REQUIRED,
                    "'listIds' must be provided and non-empty");
        }

        if (listIds.stream().anyMatch(Objects::isNull)) {
            throw new AppRegistryException(
                    ApplicationListError.LIST_IDS_REQUIRED,
                    "'listIds' must be provided and non-empty");
        }

        val size = listIds.size();
        if (new HashSet<>(listIds).size() != size) {
            throw new AppRegistryException(
                    ApplicationListError.LIST_IDS_MUST_BE_UNIQUE,
                    "Duplicate list IDs are not allowed");
        }

        if (size > MAX_LIST_IDS) {
            throw new AppRegistryException(
                    ApplicationListError.LIST_IDS_LIMIT_EXCEEDED,
                    "No more than %s list IDs are allowed".formatted(MAX_LIST_IDS));
        }

        val foundListIds =
                applicationListRepository.findByUuidIn(listIds).stream()
                        .map(ApplicationList::getUuid)
                        .collect(Collectors.toSet());
        val missingListIds = listIds.stream().filter(id -> !foundListIds.contains(id)).toList();

        if (!missingListIds.isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListError.LIST_NOT_FOUND,
                    "One or more application lists were not found",
                    java.util.Map.of("invalid_list_ids", missingListIds.toString()));
        }

        validateEntryIds(request.getEntryIds(), foundListIds);
    }

    private void validateEntryIds(List<UUID> entryIds, Set<UUID> allowedListIds) {
        if (entryIds == null || entryIds.isEmpty()) {
            return;
        }

        if (entryIds.stream().anyMatch(Objects::isNull)) {
            throw new AppRegistryException(
                    ApplicationListError.ENTRY_NOT_PROVIDED,
                    "'entryIds' must not contain null items");
        }

        val entryIdsSet = new HashSet<>(entryIds);
        if (entryIdsSet.size() != entryIds.size()) {
            throw new AppRegistryException(
                    ApplicationListError.ENTRY_IDS_MUST_BE_UNIQUE,
                    "Duplicate entry IDs are not allowed");
        }

        val entryToLists =
                applicationListEntryRepository.findApplicationListForAllEntries(entryIds);
        val knownIds = entryToLists.stream().map(EntryToList::entryId).collect(Collectors.toSet());
        val wrongListIds =
                entryToLists.stream()
                        .filter(entryToList -> !allowedListIds.contains(entryToList.listId()))
                        .map(EntryToList::entryId)
                        .collect(Collectors.toSet());

        if (!wrongListIds.isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST,
                    "One or more entries were not found in the selected lists",
                    java.util.Map.of("invalid_entry_ids", wrongListIds.toString()));
        }

        val missingIds = entryIds.stream().filter(id -> !knownIds.contains(id)).toList();
        if (!missingIds.isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST,
                    "One or more entries were not found in the selected lists",
                    java.util.Map.of("invalid_entry_ids", missingIds.toString()));
        }
    }
}
