package uk.gov.hmcts.appregister.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.audit.event.AuditEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.service.AuditOperationServiceImpl;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetSummaryDto;

@ExtendWith(MockitoExtension.class)
class AuditOperationServiceImplTest {

    private AuditOperationServiceImpl auditOperationServiceImpl;

    @Captor ArgumentCaptor<AuditEvent> requestArgumentCaptor;

    @Captor ArgumentCaptor<AuditEvent> successCapture;

    @Captor ArgumentCaptor<AuditEvent> failCapture;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        auditOperationServiceImpl = new AuditOperationServiceImpl(objectMapper);
    }

    @Test
    void testAuditOperationFlowWithResponsePayload() throws Exception {
        ApplicationCodeGetSummaryDto applicationCodeDto = new ApplicationCodeGetSummaryDto();

        AuditOperationLifecycleListener listener =
                Mockito.mock(AuditOperationLifecycleListener.class);
        auditOperationServiceImpl.processAudit(
                AuditEventEnum.GET_APPLICATION_CODE_AUDIT_EVENT,
                (req) -> {
                    // Simulate some processing and return a response
                    return Optional.of(applicationCodeDto);
                },
                listener);

        Mockito.verify(listener, Mockito.times(2)).eventPerformed(requestArgumentCaptor.capture());
        Assertions.assertEquals(2, requestArgumentCaptor.getAllValues().size());
        AuditEvent beforeOp = requestArgumentCaptor.getAllValues().get(0);
        AuditEvent completedOp = requestArgumentCaptor.getAllValues().get(1);

        Assertions.assertNotNull(beforeOp);
        Assertions.assertEquals("Get Application Code", beforeOp.getRequestAction().getEventName());
        Assertions.assertEquals("No Correlation Id Found", beforeOp.getMessageUuid());
        Assertions.assertEquals(1, beforeOp.getMessageStatus().getStatus());
        Assertions.assertEquals("NULL", beforeOp.getMessageContent());

        Assertions.assertNotNull(completedOp);
        Assertions.assertEquals(
                "Get Application Code", completedOp.getRequestAction().getEventName());
        Assertions.assertEquals("No Correlation Id Found", completedOp.getMessageUuid());
        Assertions.assertEquals(10, completedOp.getMessageStatus().getStatus());
        Assertions.assertEquals(
                objectMapper.writeValueAsString(applicationCodeDto),
                completedOp.getMessageContent());
    }

    @Test
    void testAuditOperationFlowWithResponseWithoutPayload() throws Exception {
        AuditOperationLifecycleListener listener =
                Mockito.mock(AuditOperationLifecycleListener.class);
        auditOperationServiceImpl.processAudit(
                AuditEventEnum.GET_APPLICATION_CODE_AUDIT_EVENT,
                (req) -> {
                    // Simulate some processing and return a response
                    return Optional.empty();
                },
                listener);

        Mockito.verify(listener, Mockito.times(2)).eventPerformed(requestArgumentCaptor.capture());
        Assertions.assertEquals(2, requestArgumentCaptor.getAllValues().size());
        AuditEvent beforeOp = requestArgumentCaptor.getAllValues().get(0);
        AuditEvent completedOp = requestArgumentCaptor.getAllValues().get(1);

        Assertions.assertNotNull(beforeOp);
        Assertions.assertEquals("Get Application Code", beforeOp.getRequestAction().getEventName());
        Assertions.assertEquals("No Correlation Id Found", beforeOp.getMessageUuid());
        Assertions.assertEquals(1, beforeOp.getMessageStatus().getStatus());
        Assertions.assertEquals("NULL", beforeOp.getMessageContent());

        Assertions.assertNotNull(completedOp);
        Assertions.assertEquals(
                "Get Application Code", completedOp.getRequestAction().getEventName());
        Assertions.assertEquals("No Correlation Id Found", completedOp.getMessageUuid());
        Assertions.assertEquals(10, completedOp.getMessageStatus().getStatus());
        Assertions.assertEquals("NULL", completedOp.getMessageContent());

        Assertions.assertEquals(
                "Get Application Code", completedOp.getRequestAction().getEventName());
        Assertions.assertEquals("No Correlation Id Found", completedOp.getMessageUuid());
        Assertions.assertEquals(10, completedOp.getMessageStatus().getStatus());
        Assertions.assertEquals("NULL", completedOp.getMessageContent());
    }

    @Test
    void testAuditOperationFlowWithResponseWithPayloadFailure() throws Exception {
        AuditOperationLifecycleListener listener =
                Mockito.mock(AuditOperationLifecycleListener.class);

        RuntimeException ex =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                auditOperationServiceImpl.processAudit(
                                        AuditEventEnum.GET_APPLICATION_CODE_AUDIT_EVENT,
                                        (req) -> {
                                            // Simulate some processing and return a response
                                            throw new IllegalArgumentException("");
                                        },
                                        listener));

        Mockito.verify(listener, Mockito.times(2)).eventPerformed(requestArgumentCaptor.capture());
        Assertions.assertEquals(2, requestArgumentCaptor.getAllValues().size());
        AuditEvent beforeOp = requestArgumentCaptor.getAllValues().get(0);
        AuditEvent failOp = requestArgumentCaptor.getAllValues().get(1);

        Assertions.assertNotNull(beforeOp);
        Assertions.assertEquals("Get Application Code", beforeOp.getRequestAction().getEventName());
        Assertions.assertEquals("No Correlation Id Found", beforeOp.getMessageUuid());
        Assertions.assertEquals(1, beforeOp.getMessageStatus().getStatus());
        Assertions.assertEquals("NULL", beforeOp.getMessageContent());

        Assertions.assertNotNull(failOp);
        Assertions.assertEquals("Get Application Code", failOp.getRequestAction().getEventName());
        Assertions.assertEquals("No Correlation Id Found", failOp.getMessageUuid());
        Assertions.assertEquals(-1, failOp.getMessageStatus().getStatus());
        Assertions.assertEquals("NULL", failOp.getMessageContent());

        Assertions.assertNotNull(failOp);
        Assertions.assertEquals("Get Application Code", failOp.getRequestAction().getEventName());
        Assertions.assertEquals("No Correlation Id Found", failOp.getMessageUuid());
        Assertions.assertEquals(-1, failOp.getMessageStatus().getStatus());
        Assertions.assertEquals("NULL", failOp.getMessageContent());
    }
}
