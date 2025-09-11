package uk.gov.hmcts.appregister.applicationcode.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.applicationcode.dto.ApplicationCodeDto;
import uk.gov.hmcts.appregister.applicationcode.exception.AppCodeError;
import uk.gov.hmcts.appregister.applicationcode.mapper.ApplicationCodeMapper;
import uk.gov.hmcts.appregister.applicationfee.service.ApplicationFeeService;
import uk.gov.hmcts.appregister.audit.AuditEnum;
import uk.gov.hmcts.appregister.audit.service.AuditService;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

/** Service implementation for managing application codes. */
@Service
@RequiredArgsConstructor
public class ApplicationCodeServiceImpl implements ApplicationCodeService {

    private final ApplicationCodeRepository repository;
    private final ApplicationCodeMapper applicationCodeMapper;
    private final ApplicationFeeService feeService;
    private final AuditService auditService;

    @Override
    public List<ApplicationCodeDto> findAll() {
        final List<ApplicationCode> applicationCodeList = repository.findAll();

        List<ApplicationCodeDto> applicationCodeDtoList =
                applicationCodeList.stream()
                        .map(
                                code -> {
                                    FeePair feePair =
                                            feeService.resolveFeePair(code.getFeeReference());
                                    return applicationCodeMapper.toReadDto(code, feePair);
                                })
                        .toList();

        /// audit the operation
        auditService.record(AuditEnum.GET_APPLICATION_CODES_AUDIT_EVENT);

        return applicationCodeDtoList;
    }

    @Override
    public ApplicationCodeDto findByCode(String code) {
        final ApplicationCode applicationCode =
                repository
                        .findByCode(code)
                        .orElseThrow(
                                () -> {
                                    throw new AppRegistryException(
                                            AppCodeError.CODE_NOT_FOUND, "", null);
                                });

        FeePair feePair = feeService.resolveFeePair(applicationCode.getFeeReference());

        // audit the operation
        auditService.record(AuditEnum.GET_APPLICATION_CODE_AUDIT_EVENT);
        return applicationCodeMapper.toReadDto(applicationCode, feePair);
    }
}
