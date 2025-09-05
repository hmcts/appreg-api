package uk.gov.hmcts.appregister.applicationentry.service;

import java.util.List;
import uk.gov.hmcts.appregister.applicationentry.dto.ApplicationDto;
import uk.gov.hmcts.appregister.applicationentry.dto.ApplicationWriteDto;

/** Service interface for managing applications within application lists. */
public interface ApplicationService {
    List<ApplicationDto> getAllByListId(Long listId, String userId);

    ApplicationDto getByIdForUser(Long listId, Long appId, String userId);

    ApplicationDto create(Long listId, ApplicationWriteDto appWriteDto, String userId);

    ApplicationDto update(Long listId, Long appId, ApplicationWriteDto appWriteDto, String userId);

    void delete(Long listId, Long appId, String userId);
}
