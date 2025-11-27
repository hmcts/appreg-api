package uk.gov.hmcts.appregister.applicationlist.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.hmcts.appregister.applicationlist.validator.MoveEntriesValidator;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
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

    @Override
    @Transactional
    public void move(UUID listId, MoveEntriesDto moveEntriesDto) {
        moveEntriesValidator
            .withSourceList(listId)
            .validate(
                moveEntriesDto,
                (request, success) -> {
                    aleRepository.saveAll(success.getEntriesToSave());
                    return null;
                }
            );
    }
}
