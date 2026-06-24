package uk.gov.hmcts.appregister.criminaljusticearea.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationSlf4jLogger;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.audit.service.AuditOperationServiceImpl;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.mapper.PageMapper;
import uk.gov.hmcts.appregister.common.service.LocationLookupService;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.criminaljusticearea.audit.CriminalJusticeAuditOperation;
import uk.gov.hmcts.appregister.criminaljusticearea.exception.CriminalJusticeAreaError;
import uk.gov.hmcts.appregister.criminaljusticearea.mapper.CriminalJusticeMapper;
import uk.gov.hmcts.appregister.criminaljusticearea.mapper.CriminalJusticeMapperImpl;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaPage;

@ExtendWith(MockitoExtension.class)
class CriminalJusticeAreaServiceImplTest {
    @Mock private CriminalJusticeAreaRepository repository;

    @Spy
    private List<AuditOperationLifecycleListener> listeners =
            List.of(new AuditOperationSlf4jLogger());

    @Spy
    private AuditOperationService auditOperationService = new AuditOperationServiceImpl(listeners);

    @Spy private CriminalJusticeMapper criminalJusticeMapper = new CriminalJusticeMapperImpl();

    @InjectMocks private CriminalJusticeServiceImpl service;

    @Mock private LocationLookupService locationLookupService;
    @Mock private PageMapper pageMapper;

    @Test
    void testSuccess() {
        // Given
        String code = "X123";
        String description = "Test Area";
        var cja = CriminalJusticeArea.builder().code(code).description(description).build();

        when(locationLookupService.getCjaOrThrow(code)).thenReturn(cja);

        // When
        CriminalJusticeAreaGetDto dto = service.findByCode(code);

        // Then
        Assertions.assertEquals(code, dto.getCode());
        Assertions.assertEquals(description, dto.getDescription());
        verify(auditOperationService)
                .processAudit(
                        isNull(),
                        eq(CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDIT_EVENT),
                        notNull());
    }

    @Test
    void testDuplicate_throwsDomainError() {
        // Given
        String code = "X123";
        var ex =
                new AppRegistryException(
                        CriminalJusticeAreaError.DUPLICATE_CJA_FOUND,
                        "Multiple Criminal Justice Areas found for code '%s'".formatted(code));

        when(locationLookupService.getCjaOrThrow(code)).thenThrow(ex);

        // When / Then
        AppRegistryException thrown =
                Assertions.assertThrows(AppRegistryException.class, () -> service.findByCode(code));

        Assertions.assertEquals(CriminalJusticeAreaError.DUPLICATE_CJA_FOUND, thrown.getCode());
        verify(auditOperationService)
                .processAudit(
                        isNull(),
                        eq(CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDIT_EVENT),
                        notNull());
    }

    @Test
    void testNotFound_throwsDomainError() {
        // Given
        String code = "X123";
        var ex =
                new AppRegistryException(
                        CriminalJusticeAreaError.CJA_NOT_FOUND,
                        "No Criminal Justice Areas found for code '%s'".formatted(code));

        when(locationLookupService.getCjaOrThrow(code)).thenThrow(ex);

        // When / Then
        AppRegistryException thrown =
                Assertions.assertThrows(AppRegistryException.class, () -> service.findByCode(code));

        Assertions.assertEquals(CriminalJusticeAreaError.CJA_NOT_FOUND, thrown.getCode());
        verify(auditOperationService)
                .processAudit(
                        isNull(),
                        eq(CriminalJusticeAuditOperation.GET_CRIMINAL_JUSTICE_AUDIT_EVENT),
                        notNull());
    }

    @Test
    void testSuccess_auditsRequestedLookupCriteria() {
        String code = "X123";
        var cja = CriminalJusticeArea.builder().code(code).description("Test Area").build();
        when(locationLookupService.getCjaOrThrow(code)).thenReturn(cja);

        CapturingAuditListener listener = new CapturingAuditListener();
        CriminalJusticeServiceImpl localService =
                new CriminalJusticeServiceImpl(
                        new AuditOperationServiceImpl(List.of(listener)),
                        repository,
                        criminalJusticeMapper,
                        pageMapper,
                        locationLookupService);

        CriminalJusticeAreaGetDto dto = localService.findByCode(code);

        Assertions.assertEquals(code, dto.getCode());
        Assertions.assertNotNull(listener.getCompleteEvent());
        CriminalJusticeArea audited =
                (CriminalJusticeArea) listener.getCompleteEvent().getNewValue();
        Assertions.assertNotSame(cja, audited);
        Assertions.assertEquals(code, audited.getCode());
        Assertions.assertNull(audited.getDescription());
    }

    @Test
    void findAll_emptyPage_returnsEmptyContentList() {
        var pageable = PageRequest.of(0, 10);
        when(repository.search(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var localService =
                new CriminalJusticeServiceImpl(
                        auditOperationService,
                        repository,
                        criminalJusticeMapper,
                        new PageMapper(),
                        locationLookupService);

        CriminalJusticeAreaPage result =
                localService.findAll(null, null, PagingWrapper.of(List.of(), pageable));

        Assertions.assertNotNull(result.getContent());
        Assertions.assertTrue(result.getContent().isEmpty());
    }

    private static final class CapturingAuditListener implements AuditOperationLifecycleListener {
        private CompleteEvent completeEvent;

        @Override
        public void eventPerformed(BaseAuditEvent event) {
            if (event instanceof CompleteEvent complete) {
                completeEvent = complete;
            }
        }

        private CompleteEvent getCompleteEvent() {
            return completeEvent;
        }
    }
}
