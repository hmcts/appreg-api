package uk.gov.hmcts.appregister.csds.ingress.processor.fee;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.csds.ingress.database.FeeIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressTableReadService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeeDiffService implements IngressDiffService<FeeDiffRequest, FeeDiffResult> {
    private static final String INSERT_REASON_NO_EXISTING_MATCH = "no existing fee_id match";
    private static final String UPDATE_REASON_EXISTING_MATCH = "existing fee_id match";

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
        for (val incoming : incomingById.values()) {
            val existing = existingById.get(incoming.id());
            diffRecords.add(determineDiffRecord(existing, incoming));
        }

        return new FeeDiffResult(incomingById, existingById, List.copyOf(diffRecords));
    }

    private void addIncomingRecord(
            String targetTable,
            LinkedHashMap<Long, FeeIngressRecord> incomingById,
            FeeIngressRecord item) {
        val existing = incomingById.putIfAbsent(item.id(), item);
        if (existing == null) {
            return;
        }

        log.error(
                "Duplicate incoming FEE_ID {} detected for {}. Existing record [{}], duplicate record [{}]",
                item.id(),
                targetTable,
                describe(existing),
                describe(item));
        throw new AppRegistryException(
                CommonAppError.INTERNAL_SERVER_ERROR,
                "Duplicate incoming FEE_ID " + item.id() + " detected for " + targetTable);
    }

    private IngressDiffRecord<FeeIngressRecord, FeeIngressRecord, FeeIngressRecord>
            determineDiffRecord(FeeIngressRecord existing, FeeIngressRecord incoming) {
        if (existing == null) {
            return new IngressDiffRecord<>(
                    IngressOperation.INSERT,
                    incoming,
                    null,
                    incoming,
                    INSERT_REASON_NO_EXISTING_MATCH);
        }

        return new IngressDiffRecord<>(
                IngressOperation.UPDATE,
                incoming,
                existing,
                incoming,
                UPDATE_REASON_EXISTING_MATCH);
    }

    private String describe(FeeIngressRecord item) {
        return "feeId=%s, reference=%s, startDate=%s, version=%s"
                .formatted(item.id(), item.reference(), item.startDate(), item.version());
    }
}
