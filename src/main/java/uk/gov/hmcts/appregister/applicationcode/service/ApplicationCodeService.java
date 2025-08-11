package uk.gov.hmcts.appregister.applicationcode.service;

import java.util.List;
import uk.gov.hmcts.appregister.applicationcode.dto.ApplicationCodeDto;

public interface ApplicationCodeService {
    List<ApplicationCodeDto> findAll();

    ApplicationCodeDto findByCode(String code);
}
