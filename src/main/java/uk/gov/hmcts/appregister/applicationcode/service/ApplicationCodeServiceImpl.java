package uk.gov.hmcts.appregister.applicationcode.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.applicationcode.dto.ApplicationCodeDto;
import uk.gov.hmcts.appregister.applicationcode.mapper.ApplicationCodeMapper;
import uk.gov.hmcts.appregister.applicationfee.service.ApplicationFeeService;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;

/** Service implementation for managing application codes. */
@Service
@RequiredArgsConstructor
public class ApplicationCodeServiceImpl implements ApplicationCodeService {

  private final ApplicationCodeRepository repository;
  private final ApplicationCodeMapper applicationCodeMapper;
  private final ApplicationFeeService feeService;

  @Override
  public List<ApplicationCodeDto> findAll() {
    final List<ApplicationCode> applicationCodeList = repository.findAll();

    return applicationCodeList.stream()
        .map(
            code -> {
              FeePair feePair = feeService.resolveFeePair(code.getFeeReference());
              return applicationCodeMapper.toReadDto(code, feePair);
            })
        .toList();
  }

  @Override
  public ApplicationCodeDto findByCode(String code) {
    final ApplicationCode applicationCode =
        repository
            .findByApplicationCode(code)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Application Code not found"));

    FeePair feePair = feeService.resolveFeePair(applicationCode.getFeeReference());
    return applicationCodeMapper.toReadDto(applicationCode, feePair);
  }
}
