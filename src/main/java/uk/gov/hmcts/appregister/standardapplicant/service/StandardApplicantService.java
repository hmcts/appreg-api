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
