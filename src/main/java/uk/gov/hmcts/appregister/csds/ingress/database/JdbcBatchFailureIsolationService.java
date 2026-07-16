package uk.gov.hmcts.appregister.csds.ingress.database;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JdbcBatchFailureIsolationService {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;

    public <T> List<FailedUpsertRecord<T>> identifyFailures(
            String sql,
            List<T> records,
            Function<T, Map<String, Object>> parameterMapper,
            Function<T, Long> keyExtractor,
            RuntimeException batchException) {
        var failures = new ArrayList<FailedUpsertRecord<T>>();
        var duplicateKeys = duplicateKeys(records, keyExtractor);
        if (!duplicateKeys.isEmpty()) {
            records.stream()
                    .filter(item -> duplicateKeys.contains(keyExtractor.apply(item)))
                    .forEach(
                            item ->
                                    failures.add(
                                            new FailedUpsertRecord<>(
                                                    item,
                                                    "Duplicate batch key %s detected. %s"
                                                            .formatted(
                                                                    keyExtractor.apply(item),
                                                                    mostSpecificMessage(
                                                                            batchException)))));
        }

        records.stream()
                .filter(item -> !duplicateKeys.contains(keyExtractor.apply(item)))
                .forEach(
                        item -> {
                            var error = isolateSingleRowFailure(sql, parameterMapper.apply(item));
                            if (error != null) {
                                failures.add(new FailedUpsertRecord<>(item, error));
                            }
                        });

        if (!failures.isEmpty()) {
            return List.copyOf(failures);
        }

        return records.stream()
                .map(
                        item ->
                                new FailedUpsertRecord<>(
                                        item,
                                        mostSpecificMessage(batchException)
                                                + " (batch failure could not be isolated to a "
                                                + "single row)"))
                .toList();
    }

    private String isolateSingleRowFailure(String sql, Map<String, Object> parameters) {
        var template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(
                status -> {
                    try {
                        jdbcTemplate.update(sql, new MapSqlParameterSource(parameters));
                        return null;
                    } catch (RuntimeException ex) {
                        return mostSpecificMessage(ex);
                    } finally {
                        status.setRollbackOnly();
                    }
                });
    }

    private <T> LinkedHashSet<Long> duplicateKeys(List<T> records, Function<T, Long> keyExtractor) {
        var seen = new LinkedHashSet<Long>();
        var duplicates = new LinkedHashSet<Long>();
        records.stream()
                .map(keyExtractor)
                .forEach(
                        key -> {
                            if (!seen.add(key)) {
                                duplicates.add(key);
                            }
                        });
        return duplicates;
    }

    private String mostSpecificMessage(Throwable throwable) {
        var current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        if (StringUtils.hasText(current.getMessage())) {
            return current.getMessage();
        }
        if (StringUtils.hasText(throwable.getMessage())) {
            return throwable.getMessage();
        }
        return throwable.getClass().getSimpleName();
    }
}
