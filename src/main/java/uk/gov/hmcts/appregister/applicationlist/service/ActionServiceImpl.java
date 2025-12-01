package uk.gov.hmcts.appregister.applicationlist.service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.applicationlist.validator.MoveEntriesValidator;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;

@Slf4j
@RequiredArgsConstructor
@Service
public class ActionServiceImpl implements ActionService {

    private final ApplicationListEntryRepository aleRepository;
    private final MoveEntriesValidator moveEntriesValidator;

    @Override
    @Transactional
    public void move(UUID sourceListId, MoveEntriesDto moveEntriesDto) {
        ApplicationList targetList =
                moveEntriesValidator
                        .withSourceList(sourceListId)
                        .validate(moveEntriesDto, (req, success) -> success.getTargetList());

        Set<UUID> requestedIds = new HashSet<>(moveEntriesDto.getEntryIds());

        int rowsUpdated =
                aleRepository.bulkMoveByUuidAndSourceList(requestedIds, targetList, sourceListId);

        if (rowsUpdated != requestedIds.size()) {
            throw new AppRegistryException(
                    ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST,
                    "One or more entries were not found in the source list");
        }

        log.info(
                "Completed bulk move for {} entries from list {}",
                requestedIds.size(),
                sourceListId);
    }
}
