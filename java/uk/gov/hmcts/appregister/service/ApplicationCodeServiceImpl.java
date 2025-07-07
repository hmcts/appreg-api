package uk.gov.hmcts.appregister.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.dto.internal.FeePair;
import uk.gov.hmcts.appregister.dto.read.ApplicationCodeDto;
import uk.gov.hmcts.appregister.mapper.ApplicationCodeMapper;
import uk.gov.hmcts.appregister.model.ApplicationCode;
import uk.gov.hmcts.appregister.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.service.api.ApplicationCodeService;
import uk.gov.hmcts.appregister.service.api.ApplicationFeeService;

import java.util.List;

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
            .map(code -> {
                FeePair feePair = feeService.resolveFeePair(code.getFeeReference());
                return applicationCodeMapper.toReadDto(code, feePair);
            })
            .toList();
    }

    @Override
    public ApplicationCodeDto findByCode(String code) {
        final ApplicationCode applicationCode = repository.findByApplicationCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application Code not found"));

        FeePair feePair = feeService.resolveFeePair(applicationCode.getFeeReference());
        return applicationCodeMapper.toReadDto(applicationCode, feePair);
    }
}
