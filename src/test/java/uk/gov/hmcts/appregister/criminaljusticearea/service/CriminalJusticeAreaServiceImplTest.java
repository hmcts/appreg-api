package uk.gov.hmcts.appregister.criminaljusticearea.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
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
import uk.gov.hmcts.appregister.criminaljusticearea.audit.CriminalJusticeAuditOperation;
import uk.gov.hmcts.appregister.criminaljusticearea.exception.CriminalJusticeAreaError;
import uk.gov.hmcts.appregister.criminaljusticearea.mapper.CriminalJusticeMapper;
import uk.gov.hmcts.appregister.criminaljusticearea.mapper.CriminalJusticeMapperImpl;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;

@ExtendWith(MockitoExtension.class)
class CriminalJusticeAreaServiceImplTest {
    @Mock private CriminalJusticeAreaRepository repository;

    @Spy
    private List<AuditOperationLifecycleListener> listeners =
            List.of(new AuditOperationSlf4jLogger());

    @Spy
    private AuditOperationService auditOperationService =
            new AuditOperationServiceImpl(new ObjectMapper(), listeners);

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
                        notNull(),
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
                        notNull(),
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
                        notNull(),
                        notNull());
    }

    @Test
    void testSuccess_auditsResolvedEntity() {
        String code = "X123";
        var cja = CriminalJusticeArea.builder().code(code).description("Test Area").build();
        when(locationLookupService.getCjaOrThrow(code)).thenReturn(cja);

        CapturingAuditListener listener = new CapturingAuditListener();
        CriminalJusticeServiceImpl localService =
                new CriminalJusticeServiceImpl(
                        new AuditOperationServiceImpl(new ObjectMapper(), List.of(listener)),
                        List.of(listener),
                        repository,
                        criminalJusticeMapper,
                        pageMapper,
                        locationLookupService);

        CriminalJusticeAreaGetDto dto = localService.findByCode(code);

        Assertions.assertEquals(code, dto.getCode());
        Assertions.assertNotNull(listener.getCompleteEvent());
        Assertions.assertSame(cja, listener.getCompleteEvent().getNewValue());
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
