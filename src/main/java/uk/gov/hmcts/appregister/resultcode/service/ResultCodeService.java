package uk.gov.hmcts.appregister.resultcode.service;

import java.util.List;
import uk.gov.hmcts.appregister.resultcode.dto.ResultCodeDto;

public interface ResultCodeService {
    List<ResultCodeDto> findAll();

    ResultCodeDto findByCode(String code);
}
