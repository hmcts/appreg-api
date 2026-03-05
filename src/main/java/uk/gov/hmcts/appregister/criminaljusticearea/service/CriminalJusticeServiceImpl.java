package uk.gov.hmcts.appregister.criminaljusticearea.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.service.LocationLookupService;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.criminaljusticearea.audit.CriminalJusticeAuditOperation;
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
                (req) -> {
                    org.springframework.data.domain.Page<CriminalJusticeArea> criminalJusticeList =
                            criminalJusticeAreaRepository.search(
                                    code, description, pageable.getPageable());

                    CriminalJusticeAreaPage criminalJusticeAreaPage = new CriminalJusticeAreaPage();
                    pageMapper.toPage(
                            criminalJusticeList,
                            criminalJusticeAreaPage,
                            pageable.getSortStrings());
                    criminalJusticeList.stream()
                            .forEach(
                                    (entry) ->
                                            criminalJusticeAreaPage.addContentItem(
                                                    criminalJusticeMapper.toDto(entry)));

                    AuditableResult<CriminalJusticeAreaPage, CriminalJusticeArea> result;
                    if (code == null && description == null) {
                        result =
                                new AuditableResult<>(
                                        criminalJusticeAreaPage,
                                        criminalJusticeMapper.toEntity("", ""));
                    } else {
                        result =
                                new AuditableResult<>(
                                        criminalJusticeAreaPage,
                                        criminalJusticeMapper.toEntity(code, description));
                    }

                    return Optional.of(result);
                },
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }
}
