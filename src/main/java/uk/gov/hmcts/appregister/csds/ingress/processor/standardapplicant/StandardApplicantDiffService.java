package uk.gov.hmcts.appregister.csds.ingress.processor.standardapplicant;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressTableReadService;
import uk.gov.hmcts.appregister.csds.ingress.database.StandardApplicantIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;

@Slf4j
@Service
@RequiredArgsConstructor
public class StandardApplicantDiffService
        implements IngressDiffService<StandardApplicantDiffRequest, StandardApplicantDiffResult> {
    private final JdbcIngressTableReadService tableReadService;
    private final StandardApplicantIngressDatabaseRowMapper rowMapper;

    @Override
    public StandardApplicantDiffResult diff(StandardApplicantDiffRequest request) {
        val incomingById = new LinkedHashMap<Long, StandardApplicantIngressRecord>();
        val incomingCodes = new HashSet<String>();
        request.processedData().stream()
                .flatMap(page -> request.recordsExtractor().apply(page).stream())
                .map(request.recordMapper())
                .forEach(
                        item -> {
                            if (!incomingCodes.add(item.code())) {
                                throw new AppRegistryException(
                                        CommonAppError.INTERNAL_SERVER_ERROR,
                                        "Duplicate incoming Code "
                                                + item.code()
                                                + " detected for "
                                                + request.targetTable());
                            }
                            addIncomingRecord(request.targetTable(), incomingById, item);
                        });

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
        val occupiedIds = new HashSet<>(existingById.keySet());
        val receivedVersions = new HashSet<Map.Entry<Long, String>>();
        existingById.values().stream()
                .filter(item -> item.csdsId() != null)
                .forEach(item -> receivedVersions.add(Map.entry(item.csdsId(), item.code())));
        for (val incoming : incomingById.values()) {
            if (receivedVersions.contains(Map.entry(incoming.csdsId(), incoming.code()))) {
                continue;
            }
            // Earlier imports also allocated offset IDs without recording the CSDS identity.
            val existing =
                    existingById.get(
                            incoming.id() != null
                                    ? incoming.id()
                                    : Math.addExact(incoming.csdsId(), 100000L));
            val legacyMatch =
                    existing != null
                            && existing.csdsId() == null
                            && existing.code().equals(incoming.code());
            val intended =
                    incoming.withId(
                            legacyMatch
                                    ? existing.id()
                                    : Math.addExact(incoming.csdsId(), 100000L));
            if (!legacyMatch && !occupiedIds.add(intended.id())) {
                throw new AppRegistryException(
                        CommonAppError.INTERNAL_SERVER_ERROR,
                        "Generated SA_ID "
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
        val rejectedFutureCodes = new HashSet<String>();
        request.processedData()
                .forEach(
                        page ->
                                page.path("rejectedFutureCodes")
                                        .forEach(code -> rejectedFutureCodes.add(code.asText())));
        return new StandardApplicantDiffResult(
                incomingById,
                existingById,
                List.copyOf(diffRecords),
                Set.copyOf(rejectedFutureCodes));
    }

    private void addIncomingRecord(
            String targetTable,
            LinkedHashMap<Long, StandardApplicantIngressRecord> incomingById,
            StandardApplicantIngressRecord item) {
        if (incomingById.putIfAbsent(item.csdsId(), item) == null) {
            return;
        }
        throw new AppRegistryException(
                CommonAppError.INTERNAL_SERVER_ERROR,
                "Duplicate incoming CSDS_ID " + item.csdsId() + " detected for " + targetTable);
    }
}
