package uk.gov.hmcts.appregister.applicationlist.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.hmcts.appregister.generated.model.ApplicationListStatus.OPEN;

/**
 * Service implementation for managing Application List Entries.
 *
 * <p>Handles task-based domain actions that operate across one or more
 * application list entries (for example: bulk resulting, bulk CSV upload, and
 * moving entries between lists).
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ActionsServiceImpl implements ActionsService {
    // Repositories
    private final ApplicationListRepository alRepository;
    private final ApplicationListEntryRepository aleRepository;

    @Override
    @Transactional
    public void move(UUID listId, MoveEntriesDto moveEntriesDto) {
        ApplicationList sourceList =
            alRepository
                .findByUuid(listId)
                .orElseThrow(
                    () ->
                        new AppRegistryException(
                            ApplicationListError.LIST_NOT_FOUND,
                            "No source application list found for UUID '%s'"
                                .formatted(listId)));

        ApplicationList targetList =
            alRepository
                .findByUuid(moveEntriesDto.getTargetListId())
                .orElseThrow(
                    () ->
                        new AppRegistryException(
                            ApplicationListError.LIST_NOT_FOUND,
                            "No target application list found for UUID '%s'"
                                .formatted(moveEntriesDto.getTargetListId())));

        validateLists(sourceList, targetList);

        // Validate payload entry ids
        if (moveEntriesDto.getEntryIds() == null || moveEntriesDto.getEntryIds().isEmpty()) {
            throw new AppRegistryException(
                ApplicationListError.ENTRY_NOT_PROVIDED,
                "No target application list found for UUID '%s'"
                    .formatted(listId));
        }

        final Set<UUID> requestedIds = new HashSet<>(moveEntriesDto.getEntryIds());
        final List<ApplicationListEntry> loadedEntries = aleRepository.findAllByUuidIn(requestedIds);

        final Map<UUID, ApplicationListEntry> loadedByUuid = loadedEntries.stream()
            .collect(Collectors.toMap(ApplicationListEntry::getUuid, e -> e));

        List<ApplicationListEntry> toSave = new ArrayList<>();

        // Validation loop
        for (UUID id : requestedIds) {
            ApplicationListEntry entry = loadedByUuid.get(id);
            if (entry == null) {
                throw new AppRegistryException(
                    ApplicationListError.ENTRY_NOT_FOUND,
                    "No application list entry found for UUID '%s'"
                        .formatted(id));
            }

            // Ensure entry belongs to the source list
            if (!listId.equals(entry.getApplicationList().getUuid())) {
                throw new AppRegistryException(
                    ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST,
                    "Application list entry '%s' does not belong to the source list"
                        .formatted(id));
            }

            // Already in target
            if (entry.getApplicationList().getUuid().equals(moveEntriesDto.getTargetListId())) {
                throw new AppRegistryException(
                    ApplicationListError.ENTRY_ALREADY_IN_TARGET_LIST,
                    "Application list entry '%s' is already in the target list"
                        .formatted(id));
            }

            // Valid candidate
            entry.setApplicationList(targetList);
            toSave.add(entry);
        }

        // Perform moves for candidates
        aleRepository.saveAll(toSave);
    }

    private void validateLists(ApplicationList sourceList, ApplicationList targetList) {
        boolean sourceNotOpen = sourceList.getStatus() != OPEN;
        boolean targetNotOpen = targetList.getStatus() != OPEN;

        if (sourceNotOpen || targetNotOpen) {
            StringBuilder msg = new StringBuilder(
                "Cannot move the applications because the following lists are not OPEN: ");

            if (sourceNotOpen) {
                msg.append(String.format("source list (uuid=%s) ",
                                         sourceList.getUuid()));
            }
            if (targetNotOpen) {
                msg.append(String.format("target list (uuid=%s) ",
                                         targetList.getUuid()));
            }

            throw new AppRegistryException(
                ApplicationListError.INVALID_LIST_STATUS,
                msg.toString().trim()
            );
        }
    }
}
