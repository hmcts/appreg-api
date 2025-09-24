package uk.gov.hmcts.appregister.criminaljusticearea.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.audit.AuditEventEnum;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.criminaljusticearea.exception.CriminalJusticeAreaError;
import uk.gov.hmcts.appregister.criminaljusticearea.mapper.CriminalJusticeMapper;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaPage;

@Component
@RequiredArgsConstructor
@Slf4j
public class CriminalJusticeServiceImpl implements CriminalJusticeService {
    private final AuditOperationService auditService;
    private final List<AuditOperationLifecycleListener> auditLifecycleListeners;
    private final CriminalJusticeAreaRepository criminalJusticeAreaRepository;
    private final CriminalJusticeMapper criminalJusticeMapper;
    private final PageMapper pageMapper;

    @Override
    public CriminalJusticeAreaGetDto findByCode(String code) {
        return auditService.processAudit(
                AuditEventEnum.GET_CRIMINAL_JUSTICE_AUDIT_EVENT,
                req -> {
                    final List<CriminalJusticeArea> criminalJusticeAreaList =
                            criminalJusticeAreaRepository.findByCode(code);

                    CriminalJusticeAreaGetDto codeToConsider = null;

                    // if empty throw an exception
                    if (criminalJusticeAreaList.isEmpty()) {
                        throw new AppRegistryException(
                                CriminalJusticeAreaError.CODE_NOT_FOUND,
                                " No code found for code %s".formatted(code));
                    } else {
                        if (criminalJusticeAreaList.size() > 1) {
                            log.warn(
                                    "Too many records found for code %s. Defaulting to first one"
                                            .formatted(code));
                        }
                        codeToConsider =
                                criminalJusticeMapper.toDto(
                                        criminalJusticeAreaList.stream().findFirst().get());
                    }
                    return Optional.of(codeToConsider);
                },
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }

    @Override
    public CriminalJusticeAreaPage findAll(String code, String description, Pageable pageable) {
        return auditService.processAudit(
                AuditEventEnum.GET_CRIMINAL_JUSTICE_AUDITS_EVENT,
                (req) -> {
                    org.springframework.data.domain.Page<CriminalJusticeArea> criminalJusticeList =
                            criminalJusticeAreaRepository.search(code, description, pageable);

                    CriminalJusticeAreaPage criminalJusticeAreaPage = new CriminalJusticeAreaPage();
                    pageMapper.toPage(criminalJusticeList, criminalJusticeAreaPage);
                    criminalJusticeList.stream()
                            .forEach(
                                    (entry) -> {
                                        criminalJusticeAreaPage.addContentItem(
                                                criminalJusticeMapper.toDto(entry));
                                    });

                    return Optional.of(criminalJusticeAreaPage);
                },
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }
}
