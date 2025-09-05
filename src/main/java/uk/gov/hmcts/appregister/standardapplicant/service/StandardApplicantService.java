package uk.gov.hmcts.appregister.standardapplicant.service;

import java.util.List;
import uk.gov.hmcts.appregister.standardapplicant.dto.StandardApplicantDto;

/** Service interface for managing Standard Applicants. */
public interface StandardApplicantService {
  List<StandardApplicantDto> findAll();

  StandardApplicantDto findById(Long id);
}
