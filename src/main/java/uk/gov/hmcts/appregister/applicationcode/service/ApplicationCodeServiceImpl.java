package uk.gov.hmcts.appregister.applicationcode.service;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    @Transactional
    public Page<ApplicationCodeDto> findAll(
            String appCode, String appTitle, LocalDate lodgementDate, Pageable pageable) {
        return auditService.processAudit(
                AuditEventEnum.GET_APPLICATION_CODES_AUDIT_EVENT,
                (req) -> {
                    final Page<ApplicationCode> applicationCodeList =
                            repository.search(
                                    appCode,
                                    appTitle,
                                    lodgementDate != null,
                                    lodgementDate != null
                                            ? lodgementDate.atStartOfDay().atOffset(ZoneOffset.UTC)
                                            : null,
                                    lodgementDate != null
                                            ? lodgementDate
                                                    .plusDays(1)
                                                    .atStartOfDay()
                                                    .atOffset(ZoneOffset.UTC)
                                            : null,
                                    pageable);
                    return Optional.of(
                            applicationCodeList.map(
                                    code -> {
                                        FeePair feePair =
                                                feeService.resolveFeePair(code.getFeeReference());
                                        return applicationCodeMapper.toReadDto(code, feePair);
                                    }));
                },
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }

    @Override
    public ApplicationCodeDto findByCode(String code, LocalDate date) {
        return auditService.processAudit(
                AuditEventEnum.GET_APPLICATION_CODE_AUDIT_EVENT,
                req -> {
                    final List<ApplicationCode> applicationCodeResults =
                            repository.findByCodeAndDate(
                                    code, date.atStartOfDay().atOffset(ZoneOffset.UTC));

                    ApplicationCode codeToConsider = null;

                    // if empty throw an exception
                    if (applicationCodeResults.isEmpty()) {
                        throw new AppRegistryException(
                                AppCodeError.CODE_NOT_FOUND,
                                " No code found for code %s and date %s".formatted(code, date));
                    } else {

                        if (applicationCodeResults.size() > 1) {
                            log.warn(
                                    "Too many records found for code %s and date %s. Defaulting to first one"
                                            .formatted(code, date));
                        }

                        codeToConsider = applicationCodeResults.stream().findFirst().get();
                    }

                    FeePair feePair = feeService.resolveFeePair(codeToConsider.getFeeReference());
                    return Optional.of(applicationCodeMapper.toReadDto(codeToConsider, feePair));
                },
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }
}
