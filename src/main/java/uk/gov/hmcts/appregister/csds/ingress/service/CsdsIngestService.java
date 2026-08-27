package uk.gov.hmcts.appregister.csds.ingress.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.lock.DistributedJobLock;
import uk.gov.hmcts.appregister.common.lock.DistributedJobLockService;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngestProcessorName;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProcessor;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProperties;
import uk.gov.hmcts.appregister.csds.ingress.audit.CsdsIngestAudit;
import uk.gov.hmcts.appregister.csds.ingress.audit.CsdsIngestAuditOperation;
import uk.gov.hmcts.appregister.csds.ingress.exception.CsdsIngestError;
import uk.gov.hmcts.appregister.csds.ingress.exception.CsdsPayloadValidationException;
import uk.gov.hmcts.appregister.csds.ingress.processor.IDataIngressProcessor;
import uk.gov.hmcts.appregister.generated.model.CsdsIngestResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsdsIngestService {
    private final List<IDataIngressProcessor<?>> processors;
    private final DistributedJobLockService distributedJobLockService;
    private final CsdsIngressProperties csdsIngressProperties;
    private final AuditOperationService auditOperationService;
    private final UserProvider userProvider;
    private final ObjectMapper objectMapper;

    public CsdsIngestResponse ingest(String processorName, MultipartFile file) {
        var processorType = CsdsIngestProcessorName.fromExternalName(processorName);
        var processor = requireProcessor(processorType);
        validateFile(file);
        var audit = buildAudit(processorType.getExternalName(), file);

        try {
            return auditOperationService.processAudit(
                    CsdsIngestAuditOperation.MANUAL_CSDS_INGEST_AUDIT_EVENT,
                    unused -> {
                        var lock = acquireLock();
                        try {
                            var response = processor.ingest(parse(file));
                            return Optional.of(new AuditableResult<>(response, audit));
                        } finally {
                            if (!distributedJobLockService.release(lock)) {
                                log.warn(
                                        """
                                        Distributed lock release was skipped for job {}
                                         because the lease is no longer owned
                                        """,
                                        CsdsIngressProcessor.DATABASE_JOB_NAME);
                            }
                        }
                    });
        } catch (CsdsPayloadValidationException ex) {
            throw new AppRegistryException(
                    CsdsIngestError.INVALID_RECORD_DATA, ex.getMessage(), ex);
        }
    }

    private IDataIngressProcessor<?> requireProcessor(CsdsIngestProcessorName processorType) {
        var processor =
                processors.stream()
                        .filter(
                                candidate ->
                                        processorType
                                                .getExternalName()
                                                .equals(candidate.processorName()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new AppRegistryException(
                                                CsdsIngestError.PROCESSOR_NOT_IMPLEMENTED,
                                                "The CSDS ingest processor "
                                                        + processorType.getExternalName()
                                                        + " is not implemented yet"));

        if (!processor.enabled()) {
            throw new AppRegistryException(
                    CsdsIngestError.PROCESSOR_DISABLED,
                    "The CSDS ingest processor "
                            + processorType.getExternalName()
                            + " is disabled");
        }

        return processor;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppRegistryException(
                    CsdsIngestError.FILE_MISSING,
                    "CSDS ingest file must be provided and not empty");
        }
    }

    private DistributedJobLock acquireLock() {
        return distributedJobLockService
                .tryAcquire(
                        CsdsIngressProcessor.DATABASE_JOB_NAME,
                        csdsIngressProperties.getLeaseDuration())
                .orElseThrow(
                        () ->
                                new AppRegistryException(
                                        CsdsIngestError.LOCKED,
                                        "The CSDS ingest is already running"));
    }

    private List<JsonNode> parse(MultipartFile file) {
        try (var inputStream = file.getInputStream()) {
            var uploadedJson = objectMapper.readTree(inputStream);
            if (uploadedJson == null
                    || !uploadedJson.isObject()
                    || uploadedJson.get("records") == null
                    || !uploadedJson.get("records").isArray()) {
                throw new AppRegistryException(
                        CsdsIngestError.INVALID_FILE_FORMAT,
                        "CSDS ingest file must contain a JSON object with a records array");
            }
            return List.of(uploadedJson);
        } catch (IOException ex) {
            throw new AppRegistryException(
                    CsdsIngestError.INVALID_FILE_FORMAT,
                    "CSDS ingest file must contain a JSON object with a records array",
                    ex);
        }
    }

    private CsdsIngestAudit buildAudit(String processorName, MultipartFile file) {
        return CsdsIngestAudit.builder()
                .requestingUser(userProvider.getUserId())
                .processorName(processorName)
                .fileName(
                        StringUtils.hasText(file.getOriginalFilename())
                                ? file.getOriginalFilename()
                                : "unknown")
                .fileSize(file.getSize())
                .build();
    }
}
