package uk.gov.hmcts.appregister.criminaljusticearea.service;

import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;

/** Service interface for criminal justice area. */
public interface CriminalJusticeService {
    /**
     * gets a criminal justice area by its code.
     *
     * @param code The code to find
     * @return The criminal justice area that has matched the code
     */
    CriminalJusticeAreaGetDto findByCode(String code);
}
