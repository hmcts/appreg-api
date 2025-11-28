package uk.gov.hmcts.appregister.applicationlist.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.applicationlist.validator.MoveEntriesValidationSuccess;
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
        MoveEntriesValidationSuccess validationSuccess =
                moveEntriesValidator
                        .withSourceList(listId)
                        .validate(moveEntriesDto, (request, success) -> success);

        // If validator returned null (defensive) treat as no-op
        List<uk.gov.hmcts.appregister.common.entity.ApplicationListEntry> entriesToSave =
                validationSuccess == null || validationSuccess.getEntriesToSave() == null
                        ? List.of()
                        : validationSuccess.getEntriesToSave();

        // determine chunk size from pageable mapper (prefer the configured maxPageSize)
        Integer maxPageSize = pageableMapper.getMaxPageSize();
        Integer defaultPageSize = pageableMapper.getDefaultPageSize();
        int chunkSize =
                (maxPageSize != null && maxPageSize > 0)
                        ? maxPageSize
                        : (defaultPageSize != null && defaultPageSize > 0) ? defaultPageSize : 1000;

        int total = entriesToSave.size();
        log.info(
                "Starting move of {} entries from list {} to target list {} using chunk size {}",
                total,
                listId,
                moveEntriesDto.getTargetListId(),
                chunkSize);

        int processed = 0;
        for (int start = 0; start < total; start += chunkSize) {
            int end = Math.min(start + chunkSize, total);

            // make a copy of the sublist to avoid holding references to the full list
            List<uk.gov.hmcts.appregister.common.entity.ApplicationListEntry> chunk =
                    new ArrayList<>(entriesToSave.subList(start, end));

            log.debug("Persisting chunk: start={}, end={}, size={}", start, end, chunk.size());

            // persist this chunk
            aleRepository.saveAll(chunk);

            processed += chunk.size();
            log.info("Processed {}/{} entries", processed, total);
        }

        log.info("Completed move of {} entries from list {}", total, listId);
    }
}
