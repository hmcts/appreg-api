package uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.csds.ingress.database.ApplicationCodeIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressTableReadService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationCodeDiffService
        implements IngressDiffService<ApplicationCodeDiffRequest, ApplicationCodeDiffResult> {
    private static final String INSERT_REASON_NO_EXISTING_MATCH = "no existing ac_id match";
    private static final String UPDATE_REASON_EXISTING_MATCH = "existing ac_id match";

    private final JdbcIngressTableReadService tableReadService;
    private final ApplicationCodeIngressDatabaseRowMapper rowMapper;

    @Override
    public ApplicationCodeDiffResult diff(ApplicationCodeDiffRequest request) {
        val incomingById = new LinkedHashMap<Long, ApplicationCodeIngressRecord>();
        request.processedData().stream()
                .flatMap(page -> request.recordsExtractor().apply(page).stream())
                .map(request.recordMapper())
                .forEach(item -> addIncomingRecord(request.targetTable(), incomingById, item));

        log.info("Loading existing CSDS comparison rows from {}", request.targetTable());
        val existingById =
                tableReadService.loadAll(request.targetTable(), rowMapper).stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
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
        for (val incoming : incomingById.values()) {
            val existing = existingById.get(incoming.id());
            diffRecords.add(determineDiffRecord(existing, incoming));
        }

        return new ApplicationCodeDiffResult(incomingById, existingById, List.copyOf(diffRecords));
    }

    private void addIncomingRecord(
            String targetTable,
            LinkedHashMap<Long, ApplicationCodeIngressRecord> incomingById,
            ApplicationCodeIngressRecord item) {
        val existing = incomingById.putIfAbsent(item.id(), item);
        if (existing == null) {
            return;
        }

        log.error(
                "Duplicate incoming AC_ID {} detected for {}. Existing record [{}], duplicate record [{}]",
                item.id(),
                targetTable,
                describe(existing),
                describe(item));
        throw new AppRegistryException(
                CommonAppError.INTERNAL_SERVER_ERROR,
                "Duplicate incoming AC_ID " + item.id() + " detected for " + targetTable);
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
