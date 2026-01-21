package uk.gov.hmcts.appregister.criminaljusticearea.service;

import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaPage;

/**
 * Service interface for criminal justice area.
 */
public interface CriminalJusticeService {
    /**
     * gets a criminal justice area by its code.
     *
     * @param code The code to find
     * @return The criminal justice area that has matched the code
     */
    CriminalJusticeAreaGetDto findByCode(String code);

    /**
     * find all of the criminal justice areas, optionally filtered by code and/or description.
     *
     * @param code The code to filter by (optional)
     * @param description The description to filter by (optional)
     * @param pageable The pageable details to establish a page of data
     * @return The pageable data that is returned
     */
    CriminalJusticeAreaPage findAll(String code, String description, PagingWrapper pageable);
}
