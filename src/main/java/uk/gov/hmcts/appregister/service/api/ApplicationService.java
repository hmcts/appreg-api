package uk.gov.hmcts.appregister.service.api;

import java.util.List;
import uk.gov.hmcts.appregister.dto.read.ApplicationDto;
import uk.gov.hmcts.appregister.dto.write.ApplicationWriteDto;

public interface ApplicationService {
    List<ApplicationDto> getAllByListId(Long listId, String userId);

    ApplicationDto getByIdForUser(Long listId, Long appId, String userId);

    ApplicationDto create(Long listId, ApplicationWriteDto appWriteDto, String userId);

    ApplicationDto update(Long listId, Long appId, ApplicationWriteDto appWriteDto, String userId);

    void delete(Long listId, Long appId, String userId);
}
