package uk.gov.hmcts.appregister.service.api;

import uk.gov.hmcts.appregister.dto.read.ApplicationResultDto;
import uk.gov.hmcts.appregister.dto.write.ApplicationResultWriteDto;

public interface ApplicationResultService {
    ApplicationResultDto getResultForApplication(Long listId, Long applicationId, String userId);

    ApplicationResultDto create(
            Long listId, Long applicationId, ApplicationResultWriteDto dto, String userId);

    ApplicationResultDto update(
            Long listId,
            Long applicationId,
            Long resultId,
            ApplicationResultWriteDto dto,
            String userId);

    void delete(Long listId, Long applicationId, Long resultId, String userId);
}
