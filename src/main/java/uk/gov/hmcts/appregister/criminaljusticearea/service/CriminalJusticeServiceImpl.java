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
        CriminalJusticeAreaGetDto criminalJusticeAreaGetDto =
                auditService.processAudit(
                        CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDIT_EVENT,
                        req -> {
                            var cja = locationLookupService.getCjaOrThrow(code);

                            AuditableResult<CriminalJusticeAreaGetDto, CriminalJusticeArea> result =
                                    new AuditableResult<>(criminalJusticeMapper.toDto(cja), null);

                            return Optional.of(result);
                        },
                        auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
        return criminalJusticeAreaGetDto;
    }

    @Override
    public CriminalJusticeAreaPage findAll(
            String code, String description, PagingWrapper pageable) {
        CriminalJusticeAreaPage criminalJusticeAreaPage =
                auditService.processAudit(
                        CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDITS_EVENT,
                        (req) -> {
                            org.springframework.data.domain.Page<CriminalJusticeArea>
                                    criminalJusticeList =
                                            criminalJusticeAreaRepository.search(
                                                    code, description, pageable.getPageable());

                            CriminalJusticeAreaPage craPage = new CriminalJusticeAreaPage();
                            pageMapper.toPage(
                                    criminalJusticeList, craPage, pageable.getSortStrings());
                            criminalJusticeList.stream()
                                    .forEach(
                                            (entry) ->
                                                    craPage.addContentItem(
                                                            criminalJusticeMapper.toDto(entry)));

                            AuditableResult<CriminalJusticeAreaPage, CriminalJusticeArea> result =
                                    new AuditableResult<>(craPage, null);

                            return Optional.of(result);
                        },
                        auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
        return criminalJusticeAreaPage;
    }
}
