package uk.gov.hmcts.appregister.applicationlist.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
    public void move(UUID listId, MoveEntriesDto moveEntriesDto) {
        // Validate target list
        ApplicationList targetList =
            alRepository
                .findByUuid(moveEntriesDto.getTargetListId())
                .orElseThrow(
                    () ->
                        new AppRegistryException(
                            ApplicationListError.LIST_NOT_FOUND,
                            "No target application list found for UUID '%s'"
                                .formatted(listId)));

        // Validate payload entry ids
        if (moveEntriesDto.getEntryIds() == null || moveEntriesDto.getEntryIds().isEmpty()) {
            throw new AppRegistryException(
                ApplicationListError.NO_ENTRY_ID,
                "No target application list found for UUID '%s'"
                    .formatted(listId));
        }

        // Bulk fetch entries
        Set<UUID> requestedIds = new HashSet<>(moveEntriesDto.getEntryIds());
        List<ApplicationListEntry> loadedEntries = aleRepository.findAllByUuidIn(requestedIds);
    }
}
