package uk.gov.hmcts.appregister.criminaljusticearea.service;

import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaDto;

/** Service interface for criminal justice areax. */
public interface CriminalJusticeService {
    /**
     * gets a criminal justice area by its code.
     *
     * @param code The code to find
     * @return The criminal justice area that has matched the code
     */
    CriminalJusticeAreaDto findByCode(String code);
}
