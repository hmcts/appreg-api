package uk.gov.hmcts.appregister.service.api;

import uk.gov.hmcts.appregister.dto.read.StandardApplicantDto;

import java.util.List;

public interface StandardApplicantService {
    List<StandardApplicantDto> findAll();
    StandardApplicantDto findById(Long id);
}
