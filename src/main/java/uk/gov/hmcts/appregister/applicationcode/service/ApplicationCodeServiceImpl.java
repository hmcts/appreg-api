package uk.gov.hmcts.appregister.applicationcode.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.applicationcode.audit.AppCodeAuditOperation;
import uk.gov.hmcts.appregister.applicationcode.mapper.ApplicationCodeMapper;
import uk.gov.hmcts.appregister.applicationcode.mapper.CodeAndTitle;
import uk.gov.hmcts.appregister.applicationcode.validator.GetApplicationCodeValidator;
import uk.gov.hmcts.appregister.applicationfee.service.ApplicationFeeService;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForGet;
import uk.gov.hmcts.appregister.common.util.BeanUtil;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
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
    private final PageMapper pageMapper;
    private final Clock clock;
    private final ZoneId ukZone;
    private final GetApplicationCodeValidator getApplicationCodeValidator;

    @Override
    @Transactional(readOnly = true)
    public ApplicationCodePage findAll(
            String appCode, String appTitle, LocalDate effectiveDate, PagingWrapper pageable) {

        // Use today's date when no effective date is supplied to preserve existing search
        // behaviour.
        var searchDate =
                effectiveDate != null ? effectiveDate : LocalDate.now(clock.withZone(ukZone));

        return auditService.processAudit(
                AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT,
                req -> {
                    log.debug(
                            "Start: Find Application Codes for code: {} title: {} date: {} paging: {}",
                            appCode,
                            appTitle,
                            searchDate,
                            pageable);

                    final Page<ApplicationCode> applicationCodeList =
                            repository.search(
                                    appCode, appTitle, searchDate, pageable.getPageable());
                    var feePairsByReference =
                            feeService.resolveFeePairs(
                                    applicationCodeList.stream()
                                            .map(ApplicationCode::getFeeReference)
                                            .distinct()
                                            .toList(),
                                    searchDate);

                    ApplicationCodePage newPage = new ApplicationCodePage();
                    pageMapper.toPage(applicationCodeList, newPage, pageable.getSortStrings());

                    // Map each entity to a summary DTO and add to the page content
                    applicationCodeList.map(
                            code -> {
                                var feePair =
                                        feePairsByReference.getOrDefault(
                                                code.getFeeReference(), new FeePair(null, null));

                                return newPage.addContentItem(
                                        applicationCodeMapper.toApplicationCodeGetSummaryDto(
                                                code, feePair.mainFee(), feePair.offsiteFee()));
                            });

                    log.debug(
                            "Finished: Find Application Codes for code: {} title: {} date: {} paging: {}",
                            appCode,
                            appTitle,
                            searchDate,
                            pageable);

                    CodeAndTitle codeAndTitle = new CodeAndTitle(appCode, appTitle);
                    AuditableResult<ApplicationCodePage, ApplicationCode> result =
                            new AuditableResult<>(
                                    newPage, applicationCodeMapper.toEntity(codeAndTitle));

                    return Optional.of(result);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationCodeGetDetailDto findByCode(PayloadForGet payloadForGet) {
        return auditService.processAudit(
                null,
                AppCodeAuditOperation.GET_APPLICATION_CODE_AUDIT_EVENT,
                req ->
                        getApplicationCodeValidator.validate(
                                payloadForGet,
                                (payload, success) -> {
                                    FeePair feePair =
                                            feeService.resolveFeePair(
                                                    success.getApplicationCode().getFeeReference(),
                                                    payloadForGet.getDate());
                                    Fee offsiteFee = feePair.offsiteFee();

                                    AuditableResult<ApplicationCodeGetDetailDto, ApplicationCode>
                                            result =
                                                    new AuditableResult<>(
                                                            applicationCodeMapper
                                                                    .toApplicationCodeGetDetailDto(
                                                                            success
                                                                                    .getApplicationCode(),
                                                                            feePair.mainFee(),
                                                                            offsiteFee),
                                                            applicationCodeMapper.toEntity(
                                                                    payloadForGet));

                                    log.debug(
                                            "Finish: Find Application for app code: {} date: {}",
                                            payload.getCode(),
                                            payload.getDate());
                                    return Optional.of(result);
                                }));
    }

    @Override
    @Transactional
    public void upsertApplicationCode(ApplicationCode applicationCode) {
        var applicationCodeDB =
                repository.findByCodeAndDate(applicationCode.getCode(), LocalDate.now(clock)).stream()
                        .findFirst();

        if (applicationCodeDB.isEmpty()) {
            auditService.processAudit(
                    AppCodeAuditOperation.CREATE_APPLICATION_CODE_AUDIT_EVENT,
                    req -> {
                        applicationCode.setChangedBy(1L);
                        applicationCode.setChangedDate(OffsetDateTime.now(clock));

                        repository.saveAndFlush(applicationCode);
                        AuditableResult<Void, ApplicationCode> auditableResult =
                                new AuditableResult<>(null, applicationCode);
                        return Optional.of(auditableResult);
                    },
                    auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));

        } else {
            var currentApplicationCode = applicationCodeDB.get();
            var updatedApplicationCode = BeanUtil.copyBean(currentApplicationCode);
            applicationCodeMapper.updateApplicationCode(updatedApplicationCode, applicationCode);

            if (!Objects.equals(updatedApplicationCode.getEndDate(),
                                applicationCode.getEndDate())) {
                updatedApplicationCode.setEndDate(
                    applicationCode.getEndDate());
            }

            auditService.processAudit(
                    currentApplicationCode,
                    AppCodeAuditOperation.UPDATE_APPLICATION_CODE_AUDIT_EVENT,
                    req -> {
                        updatedApplicationCode.setChangedBy(1L);
                        updatedApplicationCode.setChangedDate(OffsetDateTime.now(clock));

                        AuditableResult<Void, ApplicationCode> auditableResult =
                                new AuditableResult<>(null, updatedApplicationCode);
                        return Optional.of(auditableResult);
                    },
                    auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
            repository.saveAndFlush(updatedApplicationCode);
        }
    }
}
