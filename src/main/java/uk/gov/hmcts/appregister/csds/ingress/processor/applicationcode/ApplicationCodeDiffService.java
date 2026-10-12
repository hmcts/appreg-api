package uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.csds.ingress.database.ApplicationCodeIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressTableReadService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;
import uk.gov.hmcts.appregister.csds.ingress.exception.CsdsDuplicateRecordException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationCodeDiffService
        implements IngressDiffService<ApplicationCodeDiffRequest, ApplicationCodeDiffResult> {
    private static final String INSERT_REASON_NO_EXISTING_MATCH =
            "no existing application_code match";
    private static final String UPDATE_REASON_EXISTING_MATCH = "existing application_code match";

    private final JdbcIngressTableReadService tableReadService;
    private final ApplicationCodeIngressDatabaseRowMapper rowMapper;

    @Override
    public ApplicationCodeDiffResult diff(ApplicationCodeDiffRequest request) {
        val incomingByCode = new LinkedHashMap<String, ApplicationCodeIngressRecord>();
        request.processedData().stream()
                .flatMap(page -> request.recordsExtractor().apply(page).stream())
                .map(request.recordMapper())
                .forEach(item -> addIncomingRecord(request.targetTable(), incomingByCode, item));

        log.info("Loading existing CSDS comparison rows from {}", request.targetTable());
        val existingById =
                tableReadService.loadAll(request.targetTable(), rowMapper).stream()
                        .collect(
                                Collectors.toMap(
                                        ApplicationCodeIngressRecord::id,
                                        item -> item,
                                        (first, second) -> second,
                                        LinkedHashMap::new));

        val diffRecords =
                new ArrayList<
                        IngressDiffRecord<
                                ApplicationCodeIngressRecord,
                                ApplicationCodeIngressRecord,
                                ApplicationCodeIngressRecord>>();
        val existingByCode =
                existingById.values().stream()
                        .collect(
                                Collectors.toMap(ApplicationCodeIngressRecord::code, item -> item));
        val incomingById = new LinkedHashMap<Long, ApplicationCodeIngressRecord>();
        for (val incoming : incomingByCode.values()) {
            val existing = existingByCode.get(incoming.code());
            if (existing == null && incoming.id() == null) {
                throw new AppRegistryException(
                        CommonAppError.INTERNAL_SERVER_ERROR,
                        "Missing ApplicationCodeID for new application code " + incoming.code());
            }
            val usesGeneratedLegacyId = existing == null && incoming.pssId() == null;
            val intendedId =
                    existing == null
                            ? ApplicationCodeIngressRecord.calculateId(
                                    incoming.pssId(), incoming.id())
                            : existing.id();
            if (intendedId == null) {
                throw new AppRegistryException(
                        CommonAppError.INTERNAL_SERVER_ERROR,
                        "Missing source ID for new application code " + incoming.code());
            }
            val intended = incoming.withId(intendedId);
            val idOwner = existingById.get(intendedId);
            val incomingOwner = incomingById.putIfAbsent(intendedId, intended);
            if (usesGeneratedLegacyId
                    && ((idOwner != null && !idOwner.code().equals(incoming.code()))
                            || incomingOwner != null)) {
                log.error(
                        "Generated legacy AC_ID {} conflicts for incoming application code [{}]",
                        intendedId,
                        describe(intended));
                throw new CsdsDuplicateRecordException(
                        "Generated legacy AC_ID "
                                + intendedId
                                + " conflicts for incoming application code ["
                                + describe(intended)
                                + "]");
            }
            diffRecords.add(determineDiffRecord(existing, intended));
        }

        return new ApplicationCodeDiffResult(incomingById, existingById, List.copyOf(diffRecords));
    }

    private void addIncomingRecord(
            String targetTable,
            LinkedHashMap<String, ApplicationCodeIngressRecord> incomingByCode,
            ApplicationCodeIngressRecord item) {
        val existing = incomingByCode.putIfAbsent(item.code(), item);
        if (existing == null) {
            return;
        }

        log.error(
                "Duplicate incoming application_code {} detected for {}. Existing record [{}], duplicate record [{}]",
                item.code(),
                targetTable,
                describe(existing),
                describe(item));
        throw new CsdsDuplicateRecordException(
                "Duplicate incoming application_code "
                        + item.code()
                        + " detected for "
                        + targetTable);
    }

    private IngressDiffRecord<
                    ApplicationCodeIngressRecord,
                    ApplicationCodeIngressRecord,
                    ApplicationCodeIngressRecord>
            determineDiffRecord(
                    ApplicationCodeIngressRecord existing, ApplicationCodeIngressRecord incoming) {
        if (existing == null) {
            return new IngressDiffRecord<>(
                    IngressOperation.INSERT,
                    incoming,
                    null,
                    buildIntendedRecord(null, incoming),
                    INSERT_REASON_NO_EXISTING_MATCH);
        }

        return new IngressDiffRecord<>(
                IngressOperation.UPDATE,
                incoming,
                existing,
                buildIntendedRecord(existing, incoming),
                UPDATE_REASON_EXISTING_MATCH);
    }

    @SuppressWarnings("java:S1172")
    private ApplicationCodeIngressRecord buildIntendedRecord(
            ApplicationCodeIngressRecord existing, ApplicationCodeIngressRecord incoming) {
        // ApplicationCode currently upserts directly from the incoming CSDS representation.
        return incoming;
    }

    private String describe(ApplicationCodeIngressRecord item) {
        return "acId=%s, code=%s, title=%s, startDate=%s, version=%s"
                .formatted(item.id(), item.code(), item.title(), item.startDate(), item.version());
    }
}
