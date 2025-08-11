package uk.gov.hmcts.appregister.standardapplicant.service;

import java.util.List;
import uk.gov.hmcts.appregister.standardapplicant.dto.StandardApplicantDto;

public interface StandardApplicantService {
    List<StandardApplicantDto> findAll();

    StandardApplicantDto findById(Long id);
}
