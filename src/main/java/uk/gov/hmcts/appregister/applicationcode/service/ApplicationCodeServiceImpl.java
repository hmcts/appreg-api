package uk.gov.hmcts.appregister.applicationcode.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.applicationcode.dto.ApplicationCodeDto;
import uk.gov.hmcts.appregister.applicationcode.exception.AppCodeError;
import uk.gov.hmcts.appregister.applicationcode.mapper.ApplicationCodeMapper;
import uk.gov.hmcts.appregister.applicationfee.service.ApplicationFeeService;
import uk.gov.hmcts.appregister.audit.AuditEventEnum;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

/** Service implementation for managing application codes. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationCodeServiceImpl implements ApplicationCodeService {

    private final ApplicationCodeRepository repository;
    private final ApplicationCodeMapper applicationCodeMapper;
    private final ApplicationFeeService feeService;
    private final AuditOperationService auditService;
    private final List<AuditOperationLifecycleListener> auditLifecycleListeners;

    @Override
    public List<ApplicationCodeDto> findAll() {
        return auditService.processAudit(
                AuditEventEnum.GET_APPLICATION_CODES_AUDIT_EVENT,
                (req) -> {
                    final List<ApplicationCode> applicationCodeList = repository.findAll();
                    List<ApplicationCodeDto> applicationCodeDtoList =
                            applicationCodeList.stream()
                                    .map(
                                            code -> {
                                                FeePair feePair =
                                                        feeService.resolveFeePair(
                                                                code.getFeeReference());
                                                return applicationCodeMapper.toReadDto(
                                                        code, feePair);
                                            })
                                    .toList();

                    return Optional.of(applicationCodeDtoList);
                },
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }

    @Override
    public ApplicationCodeDto findByCode(String code, OffsetDateTime date) {
        return auditService.processAudit(
                AuditEventEnum.GET_APPLICATION_CODE_AUDIT_EVENT,
                (req) -> {
                    final List<ApplicationCode> applicationCodeResults =
                            repository.findByCodeAndDate(code, date);

                    ApplicationCode codeToConsider = null;

                    // if empty throw an exception
                    if (applicationCodeResults.isEmpty()) {
                        throw new AppRegistryException(
                                AppCodeError.CODE_NOT_FOUND,
                                " No code found for code %s and date %s".formatted(code, date));
                    } else {
                        log.warn(
                                "Too many records found for code %s and date %s. Defaulting to first one"
                                        .formatted(code, date));
                        codeToConsider = applicationCodeResults.stream().findFirst().get();
                    }

                    FeePair feePair = feeService.resolveFeePair(codeToConsider.getFeeReference());
                    return Optional.of(applicationCodeMapper.toReadDto(codeToConsider, feePair));
                },
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }
}
