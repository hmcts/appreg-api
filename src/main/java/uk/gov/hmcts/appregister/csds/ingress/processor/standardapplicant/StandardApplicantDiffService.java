package uk.gov.hmcts.appregister.csds.ingress.processor.standardapplicant;

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
import uk.gov.hmcts.appregister.csds.ingress.database.StandardApplicantIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;

@Slf4j
@Component
@RequiredArgsConstructor
public class StandardApplicantDiffService
        implements IngressDiffService<StandardApplicantDiffRequest, StandardApplicantDiffResult> {
    private final JdbcIngressTableReadService tableReadService;
    private final StandardApplicantIngressDatabaseRowMapper rowMapper;

    @Override
    public StandardApplicantDiffResult diff(StandardApplicantDiffRequest request) {
        val incomingById = new LinkedHashMap<Long, StandardApplicantIngressRecord>();
        request.processedData().stream()
                .flatMap(page -> request.recordsExtractor().apply(page).stream())
                .map(request.recordMapper())
                .forEach(item -> addIncomingRecord(request.targetTable(), incomingById, item));

        val existingById =
                tableReadService.loadAll(request.targetTable(), rowMapper).stream()
                        .collect(
                                Collectors.toMap(
                                        StandardApplicantIngressRecord::id,
                                        item -> item,
                                        (first, second) -> second,
                                        LinkedHashMap::new));
        val diffRecords =
                new ArrayList<
                        IngressDiffRecord<
                                StandardApplicantIngressRecord,
                                StandardApplicantIngressRecord,
                                StandardApplicantIngressRecord>>();
        for (val incoming : incomingById.values()) {
            val existing = existingById.get(incoming.id());
            diffRecords.add(
                    new IngressDiffRecord<>(
                            existing == null ? IngressOperation.INSERT : IngressOperation.UPDATE,
                            incoming,
                            existing,
                            incoming,
                            existing == null ? "no existing sa_id match" : "existing sa_id match"));
        }
        return new StandardApplicantDiffResult(
                incomingById, existingById, List.copyOf(diffRecords));
    }

    private void addIncomingRecord(
            String targetTable,
            LinkedHashMap<Long, StandardApplicantIngressRecord> incomingById,
            StandardApplicantIngressRecord item) {
        if (incomingById.putIfAbsent(item.id(), item) == null) {
            return;
        }
        throw new AppRegistryException(
                CommonAppError.INTERNAL_SERVER_ERROR,
                "Duplicate incoming SA_ID " + item.id() + " detected for " + targetTable);
    }
}
