package uk.gov.hmcts.appregister.standardapplicant.service;

import java.time.LocalDate;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPage;

/**
 * Service interface for managing Standard Applicants.
 */
public interface StandardApplicantService {
    /**
     * Page data according to search criteria.
     *
     * @param code The code
     * @param name The name
     * @param addressLine1 Address line 1
     * @param from Start date
     * @param to End date
     * @param pageable The pageable
     * @return The standard applicant page
     */
    StandardApplicantPage findAll(
            String code,
            String name,
            String addressLine1,
            LocalDate from,
            LocalDate to,
            PagingWrapper pageable);

    /**
     * finds a standard applicant by code.
     *
     * @param code The code of the standard applicant.
     * @return The standard applicant detail DTO.
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException In the eventuality
     *     that the code can't be found
     */
    StandardApplicantGetDetailDto findByCode(String code);

    String generateCsv(String code, String name);
}
