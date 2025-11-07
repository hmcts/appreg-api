package uk.gov.hmcts.appregister.standardapplicant.service;


import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPage;
import uk.gov.hmcts.appregister.standardapplicant.dto.StandardApplicantDto;

/**
 * Service interface for managing Standard Applicants.
 */
public interface StandardApplicantService {
    /**
     * Page data according to search criteria.
     * @param code The code
     * @param name The name
     * @param pageable The pageable
     * @return The standard applicant page
     */
    StandardApplicantPage findAll(String code, String name, Pageable pageable) ;

    StandardApplicantDto findById(Long id);
}
