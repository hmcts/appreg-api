package uk.gov.hmcts.appregister.applicationlist.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.hmcts.appregister.applicationlist.validator.MoveEntriesValidator;
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
    public void move(UUID listId, MoveEntriesDto moveEntriesDto) {
        Set<UUID> entryIdsSet = moveEntriesDto.getEntryIds();
        List<UUID> allEntryIds = new ArrayList<>(entryIdsSet);

        // determine chunk size from pageable mapper (prefer the configured maxPageSize)
        Integer maxPageSize = pageableMapper.getMaxPageSize();
        Integer defaultPageSize = pageableMapper.getDefaultPageSize();
        int chunkSize = (maxPageSize != null && maxPageSize > 0) ? maxPageSize
            : (defaultPageSize != null && defaultPageSize > 0) ? defaultPageSize : 1000;

        int total = allEntryIds.size();
        log.info("Starting move of {} entries from list {} to target list {} using chunk size {}",
                 total, listId, moveEntriesDto.getTargetListId(), chunkSize);

        int processed = 0;
        for (int start = 0; start < total; start += chunkSize) {
            int end = Math.min(start + chunkSize, total);

            // make a copy of the sublist to avoid holding references to the full list
            List<UUID> chunkIds = new ArrayList<UUID>(allEntryIds.subList(start, end));

            MoveEntriesDto chunkDto = new MoveEntriesDto();
            chunkDto.setTargetListId(moveEntriesDto.getTargetListId());
            chunkDto.setEntryIds(new HashSet<>(chunkIds));

            log.debug("Processing chunk: start={}, end={}, size={}", start, end, chunkIds.size());

            moveEntriesValidator
                .withSourceList(listId)
                .validate(
                    chunkDto,
                    (request, success) -> {
                        // saveAll for this chunk
                        aleRepository.saveAll(success.getEntriesToSave());
                        return null;
                    }
                );

            processed += chunkIds.size();
            log.info("Processed {}/{} entries", processed, total);
        }

        log.info("Completed move of {} entries from list {}", total, listId);
    }
}
