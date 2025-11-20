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
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntriesMoveStatus;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntriesMovedDto;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.hmcts.appregister.generated.model.ApplicationListEntriesMoveMode.PARTIAL;
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
    public ApplicationListEntriesMovedDto move(UUID listId, MoveEntriesDto moveEntriesDto) {
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
                ApplicationListError.NO_ENTRY_ID,
                "No target application list found for UUID '%s'"
                    .formatted(listId));
        }

        final Set<UUID> requestedIds = new HashSet<>(moveEntriesDto.getEntryIds());
        final List<ApplicationListEntry> loadedEntries = aleRepository.findAllByUuidIn(requestedIds);

        final Map<UUID, ApplicationListEntry> loadedByUuid = loadedEntries.stream()
            .collect(Collectors.toMap(ApplicationListEntry::getUuid, e -> e));

        // keep per-entry validation results in a map for decision-making
        final Map<UUID, String> invalidReasons = new HashMap<>(); // id -> reasonCode

        final List<ApplicationListEntry> candidatesToMove = new ArrayList<>();

        // Validation loop
        for (UUID id : requestedIds) {
            ApplicationListEntry entry = loadedByUuid.get(id);
            if (entry == null) {
                invalidReasons.put(id, "ALE_NOT_FOUND");
                continue;
            }

            // If expectedSourceListId provided, ensure entry belongs to it
            if (listId != null &&
                !listId.equals(entry.getApplicationList().getUuid())) {
                invalidReasons.put(id, "INVALID_SOURCE_LIST");
                continue;
            }

            // Already in target
            /*if (entry.getApplicationList().getUuid().equals(moveEntriesDto.getTargetListId())) {
                invalidReasons.put(id, "ALREADY_IN_TARGET");
                continue;
            }*/

            // Valid candidate
            candidatesToMove.add(entry);
        }

        // Decide behavior based on mode
        boolean partialMode = moveEntriesDto.getMode() == PARTIAL;
        boolean anyInvalid = !invalidReasons.isEmpty();

        if (!partialMode && anyInvalid) {
            // ALL_OR_NOTHING and at least one invalid -> abort and return 400 with details
            Map<UUID, String> details = new HashMap<>(invalidReasons);

            throw new AppRegistryException(
                ApplicationListError.INVALID_ENTRY,
                "Validation failed for some entries (ALL_OR_NOTHING): " + details
            );
        }

        // Perform moves for candidates
        int movedCount = 0;

        for (ApplicationListEntry entry : candidatesToMove) {
            // skip entries that were flagged invalid earlier (defensive)
            if (invalidReasons.containsKey(entry.getUuid())) continue;

            entry.setApplicationList(targetList);

            aleRepository.save(entry);
            movedCount++;
        }

        ApplicationListEntriesMoveStatus overallStatus;
        if (movedCount == requestedIds.size()) {
            overallStatus = ApplicationListEntriesMoveStatus.SUCCEEDED;
        } else {
            overallStatus = ApplicationListEntriesMoveStatus.COMPLETED_WITH_ERRORS;
        }

        return new ApplicationListEntriesMovedDto()
            .overallStatus(overallStatus)
            .movedCount(movedCount);
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
