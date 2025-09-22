package uk.gov.hmcts.appregister.criminaljusticearea.mapper;

import static org.mockito.ArgumentMatchers.eq;
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
import uk.gov.hmcts.appregister.audit.AuditEventEnum;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationSlf4jLogger;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.audit.service.AuditOperationServiceImpl;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.repository.CriminalJusticeAreaRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.criminaljusticearea.exception.CriminalJusticeAreaError;
import uk.gov.hmcts.appregister.criminaljusticearea.service.CriminalJusticeServiceImpl;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaDto;

@ExtendWith(MockitoExtension.class)
public class CriminalJusticeAreaServiceImplTest {
    @Mock private CriminalJusticeAreaRepository repository;

    @Spy
    private List<AuditOperationLifecycleListener> listeners =
            List.of(new AuditOperationSlf4jLogger());

    @Spy
    private AuditOperationService auditOperationService =
            new AuditOperationServiceImpl(new ObjectMapper());

    @Spy private CriminalJusticeMapper criminalJusticeMapper = new CriminalJusticeMapperImpl();

    @InjectMocks private CriminalJusticeServiceImpl service;

    @Test
    public void testSuccess() {
        String code = "X123";
        String description = "Test Area";

        when(repository.findByCjaCode(code))
                .thenReturn(
                        List.of(
                                CriminalJusticeArea.builder()
                                        .cjaCode(code)
                                        .cjaDescription("Test Area")
                                        .build()));

        CriminalJusticeAreaDto criminalJusticeAreaDto = service.findByCode(code);

        Assertions.assertEquals(code, criminalJusticeAreaDto.getCode());
        Assertions.assertEquals(description, criminalJusticeAreaDto.getDescription());

        verify(auditOperationService)
                .processAudit(
                        eq(AuditEventEnum.GET_CRIMINAL_JUSTICE_AUDIT_EVENT), notNull(), notNull());
    }

    @Test
    public void testSuccessOnDuplicate() {
        String code = "X123";
        String description = "Test Area";

        when(repository.findByCjaCode(code))
                .thenReturn(
                        List.of(
                                CriminalJusticeArea.builder()
                                        .cjaCode(code)
                                        .cjaDescription("Test Area")
                                        .build(),
                                CriminalJusticeArea.builder()
                                        .cjaCode(code)
                                        .cjaDescription("Test Area")
                                        .build()));

        CriminalJusticeAreaDto criminalJusticeAreaDto = service.findByCode(code);

        Assertions.assertEquals(code, criminalJusticeAreaDto.getCode());
        Assertions.assertEquals(description, criminalJusticeAreaDto.getDescription());
        verify(auditOperationService)
                .processAudit(
                        eq(AuditEventEnum.GET_CRIMINAL_JUSTICE_AUDIT_EVENT), notNull(), notNull());
    }

    @Test
    public void testInvalid() {
        String code = "X123";

        when(repository.findByCjaCode(code)).thenReturn(List.of());

        AppRegistryException exception =
                Assertions.assertThrows(AppRegistryException.class, () -> service.findByCode(code));
        Assertions.assertEquals(CriminalJusticeAreaError.CODE_NOT_FOUND, exception.getCode());
        verify(auditOperationService)
                .processAudit(
                        eq(AuditEventEnum.GET_CRIMINAL_JUSTICE_AUDIT_EVENT), notNull(), notNull());
    }
}
