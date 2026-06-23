package uk.gov.hmcts.appregister.criminaljusticearea.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.service.LocationLookupService;
import uk.gov.hmcts.appregister.common.util.BeanUtil;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.criminaljusticearea.audit.CriminalJusticeAuditOperation;
import uk.gov.hmcts.appregister.criminaljusticearea.mapper.CodeAndDescription;
import uk.gov.hmcts.appregister.criminaljusticearea.mapper.CriminalJusticeMapper;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaPage;
import uk.gov.hmcts.appregister.resultcode.audit.ResultCodeOperation;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CriminalJusticeServiceImpl implements CriminalJusticeService {
    private final AuditOperationService auditService;
    private final List<AuditOperationLifecycleListener> auditLifecycleListeners;
    private final CriminalJusticeAreaRepository criminalJusticeAreaRepository;
    private final CriminalJusticeMapper criminalJusticeMapper;
    private final PageMapper pageMapper;
    private final LocationLookupService locationLookupService;

    @Override
    public CriminalJusticeAreaGetDto findByCode(String code) {
        return auditService.processAudit(
                null,
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDIT_EVENT,
                req -> {
                    var cja = locationLookupService.getCjaOrThrow(code);

                    AuditableResult<CriminalJusticeAreaGetDto, CriminalJusticeArea> result =
                            new AuditableResult<>(
                                    criminalJusticeMapper.toDto(cja),
                                    criminalJusticeMapper.toEntity(code));

                    return Optional.of(result);
                },
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }

    @Override
    public CriminalJusticeAreaPage findAll(
            String code, String description, PagingWrapper pageable) {
        return auditService.processAudit(
                null,
                CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT,
                req -> {
                    org.springframework.data.domain.Page<CriminalJusticeArea> criminalJusticeList =
                            criminalJusticeAreaRepository.search(
                                    code, description, pageable.getPageable());

                    CriminalJusticeAreaPage craPage = new CriminalJusticeAreaPage();
                    pageMapper.toPage(criminalJusticeList, craPage, pageable.getSortStrings());
                    criminalJusticeList.stream()
                            .forEach(
                                    entry ->
                                            craPage.addContentItem(
                                                    criminalJusticeMapper.toDto(entry)));

                    CodeAndDescription codeAndDescription =
                            new CodeAndDescription(code, description);
                    AuditableResult<CriminalJusticeAreaPage, CriminalJusticeArea> result =
                            new AuditableResult<>(
                                    craPage, criminalJusticeMapper.toEntity(codeAndDescription));

                    return Optional.of(result);
                },
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }

    @Override
    @Transactional
    public void upsertCJA(CriminalJusticeArea cja) {
        var cjaDB = criminalJusticeAreaRepository.findByCode(cja.getCode()).stream().findFirst();

        if (cjaDB.isEmpty()) {
            auditService.processAudit(
                    ResultCodeOperation.CREATE_RESULT_CODE_AUDIT_EVENT,
                    req -> {
                        criminalJusticeAreaRepository.saveAndFlush(cja);
                        AuditableResult<Void, CriminalJusticeArea> auditableResult =
                                new AuditableResult<>(null, cja);
                        return Optional.of(auditableResult);
                    },
                    auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));

        } else {
            var currentCja = cjaDB.get();
            var updatedCja = BeanUtil.copyBean(currentCja);
            criminalJusticeMapper.updateCJA(updatedCja, cja);

            auditService.processAudit(
                    currentCja,
                    ResultCodeOperation.UPDATE_RESULT_CODE_AUDIT_EVENT,
                    req -> {
                        AuditableResult<Void, CriminalJusticeArea> auditableResult =
                                new AuditableResult<>(null, updatedCja);
                        return Optional.of(auditableResult);
                    },
                    auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
            criminalJusticeAreaRepository.saveAndFlush(updatedCja);
        }
    }
}
