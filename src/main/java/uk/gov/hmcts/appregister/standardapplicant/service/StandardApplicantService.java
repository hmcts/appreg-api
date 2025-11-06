package uk.gov.hmcts.appregister.standardapplicant.service;

import java.time.LocalDate;
import java.util.List;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.standardapplicant.dto.StandardApplicantDto;

/**
 * Service interface for managing Standard Applicants.
 */
public interface StandardApplicantService {
    List<StandardApplicantDto> findAll();

    /**
     * finds a standard applicant by code and date.
     *
     * @param code The code of the standard applicant.
     * @param date The date to check the validity of the standard applicant. The date has to be
     *     before the date of the standard applicant and after the expiry date (if present).
     * @return The standard applicant detail DTO.
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException In the eventuality
     *     that the code can't be found
     */
    StandardApplicantGetDetailDto findByCode(String code, LocalDate date);
}
