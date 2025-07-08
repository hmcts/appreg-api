package uk.gov.hmcts.appregister.service.api;

import java.util.List;
import uk.gov.hmcts.appregister.dto.read.StandardApplicantDto;

public interface StandardApplicantService {
    List<StandardApplicantDto> findAll();

    StandardApplicantDto findById(Long id);
}
