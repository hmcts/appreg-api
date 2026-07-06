package uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.csds.ingress.database.ApplicationCodeIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressTableReadService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationCodeDiffService
        implements IngressDiffService<List<JsonNode>, ApplicationCodeDiffResult> {
    private static final String INSERT_REASON_NO_EXISTING_MATCH = "no existing ac_id match";
    private static final String UPDATE_REASON_EXISTING_MATCH = "existing ac_id match";

    private final JdbcIngressTableReadService tableReadService;
    private final ApplicationCodeIngressDatabaseRowMapper rowMapper;

    @Override
    public ApplicationCodeDiffResult diff(List<JsonNode> processedData) {
        throw new UnsupportedOperationException("Use diff(...) with record mapping functions");
    }

    public ApplicationCodeDiffResult diff(
            String targetTable,
            List<JsonNode> processedData,
            Function<JsonNode, ApplicationCodeIngressRecord> recordMapper,
            Function<JsonNode, List<JsonNode>> recordsExtractor) {
        val incomingById =
                processedData.stream()
                        .flatMap(page -> recordsExtractor.apply(page).stream())
                        .map(recordMapper)
                        .collect(
                                Collectors.toMap(
                                        ApplicationCodeIngressRecord::id,
                                        item -> item,
                                        (first, second) -> second,
                                        LinkedHashMap::new));

        log.info("Loading existing CSDS comparison rows from {}", targetTable);
        val existingById =
                tableReadService.loadAll(targetTable, rowMapper).stream()
                        .collect(
                                Collectors.toMap(
                                        ApplicationCodeIngressRecord::id,
                                        item -> item,
                                        (first, second) -> second,
                                        LinkedHashMap::new));

        val diffRecords =
                new ArrayList<
                        IngressDiffRecord<
                                ApplicationCodeIngressRecord,
                                ApplicationCodeIngressRecord,
                                ApplicationCodeIngressRecord>>();
        for (val incoming : incomingById.values()) {
            val existing = existingById.get(incoming.id());
            diffRecords.add(determineDiffRecord(existing, incoming));
        }

        return new ApplicationCodeDiffResult(incomingById, existingById, List.copyOf(diffRecords));
    }

    private IngressDiffRecord<
                    ApplicationCodeIngressRecord,
                    ApplicationCodeIngressRecord,
                    ApplicationCodeIngressRecord>
            determineDiffRecord(
                    ApplicationCodeIngressRecord existing, ApplicationCodeIngressRecord incoming) {
        if (existing == null) {
            return new IngressDiffRecord<>(
                    IngressOperation.INSERT,
                    incoming,
                    null,
                    buildIntendedRecord(null, incoming),
                    INSERT_REASON_NO_EXISTING_MATCH);
        }

        return new IngressDiffRecord<>(
                IngressOperation.UPDATE,
                incoming,
                existing,
                buildIntendedRecord(existing, incoming),
                UPDATE_REASON_EXISTING_MATCH);
    }

    private ApplicationCodeIngressRecord buildIntendedRecord(
            ApplicationCodeIngressRecord existing, ApplicationCodeIngressRecord incoming) {
        // ApplicationCode currently upserts directly from the incoming CSDS representation.
        return incoming;
    }
}
