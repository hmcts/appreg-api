package uk.gov.hmcts.appregister.csds.ingress.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.appregister.audit.service.AuditOperationServiceImpl;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.lock.DistributedJobLock;
import uk.gov.hmcts.appregister.common.lock.DistributedJobLockService;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngestProcessorName;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProperties;
import uk.gov.hmcts.appregister.csds.ingress.exception.CsdsIngestError;
import uk.gov.hmcts.appregister.csds.ingress.processor.IDataIngressProcessor;
import uk.gov.hmcts.appregister.generated.model.CsdsIngestResponse;

@ExtendWith(MockitoExtension.class)
class CsdsIngestServiceTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock private IDataIngressProcessor<CsdsIngestResponse> applicationCodeProcessor;
    @Mock private IDataIngressProcessor<CsdsIngestResponse> feeProcessor;
    @Mock private DistributedJobLockService distributedJobLockService;
    @Mock private UserProvider userProvider;

    private CsdsIngressProperties properties;
    private CsdsIngestService service;

    @BeforeEach
    void setUp() {
        properties = new CsdsIngressProperties();
        properties.setLeaseDuration(Duration.ofMinutes(5));
        service =
                new CsdsIngestService(
                        List.of((IDataIngressProcessor<?>) applicationCodeProcessor, feeProcessor),
                        distributedJobLockService,
                        properties,
                        new AuditOperationServiceImpl(List.of()),
                        userProvider,
                        OBJECT_MAPPER);
    }

    @Test
    void
            given_validTwoMegabyteFileAndAvailableLock_when_ingest_then_processesFileAndReturnsSummary()
                    throws Exception {
        var file = mockFile("{\"responseCode\":1,\"records\":[{\"AC_ID\":1}]}");
        var lock = new DistributedJobLock("CSDS_DATA_INGRESS", "token", Duration.ofMinutes(5));
        var response = new CsdsIngestResponse().inserted(1).updated(3);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JsonNode>> parsedPagesCaptor = ArgumentCaptor.forClass(List.class);

        when(userProvider.getUserId()).thenReturn("tenant:object");
        when(file.getSize()).thenReturn(2L * 1024 * 1024);
        when(applicationCodeProcessor.processorName())
                .thenReturn(CsdsIngestProcessorName.APPLICATION_CODES.getExternalName());
        when(applicationCodeProcessor.enabled()).thenReturn(true);
        when(distributedJobLockService.tryAcquire("CSDS_DATA_INGRESS", Duration.ofMinutes(5)))
                .thenReturn(Optional.of(lock));
        when(distributedJobLockService.release(lock)).thenReturn(true);
        when(applicationCodeProcessor.ingest(anyList())).thenReturn(response);

        var actual =
                service.ingest(CsdsIngestProcessorName.APPLICATION_CODES.getExternalName(), file);

        assertThat(actual).isSameAs(response);
        verify(applicationCodeProcessor).processorName();
        verify(applicationCodeProcessor).ingest(parsedPagesCaptor.capture());
        assertThat(parsedPagesCaptor.getValue()).hasSize(1).first().isInstanceOf(JsonNode.class);
        verify(distributedJobLockService).release(lock);
    }

    @Test
    void given_validFeeFileAndAvailableLock_when_ingest_then_processesFeeFileAndReturnsSummary()
            throws Exception {
        var file = mockFile("{\"responseCode\":1,\"records\":[{\"FEE_ID\":33}]}");
        var lock = new DistributedJobLock("CSDS_DATA_INGRESS", "token", Duration.ofMinutes(5));
        var response = new CsdsIngestResponse().inserted(2).updated(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JsonNode>> parsedPagesCaptor = ArgumentCaptor.forClass(List.class);

        when(userProvider.getUserId()).thenReturn("tenant:object");
        when(applicationCodeProcessor.processorName())
                .thenReturn(CsdsIngestProcessorName.APPLICATION_CODES.getExternalName());
        when(feeProcessor.processorName())
                .thenReturn(CsdsIngestProcessorName.FEE.getExternalName());
        when(feeProcessor.enabled()).thenReturn(true);
        when(distributedJobLockService.tryAcquire("CSDS_DATA_INGRESS", Duration.ofMinutes(5)))
                .thenReturn(Optional.of(lock));
        when(distributedJobLockService.release(lock)).thenReturn(true);
        when(feeProcessor.ingest(anyList())).thenReturn(response);

        var actual = service.ingest(CsdsIngestProcessorName.FEE.getExternalName(), file);

        assertThat(actual).isSameAs(response);
        verify(applicationCodeProcessor).processorName();
        verify(feeProcessor).processorName();
        verify(feeProcessor).ingest(parsedPagesCaptor.capture());
        assertThat(parsedPagesCaptor.getValue()).hasSize(1).first().isInstanceOf(JsonNode.class);
        verify(distributedJobLockService).release(lock);
    }

    @Test
    void given_knownButUnimplementedProcessor_when_ingest_then_throwsNotImplementedError() {
        var file = mock(MultipartFile.class);
        var processorName = CsdsIngestProcessorName.RESOLUTION_CODES.getExternalName();
        when(applicationCodeProcessor.processorName())
                .thenReturn(CsdsIngestProcessorName.APPLICATION_CODES.getExternalName());

        assertThatThrownBy(() -> service.ingest(processorName, file))
                .isInstanceOf(AppRegistryException.class)
                .satisfies(
                        thrown ->
                                assertThat(((AppRegistryException) thrown).getCode())
                                        .isEqualTo(CsdsIngestError.PROCESSOR_NOT_IMPLEMENTED));

        verify(applicationCodeProcessor).processorName();
        verifyNoMoreInteractions(applicationCodeProcessor);
        verifyNoInteractions(distributedJobLockService);
    }

    @Test
    void given_knownButDisabledProcessor_when_ingest_then_throwsDisabledError() {
        var file = mock(MultipartFile.class);
        var processorName = CsdsIngestProcessorName.APPLICATION_CODES.getExternalName();
        when(applicationCodeProcessor.processorName()).thenReturn(processorName);
        when(applicationCodeProcessor.enabled()).thenReturn(false);

        assertThatThrownBy(() -> service.ingest(processorName, file))
                .isInstanceOf(AppRegistryException.class)
                .satisfies(
                        thrown ->
                                assertThat(((AppRegistryException) thrown).getCode())
                                        .isEqualTo(CsdsIngestError.PROCESSOR_DISABLED));

        verify(applicationCodeProcessor).processorName();
        verify(applicationCodeProcessor).enabled();
        verifyNoMoreInteractions(applicationCodeProcessor);
        verifyNoInteractions(distributedJobLockService);
    }

    @Test
    void given_knownButDisabledFeeProcessor_when_ingest_then_throwsDisabledError() {
        var file = mock(MultipartFile.class);
        var processorName = CsdsIngestProcessorName.FEE.getExternalName();
        when(applicationCodeProcessor.processorName())
                .thenReturn(CsdsIngestProcessorName.APPLICATION_CODES.getExternalName());
        when(feeProcessor.processorName()).thenReturn(processorName);
        when(feeProcessor.enabled()).thenReturn(false);

        assertThatThrownBy(() -> service.ingest(processorName, file))
                .isInstanceOf(AppRegistryException.class)
                .satisfies(
                        thrown ->
                                assertThat(((AppRegistryException) thrown).getCode())
                                        .isEqualTo(CsdsIngestError.PROCESSOR_DISABLED));

        verify(applicationCodeProcessor).processorName();
        verify(feeProcessor).processorName();
        verify(feeProcessor).enabled();
        verifyNoMoreInteractions(applicationCodeProcessor, feeProcessor);
        verifyNoInteractions(distributedJobLockService);
    }

    @Test
    void given_unknownProcessor_when_ingest_then_throwsInvalidProcessorError() {
        var file = mock(MultipartFile.class);

        assertThatThrownBy(() -> service.ingest("not-a-processor", file))
                .isInstanceOf(AppRegistryException.class)
                .satisfies(
                        thrown ->
                                assertThat(((AppRegistryException) thrown).getCode())
                                        .isEqualTo(CsdsIngestError.INVALID_PROCESSOR));

        verifyNoInteractions(distributedJobLockService, applicationCodeProcessor);
    }

    @Test
    void given_emptyFile_when_ingest_then_throwsMissingFileError() {
        var file = mock(MultipartFile.class);
        var processorName = CsdsIngestProcessorName.APPLICATION_CODES.getExternalName();
        when(file.isEmpty()).thenReturn(true);
        when(applicationCodeProcessor.processorName()).thenReturn(processorName);
        when(applicationCodeProcessor.enabled()).thenReturn(true);

        assertThatThrownBy(() -> service.ingest(processorName, file))
                .isInstanceOf(AppRegistryException.class)
                .satisfies(
                        thrown ->
                                assertThat(((AppRegistryException) thrown).getCode())
                                        .isEqualTo(CsdsIngestError.FILE_MISSING));

        verify(applicationCodeProcessor).processorName();
        verify(applicationCodeProcessor).enabled();
        verifyNoMoreInteractions(applicationCodeProcessor);
        verifyNoInteractions(distributedJobLockService);
    }

    @Test
    void given_fileWithoutRecordsArray_when_ingest_then_throwsInvalidFormatError()
            throws Exception {
        var file = mockFile("{\"responseCode\":1}");
        var processorName = CsdsIngestProcessorName.APPLICATION_CODES.getExternalName();
        var lock = new DistributedJobLock("CSDS_DATA_INGRESS", "token", Duration.ofMinutes(5));

        when(userProvider.getUserId()).thenReturn("tenant:object");
        when(applicationCodeProcessor.processorName()).thenReturn(processorName);
        when(applicationCodeProcessor.enabled()).thenReturn(true);
        when(distributedJobLockService.tryAcquire("CSDS_DATA_INGRESS", Duration.ofMinutes(5)))
                .thenReturn(Optional.of(lock));
        when(distributedJobLockService.release(lock)).thenReturn(true);

        assertThatThrownBy(() -> service.ingest(processorName, file))
                .isInstanceOf(AppRegistryException.class)
                .satisfies(
                        thrown ->
                                assertThat(((AppRegistryException) thrown).getCode())
                                        .isEqualTo(CsdsIngestError.INVALID_FILE_FORMAT));

        verify(distributedJobLockService).release(lock);
        verify(applicationCodeProcessor).processorName();
        verify(applicationCodeProcessor).enabled();
        verifyNoMoreInteractions(applicationCodeProcessor);
    }

    private MultipartFile mockFile(String content) throws Exception {
        var file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("application_codes.json");
        when(file.getSize()).thenReturn((long) content.length());
        when(file.getInputStream())
                .thenReturn(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        return file;
    }
}
