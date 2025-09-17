package uk.gov.hmcts.appregister.applicationcode.service;

import java.time.OffsetDateTime;
import java.util.List;
import uk.gov.hmcts.appregister.applicationcode.dto.ApplicationCodeDto;

/** Service interface for managing application codes. */
public interface ApplicationCodeService {
    List<ApplicationCodeDto> findAll();

    ApplicationCodeDto findByCode(String code, OffsetDateTime dateTime);
}
