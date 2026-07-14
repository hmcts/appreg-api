package uk.gov.hmcts.appregister.csds.ingress.processor.nationalcourthouse;

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
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressTableReadService;
import uk.gov.hmcts.appregister.csds.ingress.database.NationalCourtHouseIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;

@Slf4j
@Component
@RequiredArgsConstructor
public class NationalCourtHouseDiffService
        implements IngressDiffService<NationalCourtHouseDiffRequest, NationalCourtHouseDiffResult> {
    private static final String INSERT_REASON_NO_EXISTING_MATCH = "no existing nch_id match";
    private static final String UPDATE_REASON_EXISTING_MATCH = "existing nch_id match";

    private final JdbcIngressTableReadService tableReadService;
    private final NationalCourtHouseIngressDatabaseRowMapper rowMapper;

    @Override
    public NationalCourtHouseDiffResult diff(NationalCourtHouseDiffRequest request) {
        val incomingById = new LinkedHashMap<Long, NationalCourtHouseIngressRecord>();
        request.processedData().stream()
                .flatMap(page -> request.recordsExtractor().apply(page).stream())
                .map(request.recordMapper())
                .forEach(item -> addIncomingRecord(request.targetTable(), incomingById, item));

        log.info("Loading existing CSDS comparison rows from {}", request.targetTable());
        val existingById =
                tableReadService.loadAll(request.targetTable(), rowMapper).stream()
                        .collect(
                                Collectors.toMap(
                                        NationalCourtHouseIngressRecord::id,
                                        item -> item,
                                        (first, second) -> second,
                                        LinkedHashMap::new));

        val diffRecords =
                new ArrayList<
                        IngressDiffRecord<
                                NationalCourtHouseIngressRecord,
                                NationalCourtHouseIngressRecord,
                                NationalCourtHouseIngressRecord>>();
        for (val incoming : incomingById.values()) {
            val existing = existingById.get(incoming.id());
            diffRecords.add(determineDiffRecord(existing, incoming));
        }

        return new NationalCourtHouseDiffResult(
                incomingById, existingById, List.copyOf(diffRecords));
    }

    private void addIncomingRecord(
            String targetTable,
            LinkedHashMap<Long, NationalCourtHouseIngressRecord> incomingById,
            NationalCourtHouseIngressRecord item) {
        val existing = incomingById.putIfAbsent(item.id(), item);
        if (existing == null) {
            return;
        }

        log.error(
                "Duplicate incoming NCH_ID {} detected for {}. Existing record [{}], duplicate record [{}]",
                item.id(),
                targetTable,
                describe(existing),
                describe(item));
        throw new AppRegistryException(
                CommonAppError.INTERNAL_SERVER_ERROR,
                "Duplicate incoming NCH_ID " + item.id() + " detected for " + targetTable);
    }

    private IngressDiffRecord<
                    NationalCourtHouseIngressRecord,
                    NationalCourtHouseIngressRecord,
                    NationalCourtHouseIngressRecord>
            determineDiffRecord(
                    NationalCourtHouseIngressRecord existing,
                    NationalCourtHouseIngressRecord incoming) {
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

    private String describe(NationalCourtHouseIngressRecord item) {
        return "nchId=%s, name=%s, locationCode=%s, startDate=%s, version=%s"
                .formatted(
                        item.id(),
                        item.name(),
                        item.courtLocationCode(),
                        item.startDate(),
                        item.version());
    }
}
