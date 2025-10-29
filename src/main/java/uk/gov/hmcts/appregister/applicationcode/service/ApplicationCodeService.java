package uk.gov.hmcts.appregister.applicationcode.service;

import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodePage;

/**
 * Service interface for managing application codes.
 */
public interface ApplicationCodeService {
    ApplicationCodePage findAll(String appCode, String appTitle, Pageable pageable);

    ApplicationCodeGetDetailDto findByCode(String code, LocalDate dateTime);
}
