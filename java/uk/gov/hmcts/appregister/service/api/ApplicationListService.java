package uk.gov.hmcts.appregister.service.api;

import uk.gov.hmcts.appregister.dto.read.ApplicationListDto;
import uk.gov.hmcts.appregister.dto.write.ApplicationListWriteDto;

import java.util.List;

public interface ApplicationListService {
    List<ApplicationListDto> getAllForUser(String userId);
    ApplicationListDto getByIdForUser(Long id, String userId);
    ApplicationListDto create(ApplicationListWriteDto dto, String userId);
    ApplicationListDto update(Long id, ApplicationListWriteDto dto, String userId);
    void delete(Long id, String userId);
}
