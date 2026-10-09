package uk.gov.hmcts.appregister.csds.ingress.processor.resolutioncode;

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
import uk.gov.hmcts.appregister.csds.ingress.database.ResolutionCodeIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResolutionCodeDiffService
        implements IngressDiffService<ResolutionCodeDiffRequest, ResolutionCodeDiffResult> {
    private static final String INSERT_REASON_NO_EXISTING_MATCH =
            "no existing resolution_code match";
    private static final String UPDATE_REASON_EXISTING_MATCH = "existing resolution_code match";

    private final JdbcIngressTableReadService tableReadService;
    private final ResolutionCodeIngressDatabaseRowMapper rowMapper;

    @Override
    public ResolutionCodeDiffResult diff(ResolutionCodeDiffRequest request) {
        val incomingByCode = new LinkedHashMap<String, ResolutionCodeIngressRecord>();
        request.processedData().stream()
                .flatMap(page -> request.recordsExtractor().apply(page).stream())
                .map(request.recordMapper())
                .forEach(item -> addIncomingRecord(request.targetTable(), incomingByCode, item));

        log.info("Loading existing CSDS comparison rows from {}", request.targetTable());
        val existingById =
                tableReadService.loadAll(request.targetTable(), rowMapper).stream()
                        .collect(
                                Collectors.toMap(
                                        ResolutionCodeIngressRecord::id,
                                        item -> item,
                                        (first, second) -> second,
                                        LinkedHashMap::new));

        val diffRecords =
                new ArrayList<
                        IngressDiffRecord<
                                ResolutionCodeIngressRecord,
                                ResolutionCodeIngressRecord,
                                ResolutionCodeIngressRecord>>();
        val existingByCode =
                existingById.values().stream()
                        .collect(Collectors.toMap(ResolutionCodeIngressRecord::code, item -> item));
        val incomingById = new LinkedHashMap<Long, ResolutionCodeIngressRecord>();
        for (val incoming : incomingByCode.values()) {
            val existing = existingByCode.get(incoming.code());
            val intendedId =
                    existing == null
                            ? ResolutionCodeIngressRecord.calculateId(
                                    incoming.pssId(), incoming.id())
                            : existing.id();
            if (intendedId == null) {
                throw new AppRegistryException(
                        CommonAppError.INTERNAL_SERVER_ERROR,
                        "Missing source ID for new resolution code " + incoming.code());
            }
            val intended = incoming.withId(intendedId);
            val idOwner = existingById.get(intendedId);
            if ((idOwner != null && !idOwner.code().equals(incoming.code()))
                    || incomingById.putIfAbsent(intendedId, intended) != null) {
                throw new AppRegistryException(
                        CommonAppError.INTERNAL_SERVER_ERROR,
                        "Conflicting RC_ID "
                                + intendedId
                                + " for resolution code "
                                + incoming.code());
            }
            diffRecords.add(determineDiffRecord(existing, intended));
        }

        return new ResolutionCodeDiffResult(incomingById, existingById, List.copyOf(diffRecords));
    }

    private void addIncomingRecord(
            String targetTable,
            LinkedHashMap<String, ResolutionCodeIngressRecord> incomingById,
            ResolutionCodeIngressRecord item) {
        val existing = incomingById.putIfAbsent(item.code(), item);
        if (existing == null) {
            return;
        }

        log.error(
                "Duplicate incoming resolution_code {} detected for {}. Existing record [{}], duplicate record [{}]",
                item.code(),
                targetTable,
                describe(existing),
                describe(item));
        throw new AppRegistryException(
                CommonAppError.INTERNAL_SERVER_ERROR,
                "Duplicate incoming resolution_code "
                        + item.code()
                        + " detected for "
                        + targetTable);
    }

    private IngressDiffRecord<
                    ResolutionCodeIngressRecord,
                    ResolutionCodeIngressRecord,
                    ResolutionCodeIngressRecord>
            determineDiffRecord(
                    ResolutionCodeIngressRecord existing, ResolutionCodeIngressRecord incoming) {
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

    private String describe(ResolutionCodeIngressRecord item) {
        return "rcId=%s, code=%s, title=%s, startDate=%s, version=%s"
                .formatted(item.id(), item.code(), item.title(), item.startDate(), item.version());
    }
}
