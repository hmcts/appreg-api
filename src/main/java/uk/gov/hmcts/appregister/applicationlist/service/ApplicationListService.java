package uk.gov.hmcts.appregister.applicationlist.service;

import java.util.List;
import uk.gov.hmcts.appregister.applicationlist.dto.ApplicationListDto;
import uk.gov.hmcts.appregister.applicationlist.dto.ApplicationListWriteDto;

/** Service interface for managing application lists. */
public interface ApplicationListService {
  List<ApplicationListDto> getAllForUser();

  ApplicationListDto getByIdForUser(Long id);

  ApplicationListDto create(ApplicationListWriteDto dto);

  ApplicationListDto update(Long id, ApplicationListWriteDto dto);

  void delete(Long id);
}
