package uk.gov.hmcts.appregister.applicationentry.service;

import java.util.List;
import uk.gov.hmcts.appregister.applicationentry.dto.ApplicationListEntryDto;
import uk.gov.hmcts.appregister.applicationentry.dto.ApplicationWriteDto;

/** Service interface for managing applications within application lists. */
public interface ApplicationListEntryService {
    List<ApplicationListEntryDto> getAllByListId(Long listId);

    ApplicationListEntryDto getByIdForUser(Long listId, Long appId);

    ApplicationListEntryDto create(Long listId, ApplicationWriteDto appWriteDto);

    ApplicationListEntryDto update(Long listId, Long appId, ApplicationWriteDto appWriteDto);

    void delete(Long listId, Long appId);
}
