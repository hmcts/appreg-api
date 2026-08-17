package uk.gov.hmcts.appregister.criminaljusticearea.service;

import java.util.ArrayList;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.service.LocationLookupService;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.criminaljusticearea.audit.CriminalJusticeAuditOperation;
import uk.gov.hmcts.appregister.criminaljusticearea.mapper.CodeAndDescription;
import uk.gov.hmcts.appregister.criminaljusticearea.mapper.CriminalJusticeMapper;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaPage;

@Service
@RequiredArgsConstructor
@Slf4j
public class CriminalJusticeServiceImpl implements CriminalJusticeService {
    private final AuditOperationService auditService;
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
                });
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
                    craPage.setContent(new ArrayList<>());
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
                });
    }
}
