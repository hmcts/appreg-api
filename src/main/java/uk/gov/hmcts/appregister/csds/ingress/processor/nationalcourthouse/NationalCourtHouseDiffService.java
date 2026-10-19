package uk.gov.hmcts.appregister.csds.ingress.processor.nationalcourthouse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressTableReadService;
import uk.gov.hmcts.appregister.csds.ingress.database.NationalCourtHouseIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;

@Slf4j
@Service
@RequiredArgsConstructor
public class NationalCourtHouseDiffService
        implements IngressDiffService<NationalCourtHouseDiffRequest, NationalCourtHouseDiffResult> {
    private final JdbcIngressTableReadService tableReadService;
    private final NationalCourtHouseIngressDatabaseRowMapper rowMapper;

    @Override
    public NationalCourtHouseDiffResult diff(NationalCourtHouseDiffRequest request) {
        val incomingById = new LinkedHashMap<Long, NationalCourtHouseIngressRecord>();
        val incomingCodes = new HashSet<String>();
        request.processedData().stream()
                .flatMap(page -> request.recordsExtractor().apply(page).stream())
                .map(request.recordMapper())
                .forEach(
                        item -> {
                            if (!incomingCodes.add(
                                    item.courtLocationCode().toUpperCase(Locale.ROOT))) {
                                throw new AppRegistryException(
                                        CommonAppError.INTERNAL_SERVER_ERROR,
                                        "Duplicate incoming CourtLocationCode "
                                                + item.courtLocationCode()
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
        val occupiedIds = new HashSet<>(existingById.keySet());
        val receivedVersions = new HashSet<Map.Entry<Long, String>>();
        existingById.values().stream()
                .filter(item -> item.csdsId() != null && item.courtLocationCode() != null)
                .forEach(
                        item ->
                                receivedVersions.add(
                                        Map.entry(
                                                item.csdsId(),
                                                item.courtLocationCode()
                                                        .toUpperCase(Locale.ROOT))));
        for (val incoming : incomingById.values()) {
            if (receivedVersions.contains(
                    Map.entry(
                            incoming.csdsId(),
                            incoming.courtLocationCode().toUpperCase(Locale.ROOT)))) {
                continue;
            }
            val existing = existingById.get(incoming.id());
            val legacyMatch =
                    existing != null
                            && existing.csdsId() == null
                            && incoming.courtLocationCode()
                                    .equalsIgnoreCase(existing.courtLocationCode());
            val intended =
                    incoming.withId(
                            legacyMatch
                                    ? existing.id()
                                    : Math.addExact(incoming.csdsId(), 100000L));
            if (!legacyMatch && !occupiedIds.add(intended.id())) {
                throw new AppRegistryException(
                        CommonAppError.INTERNAL_SERVER_ERROR,
                        "Generated NCH_ID "
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
                                    ? "first CSDS receipt of legacy record"
                                    : "new CSDS version"));
        }

        return new NationalCourtHouseDiffResult(
                incomingById, existingById, List.copyOf(diffRecords));
    }

    private void addIncomingRecord(
            String targetTable,
            LinkedHashMap<Long, NationalCourtHouseIngressRecord> incomingById,
            NationalCourtHouseIngressRecord item) {
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
