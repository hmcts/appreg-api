package uk.gov.hmcts.appregister.csds.ingress.processor.nationalcourthouse;

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
    private static final String INSERT_REASON_NO_EXISTING_MATCH =
            "no existing courthouse_name match";
    private static final String UPDATE_REASON_EXISTING_MATCH = "existing courthouse_name match";

    private final JdbcIngressTableReadService tableReadService;
    private final NationalCourtHouseIngressDatabaseRowMapper rowMapper;

    @Override
    public NationalCourtHouseDiffResult diff(NationalCourtHouseDiffRequest request) {
        val incomingByName = new LinkedHashMap<String, NationalCourtHouseIngressRecord>();
        request.processedData().stream()
                .flatMap(page -> request.recordsExtractor().apply(page).stream())
                .map(request.recordMapper())
                .forEach(item -> addIncomingRecord(request.targetTable(), incomingByName, item));

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
        val existingByName =
                existingById.values().stream()
                        .collect(
                                Collectors.toMap(
                                        NationalCourtHouseIngressRecord::name, item -> item));
        val incomingById = new LinkedHashMap<Long, NationalCourtHouseIngressRecord>();
        for (val incoming : incomingByName.values()) {
            val existing = existingByName.get(incoming.name());
            val intendedId =
                    existing == null
                            ? NationalCourtHouseIngressRecord.calculateId(
                                    incoming.pssId(), incoming.id())
                            : existing.id();
            if (intendedId == null) {
                throw new AppRegistryException(
                        CommonAppError.INTERNAL_SERVER_ERROR,
                        "Missing source ID for new national courthouse " + incoming.name());
            }
            val intended = incoming.withId(intendedId);
            val idOwner = existingById.get(intendedId);
            if ((idOwner != null && !idOwner.name().equals(incoming.name()))
                    || incomingById.putIfAbsent(intendedId, intended) != null) {
                throw new AppRegistryException(
                        CommonAppError.INTERNAL_SERVER_ERROR,
                        "Conflicting NCH_ID "
                                + intendedId
                                + " for national courthouse "
                                + incoming.name());
            }
            diffRecords.add(determineDiffRecord(existing, intended));
        }

        return new NationalCourtHouseDiffResult(
                incomingById, existingById, List.copyOf(diffRecords));
    }

    private void addIncomingRecord(
            String targetTable,
            LinkedHashMap<String, NationalCourtHouseIngressRecord> incomingByName,
            NationalCourtHouseIngressRecord item) {
        val existing = incomingByName.putIfAbsent(item.name(), item);
        if (existing == null) {
            return;
        }

        log.error(
                "Duplicate incoming courthouse_name {} detected for {}. Existing record [{}], duplicate record [{}]",
                item.name(),
                targetTable,
                describe(existing),
                describe(item));
        throw new AppRegistryException(
                CommonAppError.INTERNAL_SERVER_ERROR,
                "Duplicate incoming courthouse_name "
                        + item.name()
                        + " detected for "
                        + targetTable);
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
        return "courtId=%s, pssId=%s, name=%s, locationCode=%s, startDate=%s, version=%s"
                .formatted(
                        item.id(),
                        item.pssId(),
                        item.name(),
                        item.courtLocationCode(),
                        item.startDate(),
                        item.version());
    }
}
