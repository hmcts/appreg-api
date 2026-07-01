package uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;

@Component
@RequiredArgsConstructor
public class ApplicationCodeDiffService
        implements IngressDiffService<List<JsonNode>, ApplicationCodeDiffResult> {
    private static final String IGNORE_REASON_END_DATED_IN_PAST = "same end date in past";
    private static final String INSERT_REASON_NO_EXISTING_MATCH = "no existing match";
    private static final String UPDATE_REASON_ACTIVE_EXISTING_RECORD = "existing record active";
    private static final String UPDATE_REASON_END_DATE_CHANGED = "end date changed";

    private final ApplicationCodeRepository applicationCodeRepository;

    @Override
    public ApplicationCodeDiffResult diff(List<JsonNode> processedData) {
        throw new UnsupportedOperationException("Use diff(...) with record mapping functions");
    }

    public ApplicationCodeDiffResult diff(
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
                                        record -> record,
                                        (first, second) -> second,
                                        LinkedHashMap::new));

        val existingById =
                applicationCodeRepository.findAll().stream()
                        .map(ApplicationCodeIngressRecord::fromEntity)
                        .collect(
                                Collectors.toMap(
                                        ApplicationCodeIngressRecord::id,
                                        record -> record,
                                        (first, second) -> second,
                                        LinkedHashMap::new));
        val existingByBusinessKey =
                existingById.values().stream()
                        .collect(
                                Collectors.toMap(
                                        ApplicationCodeBusinessKey::from,
                                        Function.identity(),
                                        (first, second) -> second,
                                        LinkedHashMap::new));

        val diffRecords =
                new ArrayList<
                        IngressDiffRecord<
                                ApplicationCodeIngressRecord, ApplicationCodeIngressRecord>>();
        for (val incoming : incomingById.values()) {
            val existing = existingByBusinessKey.get(ApplicationCodeBusinessKey.from(incoming));
            diffRecords.add(determineDiffRecord(existing, incoming));
        }

        return new ApplicationCodeDiffResult(incomingById, existingById, List.copyOf(diffRecords));
    }

    private IngressDiffRecord<ApplicationCodeIngressRecord, ApplicationCodeIngressRecord>
            determineDiffRecord(
                    ApplicationCodeIngressRecord existing, ApplicationCodeIngressRecord incoming) {
        if (existing == null) {
            return new IngressDiffRecord<>(
                    IngressOperation.INSERT, incoming, null, INSERT_REASON_NO_EXISTING_MATCH);
        }

        if (existing.endDate() == null) {
            return new IngressDiffRecord<>(
                    IngressOperation.UPDATE,
                    incoming,
                    existing,
                    UPDATE_REASON_ACTIVE_EXISTING_RECORD);
        }

        if (Objects.equals(existing.endDate(), incoming.endDate())
                && existing.endDate().isBefore(LocalDate.now())) {
            return new IngressDiffRecord<>(
                    IngressOperation.IGNORE, incoming, existing, IGNORE_REASON_END_DATED_IN_PAST);
        }

        return new IngressDiffRecord<>(
                IngressOperation.UPDATE, incoming, existing, UPDATE_REASON_END_DATE_CHANGED);
    }

    private record ApplicationCodeBusinessKey(String code, LocalDate startDate) {
        private static ApplicationCodeBusinessKey from(ApplicationCodeIngressRecord record) {
            return new ApplicationCodeBusinessKey(record.code(), record.startDate());
        }
    }
}
