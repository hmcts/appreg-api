package uk.gov.hmcts.appregister.service.api;

import uk.gov.hmcts.appregister.dto.read.ResultCodeDto;

import java.util.List;

public interface ResultCodeService {
    List<ResultCodeDto> findAll();
    ResultCodeDto findByCode(String code);
}
