package uk.gov.hmcts.appregister.service.api;

import java.util.List;
import uk.gov.hmcts.appregister.dto.read.ApplicationListDto;
import uk.gov.hmcts.appregister.dto.write.ApplicationListWriteDto;

public interface ApplicationListService {
    List<ApplicationListDto> getAllForUser(String userId);

    ApplicationListDto getByIdForUser(Long id, String userId);

    ApplicationListDto create(ApplicationListWriteDto dto, String userId);

    ApplicationListDto update(Long id, ApplicationListWriteDto dto, String userId);

    void delete(Long id, String userId);
}
