package uk.gov.hmcts.appregister.applicationlist.service;

import java.util.List;
import uk.gov.hmcts.appregister.applicationlist.dto.ApplicationListDto;
import uk.gov.hmcts.appregister.applicationlist.dto.ApplicationListWriteDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;

/**
 * Service interface for managing application lists.
 */
public interface ApplicationListService {
    List<ApplicationListDto> getAll();

    ApplicationListDto getByIdForUser(Long id);

    ApplicationListCreateDto create(ApplicationListGetDetailDto dto);

    ApplicationListDto update(Long id, ApplicationListWriteDto dto);

    void delete(Long id);
}
