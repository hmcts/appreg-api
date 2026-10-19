package uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationCodeDiffService
        implements IngressDiffService<ApplicationCodeDiffRequest, ApplicationCodeDiffResult> {
    private static final String INSERT_REASON_NO_EXISTING_MATCH = "new CSDS version";
    private static final String UPDATE_REASON_EXISTING_MATCH =
            "first CSDS receipt of legacy record";

    private final JdbcIngressTableReadService tableReadService;
    private final ApplicationCodeIngressDatabaseRowMapper rowMapper;

    @Override
    public ApplicationCodeDiffResult diff(ApplicationCodeDiffRequest request) {
        val incomingById = new LinkedHashMap<Long, ApplicationCodeIngressRecord>();
        val incomingCodes = new HashSet<String>();
        request.processedData().stream()
                .flatMap(page -> request.recordsExtractor().apply(page).stream())
                .map(request.recordMapper())
                .forEach(
                        item -> {
                            if (!incomingCodes.add(item.code())) {
                                throw new AppRegistryException(
                                        CommonAppError.INTERNAL_SERVER_ERROR,
                                        "Duplicate incoming Code "
                                                + item.code()
                                                + " detected for "
                                                + request.targetTable());
                            }
                            addIncomingRecord(request.targetTable(), incomingById, item);
                        });

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
        val occupiedIds = new HashSet<>(existingById.keySet());
        val receivedVersions = new HashSet<Map.Entry<Long, String>>();
        existingById.values().stream()
                .filter(item -> item.csdsId() != null)
                .forEach(item -> receivedVersions.add(Map.entry(item.csdsId(), item.code())));
        for (val incoming : incomingById.values()) {
            if (receivedVersions.contains(Map.entry(incoming.csdsId(), incoming.code()))) {
                continue;
            }
            val existing = existingById.get(incoming.id());
            val legacyMatch =
                    existing != null
                            && existing.csdsId() == null
                            && existing.code().equals(incoming.code());
            val intended =
                    incoming.withId(
                            legacyMatch
                                    ? existing.id()
                                    : ApplicationCodeIngressRecord.calculateId(
                                            null, incoming.csdsId()));
            if (!legacyMatch && !occupiedIds.add(intended.id())) {
                throw new AppRegistryException(
                        CommonAppError.INTERNAL_SERVER_ERROR,
                        "Generated AC_ID "
                                + intended.id()
                                + " collides with an existing or planned record");
            }
            diffRecords.add(
                    new IngressDiffRecord<>(
                            legacyMatch ? IngressOperation.UPDATE : IngressOperation.INSERT,
                            incoming,
                            legacyMatch ? existing : null,
                            intended,
                            legacyMatch
                                    ? UPDATE_REASON_EXISTING_MATCH
                                    : INSERT_REASON_NO_EXISTING_MATCH));
        }

        return new ApplicationCodeDiffResult(incomingById, existingById, List.copyOf(diffRecords));
    }

    private void addIncomingRecord(
            String targetTable,
            LinkedHashMap<Long, ApplicationCodeIngressRecord> incomingById,
            ApplicationCodeIngressRecord item) {
        val existing = incomingById.putIfAbsent(item.csdsId(), item);
        if (existing == null) {
            return;
        }

        log.error(
                "Duplicate incoming CSDS_ID {} detected for {}. Existing record [{}], duplicate record [{}]",
                item.csdsId(),
                targetTable,
                describe(existing),
                describe(item));
        throw new AppRegistryException(
                CommonAppError.INTERNAL_SERVER_ERROR,
                "Duplicate incoming CSDS_ID " + item.csdsId() + " detected for " + targetTable);
    }

    private String describe(ApplicationCodeIngressRecord item) {
        return "acId=%s, code=%s, title=%s, startDate=%s, version=%s"
                .formatted(item.id(), item.code(), item.title(), item.startDate(), item.version());
    }
}
