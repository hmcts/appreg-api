package uk.gov.hmcts.appregister.csds.ingress.processor.fee;

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
import uk.gov.hmcts.appregister.csds.ingress.database.FeeIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressTableReadService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;
import uk.gov.hmcts.appregister.csds.ingress.exception.CsdsDuplicateRecordException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeDiffService implements IngressDiffService<FeeDiffRequest, FeeDiffResult> {
    private static final String INSERT_REASON_NO_EXISTING_MATCH =
            "no existing fee business-key match";
    private static final String UPDATE_REASON_EXISTING_MATCH = "existing fee business-key match";

    private final JdbcIngressTableReadService tableReadService;
    private final FeeIngressDatabaseRowMapper rowMapper;

    @Override
    public FeeDiffResult diff(FeeDiffRequest request) {
        val incomingByKey = new LinkedHashMap<FeeIngressRecord.BusinessKey, FeeIngressRecord>();
        request.processedData().stream()
                .flatMap(page -> request.recordsExtractor().apply(page).stream())
                .map(request.recordMapper())
                .forEach(item -> addIncomingRecord(request.targetTable(), incomingByKey, item));

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
        val existingByKey =
                existingById.values().stream()
                        .collect(Collectors.toMap(FeeIngressRecord::businessKey, item -> item));
        val incomingById = new LinkedHashMap<Long, FeeIngressRecord>();
        for (val incoming : incomingByKey.values()) {
            val existing = existingByKey.get(incoming.businessKey());
            val intendedId =
                    existing == null
                            ? FeeIngressRecord.calculateId(incoming.pssId(), incoming.id())
                            : existing.id();
            if (intendedId == null) {
                throw new AppRegistryException(
                        CommonAppError.INTERNAL_SERVER_ERROR,
                        "Missing source ID for new fee " + incoming.businessKey());
            }
            val intended = incoming.withId(intendedId);
            val idOwner = existingById.get(intendedId);
            val incomingOwner = incomingById.putIfAbsent(intendedId, intended);
            if ((idOwner != null && !idOwner.businessKey().equals(incoming.businessKey()))
                    || incomingOwner != null) {
                val message =
                        "Legacy FEE_ID %s conflicts for incoming fee [%s] with owner [%s]"
                                .formatted(
                                        intendedId,
                                        describe(intended),
                                        describe(incomingOwner != null ? incomingOwner : idOwner));
                log.error(message);
                throw new CsdsDuplicateRecordException(message);
            }
            diffRecords.add(determineDiffRecord(existing, intended));
        }

        return new FeeDiffResult(incomingById, existingById, List.copyOf(diffRecords));
    }

    private void addIncomingRecord(
            String targetTable,
            LinkedHashMap<FeeIngressRecord.BusinessKey, FeeIngressRecord> incomingByKey,
            FeeIngressRecord item) {
        val existing = incomingByKey.putIfAbsent(item.businessKey(), item);
        if (existing == null) {
            return;
        }

        val message =
                ("Duplicate incoming fee business key %s detected for %s. Existing record [%s], "
                                + "duplicate record [%s]")
                        .formatted(
                                item.businessKey(),
                                targetTable,
                                describe(existing),
                                describe(item));
        log.error(message);
        throw new CsdsDuplicateRecordException(message);
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
        return "civilFeeId=%s, pssId=%s, reference=%s, startDate=%s, version=%s"
                .formatted(
                        item.id(),
                        item.pssId(),
                        item.reference(),
                        item.startDate(),
                        item.version());
    }
}
