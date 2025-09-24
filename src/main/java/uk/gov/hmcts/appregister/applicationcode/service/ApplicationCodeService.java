package uk.gov.hmcts.appregister.applicationcode.service;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.applicationcode.dto.ApplicationCodeDto;

/** Service interface for managing application codes. */
public interface ApplicationCodeService {
    Page<ApplicationCodeDto> findAll(
            String appCode, String appTitle, LocalDate lodgementDate, Pageable pageable);

    ApplicationCodeDto findByCode(String code, LocalDate dateTime);
}
