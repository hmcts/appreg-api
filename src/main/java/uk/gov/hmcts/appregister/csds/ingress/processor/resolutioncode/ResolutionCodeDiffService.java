package uk.gov.hmcts.appregister.csds.ingress.processor.resolutioncode;

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
import uk.gov.hmcts.appregister.csds.ingress.database.ResolutionCodeIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResolutionCodeDiffService
        implements IngressDiffService<ResolutionCodeDiffRequest, ResolutionCodeDiffResult> {
    private static final String INSERT_REASON_NO_EXISTING_MATCH = "no existing rc_id match";
    private static final String UPDATE_REASON_EXISTING_MATCH = "existing rc_id match";

    private final JdbcIngressTableReadService tableReadService;
    private final ResolutionCodeIngressDatabaseRowMapper rowMapper;

    @Override
    public ResolutionCodeDiffResult diff(ResolutionCodeDiffRequest request) {
        val incomingById = new LinkedHashMap<Long, ResolutionCodeIngressRecord>();
        request.processedData().stream()
                .flatMap(page -> request.recordsExtractor().apply(page).stream())
                .map(request.recordMapper())
                .forEach(item -> addIncomingRecord(request.targetTable(), incomingById, item));

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
        for (val incoming : incomingById.values()) {
            val existing = existingById.get(incoming.id());
            diffRecords.add(determineDiffRecord(existing, incoming));
        }

        return new ResolutionCodeDiffResult(incomingById, existingById, List.copyOf(diffRecords));
    }

    private void addIncomingRecord(
            String targetTable,
            LinkedHashMap<Long, ResolutionCodeIngressRecord> incomingById,
            ResolutionCodeIngressRecord item) {
        val existing = incomingById.putIfAbsent(item.id(), item);
        if (existing == null) {
            return;
        }

        log.error(
                "Duplicate incoming RC_ID {} detected for {}. Existing record [{}], duplicate record [{}]",
                item.id(),
                targetTable,
                describe(existing),
                describe(item));
        throw new AppRegistryException(
                CommonAppError.INTERNAL_SERVER_ERROR,
                "Duplicate incoming RC_ID " + item.id() + " detected for " + targetTable);
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
