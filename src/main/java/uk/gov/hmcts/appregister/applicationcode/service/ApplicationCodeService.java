package uk.gov.hmcts.appregister.applicationcode.service;

import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.applicationcode.dto.ApplicationCodeDto;

/** Service interface for managing application codes. */
public interface ApplicationCodeService {
    Page<ApplicationCodeDto> findAll(
            String appCode, String appTitle, OffsetDateTime lodgementDate, Pageable pageable);

    ApplicationCodeDto findByCode(String code);
}
