package uk.gov.hmcts.appregister.service.api;

import java.util.List;
import uk.gov.hmcts.appregister.dto.read.ApplicationCodeDto;

public interface ApplicationCodeService {
    List<ApplicationCodeDto> findAll();

    ApplicationCodeDto findByCode(String code);
}
