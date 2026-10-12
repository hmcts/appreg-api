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
import uk.gov.hmcts.appregister.csds.ingress.exception.CsdsDuplicateRecordException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NationalCourtHouseDiffService
        implements IngressDiffService<NationalCourtHouseDiffRequest, NationalCourtHouseDiffResult> {
    private static final String INSERT_REASON_NO_EXISTING_MATCH =
            "no existing court_location_code match";
    private static final String UPDATE_REASON_EXISTING_MATCH = "existing court_location_code match";

    private final JdbcIngressTableReadService tableReadService;
    private final NationalCourtHouseIngressDatabaseRowMapper rowMapper;

    @Override
    public NationalCourtHouseDiffResult diff(NationalCourtHouseDiffRequest request) {
        val incomingByCode = new LinkedHashMap<String, NationalCourtHouseIngressRecord>();
        request.processedData().stream()
                .flatMap(page -> request.recordsExtractor().apply(page).stream())
                .map(request.recordMapper())
                .forEach(item -> addIncomingRecord(request.targetTable(), incomingByCode, item));

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
        val existingByCode =
                existingById.values().stream()
                        .collect(
                                Collectors.toMap(
                                        NationalCourtHouseIngressRecord::courtLocationCode,
                                        item -> item));
        val incomingById = new LinkedHashMap<Long, NationalCourtHouseIngressRecord>();
        for (val incoming : incomingByCode.values()) {
            val existing = existingByCode.get(incoming.courtLocationCode());
            val intendedId =
                    existing == null
                            ? NationalCourtHouseIngressRecord.calculateId(
                                    incoming.pssId(), incoming.id())
                            : existing.id();
            if (intendedId == null) {
                throw new AppRegistryException(
                        CommonAppError.INTERNAL_SERVER_ERROR,
                        "Missing source ID for new national courthouse "
                                + incoming.courtLocationCode());
            }
            val intended = incoming.withId(intendedId);
            val idOwner = existingById.get(intendedId);
            val incomingOwner = incomingById.putIfAbsent(intendedId, intended);
            if ((idOwner != null
                            && !idOwner.courtLocationCode().equals(incoming.courtLocationCode()))
                    || incomingOwner != null) {
                val message =
                        "Legacy NCH_ID %s conflicts for incoming national courthouse [%s] with owner [%s]"
                                .formatted(
                                        intendedId,
                                        describe(intended),
                                        describe(incomingOwner != null ? incomingOwner : idOwner));
                log.error(message);
                throw new CsdsDuplicateRecordException(message);
            }
            diffRecords.add(determineDiffRecord(existing, intended));
        }

        return new NationalCourtHouseDiffResult(
                incomingById, existingById, List.copyOf(diffRecords));
    }

    private void addIncomingRecord(
            String targetTable,
            LinkedHashMap<String, NationalCourtHouseIngressRecord> incomingByCode,
            NationalCourtHouseIngressRecord item) {
        val existing = incomingByCode.putIfAbsent(item.courtLocationCode(), item);
        if (existing == null) {
            return;
        }

        val message =
                ("Duplicate incoming court_location_code %s detected for %s. Existing record [%s], "
                                + "duplicate record [%s]")
                        .formatted(
                                item.courtLocationCode(),
                                targetTable,
                                describe(existing),
                                describe(item));
        log.error(message);
        throw new CsdsDuplicateRecordException(message);
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
