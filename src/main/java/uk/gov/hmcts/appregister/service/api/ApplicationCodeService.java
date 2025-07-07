package uk.gov.hmcts.appregister.service.api;

import uk.gov.hmcts.appregister.dto.read.ApplicationCodeDto;
import uk.gov.hmcts.appregister.model.ApplicationCode;

import java.util.List;

public interface ApplicationCodeService {
    List<ApplicationCodeDto> findAll();
    ApplicationCodeDto findByCode(String code);
}
