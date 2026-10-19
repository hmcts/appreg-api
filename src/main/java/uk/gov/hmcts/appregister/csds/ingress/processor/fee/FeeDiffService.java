package uk.gov.hmcts.appregister.csds.ingress.processor.fee;

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
import uk.gov.hmcts.appregister.csds.ingress.database.FeeIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressTableReadService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeDiffService implements IngressDiffService<FeeDiffRequest, FeeDiffResult> {
    private static final String INSERT_REASON_NO_EXISTING_MATCH = "new CSDS fee version";
    private static final String UPDATE_REASON_EXISTING_MATCH = "enrich legacy fee with CSDS ID";

    private final JdbcIngressTableReadService tableReadService;
    private final FeeIngressDatabaseRowMapper rowMapper;

    @Override
    public FeeDiffResult diff(FeeDiffRequest request) {
        val incomingById = new LinkedHashMap<Long, FeeIngressRecord>();
        request.processedData().stream()
                .flatMap(page -> request.recordsExtractor().apply(page).stream())
                .map(request.recordMapper())
                .forEach(item -> addIncomingRecord(request.targetTable(), incomingById, item));

        log.info("Loading existing CSDS comparison rows from {}", request.targetTable());
        val existingById =
                tableReadService.loadAll(request.targetTable(), rowMapper).stream()
                        .collect(
                                Collectors.toMap(
                                        FeeIngressRecord::id,
                                        item -> item,
                                        (first, second) -> second,
                                        LinkedHashMap::new));

        val diffRecords =
                new ArrayList<
                        IngressDiffRecord<FeeIngressRecord, FeeIngressRecord, FeeIngressRecord>>();
        val keys = new HashSet<FeeIngressRecord.BusinessKey>();
        val knownVersions =
                existingById.values().stream()
                        .filter(item -> item.csdsId() != null)
                        .map(item -> Map.entry(item.csdsId(), item.businessKey()))
                        .collect(Collectors.toSet());
        val plannedIds = new HashSet<Long>();
        for (val incoming : incomingById.values()) {
            if (!keys.add(incoming.businessKey())) {
                throw new AppRegistryException(
                        CommonAppError.INTERNAL_SERVER_ERROR,
                        "Duplicate incoming fee business key " + incoming.businessKey());
            }
            if (knownVersions.contains(Map.entry(incoming.csdsId(), incoming.businessKey()))) {
                continue;
            }
            // Earlier imports also allocated offset IDs without recording the CSDS identity.
            val legacy =
                    existingById.get(
                            incoming.id() != null
                                    ? incoming.id()
                                    : FeeIngressRecord.calculateId(null, incoming.csdsId()));
            val enrich =
                    legacy != null
                            && legacy.csdsId() == null
                            && legacy.businessKey().equals(incoming.businessKey());
            val intended =
                    incoming.withId(
                            enrich
                                    ? legacy.id()
                                    : FeeIngressRecord.calculateId(null, incoming.csdsId()));
            if ((!enrich && existingById.containsKey(intended.id()))
                    || !plannedIds.add(intended.id())) {
                throw new AppRegistryException(
                        CommonAppError.INTERNAL_SERVER_ERROR,
                        "FEE_ID %s conflicts for incoming fee %s"
                                .formatted(intended.id(), incoming.businessKey()));
            }
            diffRecords.add(determineDiffRecord(enrich ? legacy : null, incoming, intended));
        }

        return new FeeDiffResult(incomingById, existingById, List.copyOf(diffRecords));
    }

    private void addIncomingRecord(
            String targetTable,
            LinkedHashMap<Long, FeeIngressRecord> incomingById,
            FeeIngressRecord item) {
        val existing = incomingById.putIfAbsent(item.csdsId(), item);
        if (existing == null) {
            return;
        }

        log.error(
                "Duplicate incoming CivilFeeID {} detected for {}. Existing record [{}], duplicate record [{}]",
                item.csdsId(),
                targetTable,
                describe(existing),
                describe(item));
        throw new AppRegistryException(
                CommonAppError.INTERNAL_SERVER_ERROR,
                "Duplicate incoming CivilFeeID " + item.csdsId() + " detected for " + targetTable);
    }

    private IngressDiffRecord<FeeIngressRecord, FeeIngressRecord, FeeIngressRecord>
            determineDiffRecord(
                    FeeIngressRecord existing,
                    FeeIngressRecord incoming,
                    FeeIngressRecord intended) {
        if (existing == null) {
            return new IngressDiffRecord<>(
                    IngressOperation.INSERT,
                    incoming,
                    null,
                    intended,
                    INSERT_REASON_NO_EXISTING_MATCH);
        }

        return new IngressDiffRecord<>(
                IngressOperation.UPDATE,
                incoming,
                existing,
                intended,
                UPDATE_REASON_EXISTING_MATCH);
    }

    private String describe(FeeIngressRecord item) {
        return "feeId=%s, civilFeeId=%s, businessKey=%s, version=%s"
                .formatted(item.id(), item.csdsId(), item.businessKey(), item.version());
    }
}
