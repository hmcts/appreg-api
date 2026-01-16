package uk.gov.hmcts.appregister.applicationcode.service;

import uk.gov.hmcts.appregister.common.model.PayloadForGet;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodePage;

/**
 * Service interface for managing application codes.
 */
public interface ApplicationCodeService {
    ApplicationCodePage findAll(String appCode, String appTitle, PagingWrapper pageable);

    /**
     * find the application code details by code.
     *
     * @param payloadForGet the payload that contains a code and a date
     * @return The dto containing application code details
     */
    ApplicationCodeGetDetailDto findByCode(PayloadForGet payloadForGet);
}
