package uk.gov.hmcts.appregister.applicationresult.service;

import uk.gov.hmcts.appregister.applicationresult.dto.ApplicationResultDto;
import uk.gov.hmcts.appregister.applicationresult.dto.ApplicationResultWriteDto;

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
