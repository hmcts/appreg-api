package uk.gov.hmcts.appregister.applicationcode.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.applicationcode.exception.ApplicationCodeError;
import uk.gov.hmcts.appregister.applicationcode.mapper.ApplicationCodeMapper;
import uk.gov.hmcts.appregister.applicationfee.service.ApplicationFeeService;
import uk.gov.hmcts.appregister.audit.AuditEventEnum;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodePage;

/**
 * Service implementation for managing application codes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationCodeServiceImpl implements ApplicationCodeService {

    private static final int SINGLE_RECORD = 1;

    private final ApplicationCodeRepository repository;
    private final ApplicationCodeMapper applicationCodeMapper;
    private final ApplicationFeeService feeService;
    private final AuditOperationService auditService;
    private final List<AuditOperationLifecycleListener> auditLifecycleListeners;
    private final PageMapper pageMapper;
    private final Clock clock;
    private final ZoneId ukZone;

    @Override
    @Transactional(readOnly = true)
    public ApplicationCodePage findAll(String appCode, String appTitle, Pageable pageable) {

        // Use today's date to ensure we only return Result Codes that are currently active.
        var todayUk = LocalDate.now(clock.withZone(ukZone));

        return auditService.processAudit(
                AuditEventEnum.GET_APPLICATION_CODES_AUDIT_EVENT,
                (req) -> {
                    log.debug(
                            "Start: Find Application List for: app code: {} app title: {} with paging: {}",
                            appCode,
                            appTitle,
                            pageable);

                    final Page<ApplicationCode> applicationCodeList =
                            repository.search(appCode, appTitle, todayUk, pageable);

                    ApplicationCodePage newPage = new ApplicationCodePage();
                    pageMapper.toPage(applicationCodeList, newPage);

                    // Map each entity to a summary DTO and add to the page content
                    applicationCodeList.map(
                            code -> {
                                FeePair feePair = feeService.resolveFeePair(code.getFeeReference());

                                return newPage.addContentItem(
                                        applicationCodeMapper.toApplicationCodeGetSummaryDto(
                                                code,
                                                feePair != null ? feePair.mainFee() : null,
                                                feePair != null ? feePair.offsiteFee() : null));
                            });

                    log.debug(
                            "Finished: Find Application List for: app code: {} app title: {} with paging: {}",
                            appCode,
                            appTitle,
                            pageable);

                    return Optional.of(newPage);
                },
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationCodeGetDetailDto findByCode(String code, LocalDate date) {
        return auditService.processAudit(
                AuditEventEnum.GET_APPLICATION_CODE_AUDIT_EVENT,
                req -> {
                    log.debug("Start: Find Application for app code: {} date: {}", code, date);

                    final List<ApplicationCode> applicationCodeResults =
                            repository.findByCodeAndDate(code, date);

                    ApplicationCode codeToConsider = null;

                    if (applicationCodeResults.isEmpty()) {
                        throw new AppRegistryException(
                                ApplicationCodeError.CODE_NOT_FOUND,
                                " No code found for code %s and date %s".formatted(code, date));
                    } else {
                        if (applicationCodeResults.size() > 1) {
                            log.warn(
                                    "Too many records found for code %s and date %s. Defaulting to first one"
                                            .formatted(code, date));
                        }

                        codeToConsider = applicationCodeResults.getFirst();
                    }

                    FeePair feePair = feeService.resolveFeePair(codeToConsider.getFeeReference());
                    Optional<ApplicationCodeGetDetailDto> result =
                            Optional.of(
                                    applicationCodeMapper.toApplicationCodeGetDetailDto(
                                            codeToConsider,
                                            feePair != null ? feePair.mainFee() : null,
                                            feePair != null ? feePair.offsiteFee() : null));

                    log.debug("Finish: Find Application for app code: {} date: {}", code, date);
                    return result;
                },
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }
}
