package uk.gov.hmcts.appregister.applicationlist.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.hmcts.appregister.applicationlist.validator.MoveEntriesValidator;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;

/**
 * Service implementation for managing Application List Entries.
 *
 * <p>Handles task-based domain actions that operate across one or more application list entries
 * (for example: bulk resulting, bulk CSV upload, and moving entries between lists).
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ActionServiceImpl implements ActionService {
    // Repositories
    private final ApplicationListEntryRepository aleRepository;

    // Validators
    private final MoveEntriesValidator moveEntriesValidator;

    // Mappers
    private final PageableMapper pageableMapper;

    @Override
    @Transactional
    public void move(UUID sourceListId, MoveEntriesDto moveEntriesDto) {
        ApplicationList targetList =
            moveEntriesValidator
                .withSourceList(sourceListId)
                .validate(moveEntriesDto, (req, success) ->
                    success.getTargetList());

        // IDs to process
        Set<UUID> requestedIds = new HashSet<>(moveEntriesDto.getEntryIds());

        // Resolve paging size
        int pageSize = resolvePageSize();

        int pageIndex = 0;
        Page<ApplicationListEntry> page;

        do {
            Pageable pageable = PageRequest.of(pageIndex, pageSize);

            // ----- CHUNKED READ -----
            page = aleRepository
                .findAllByUuidInAndApplicationListUuid(requestedIds, sourceListId, pageable);

            if (page.hasContent()) {
                List<ApplicationListEntry> entries = page.getContent();

                // update entries
                for (ApplicationListEntry entry : entries) {
                    entry.setApplicationList(targetList);
                }

                // ----- CHUNKED SAVE -----
                aleRepository.saveAll(entries);
            }

            pageIndex++;

        } while (!page.isLast());

        log.info("Completed paged move for {} entries from list {}", requestedIds.size(), sourceListId);
    }

    private int resolvePageSize() {
        Integer max = pageableMapper.getMaxPageSize();
        Integer def = pageableMapper.getDefaultPageSize();
        return (max != null && max > 0)
            ? max
            : (def != null && def > 0)
            ? def
            : 1000;
    }
}
