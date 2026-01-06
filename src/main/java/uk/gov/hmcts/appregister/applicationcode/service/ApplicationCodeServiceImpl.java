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
import uk.gov.hmcts.appregister.applicationcode.audit.AppCodeAuditOperation;
import uk.gov.hmcts.appregister.applicationcode.mapper.ApplicationCodeMapper;
import uk.gov.hmcts.appregister.applicationcode.validator.GetApplicationCodeValidator;
import uk.gov.hmcts.appregister.applicationfee.service.ApplicationFeeService;
import uk.gov.hmcts.appregister.common.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.common.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.common.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.base.FeePair;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForGet;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodePage;

/**
 * Service implementation for managing application codes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationCodeServiceImpl implements ApplicationCodeService {

    private final ApplicationCodeRepository repository;
    private final ApplicationCodeMapper applicationCodeMapper;
    private final ApplicationFeeService feeService;
    private final AuditOperationService auditService;
    private final List<AuditOperationLifecycleListener> auditLifecycleListeners;
    private final PageMapper pageMapper;
    private final Clock clock;
    private final ZoneId ukZone;
    private final GetApplicationCodeValidator getApplicationCodeValidator;

    @Override
    @Transactional(readOnly = true)
    public ApplicationCodePage findAll(String appCode, String appTitle, Pageable pageable) {

        // Use today's date to ensure we only return Result Codes that are currently active.
        var todayUk = LocalDate.now(clock.withZone(ukZone));

        return auditService.processAudit(
                AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT,
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

                    AuditableResult<ApplicationCodePage, ApplicationCode> result =
                            new AuditableResult<>(newPage, null);
                    return Optional.of(result);
                },
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationCodeGetDetailDto findByCode(PayloadForGet payloadForGet) {
        return auditService.processAudit(
                AppCodeAuditOperation.GET_APPLICATION_CODE_AUDIT_EVENT,
                req -> {
                    log.debug(
                            "Start: Find active Application Code using code: {} date: {}",
                            payloadForGet.getCode(),
                            payloadForGet.getDate());

                    return getApplicationCodeValidator.validate(
                            payloadForGet,
                            (payload, success) -> {
                                FeePair feePair =
                                        feeService.resolveFeePair(
                                                success.getApplicationCode().getFeeReference());

                                AuditableResult<ApplicationCodeGetDetailDto, ApplicationCode>
                                        result =
                                                new AuditableResult<>(
                                                        applicationCodeMapper
                                                                .toApplicationCodeGetDetailDto(
                                                                        success
                                                                                .getApplicationCode(),
                                                                        feePair != null
                                                                                ? feePair.mainFee()
                                                                                : null,
                                                                        feePair != null
                                                                                ? feePair
                                                                                        .offsiteFee()
                                                                                : null),
                                                        null);

                                log.debug(
                                        "Finish: Find Application for app code: {} date: {}",
                                        payload.getCode(),
                                        payload.getDate());
                                return Optional.of(result);
                            });
                },
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }
}
