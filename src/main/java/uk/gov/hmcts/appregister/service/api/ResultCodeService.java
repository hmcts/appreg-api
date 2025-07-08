package uk.gov.hmcts.appregister.service.api;

import java.util.List;
import uk.gov.hmcts.appregister.dto.read.ResultCodeDto;

public interface ResultCodeService {
    List<ResultCodeDto> findAll();

    ResultCodeDto findByCode(String code);
}
