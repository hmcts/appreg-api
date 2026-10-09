package uk.gov.hmcts.appregister.csds.ingress.processor.resolutioncode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressTableReadService;
import uk.gov.hmcts.appregister.csds.ingress.database.ResolutionCodeIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;

class ResolutionCodeDiffServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final JdbcIngressTableReadService reader = mock(JdbcIngressTableReadService.class);
    private final ResolutionCodeIngressDatabaseRowMapper rowMapper =
            new ResolutionCodeIngressDatabaseRowMapper();
    private final ResolutionCodeDiffService service =
            new ResolutionCodeDiffService(reader, rowMapper);

    @Test
    void existingCodeKeepsStoredIdEvenWithMissingOrOverflowingSourceIds() {
        var existing = record(42L, null, "CODE");
        when(reader.loadAll("resolution_codes", rowMapper)).thenReturn(List.of(existing));
        for (var incoming :
                List.of(record(null, null, "CODE"), record(Long.MAX_VALUE, 99L, "CODE"))) {
            var diff = diff(List.of(incoming));
            assertThat(diff.incomingById()).containsOnlyKeys(42L);
            assertThat(diff.diffRecords().getFirst().operation())
                    .isEqualTo(IngressOperation.UPDATE);
            assertThat(diff.diffRecords().getFirst().intended().id()).isEqualTo(existing.id());
        }
    }

    @Test
    void newCodesUsePssOrOffsetAndRepeatIngressIsStable() {
        when(reader.loadAll("resolution_codes", rowMapper)).thenReturn(List.of());
        var first = diff(List.of(record(7L, null, "OFFSET"), record(8L, 77L, "PSS")));
        assertThat(first.incomingById()).containsOnlyKeys(100007L, 77L);
        when(reader.loadAll("resolution_codes", rowMapper))
                .thenReturn(first.diffRecords().stream().map(item -> item.intended()).toList());
        assertThat(
                        diff(List.of(record(null, null, "OFFSET"), record(null, null, "PSS")))
                                .diffRecords())
                .allSatisfy(
                        item -> {
                            assertThat(item.operation()).isEqualTo(IngressOperation.UPDATE);
                            assertThat(item.intended().id()).isEqualTo(item.existing().id());
                        });
    }

    @Test
    void duplicateCodesAcrossPagesFailEvenWithDifferentIds() {
        assertThatThrownBy(() -> diff(List.of(record(1L, null, "CODE"), record(2L, null, "CODE"))))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("Duplicate incoming resolution_code CODE");
    }

    @Test
    void missingIdsOverflowAndExistingOrIncomingNumericCollisionsFail() {
        when(reader.loadAll("resolution_codes", rowMapper))
                .thenReturn(List.of(record(42L, null, "OLD")));
        assertThatThrownBy(() -> diff(List.of(record(null, null, "NEW"))))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("Missing source ID");
        assertThatThrownBy(() -> diff(List.of(record(Long.MAX_VALUE, null, "NEW"))))
                .isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> diff(List.of(record(7L, 42L, "NEW"))))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("Conflicting RC_ID 42");
        assertThatThrownBy(
                        () -> diff(List.of(record(7L, null, "NEW"), record(8L, 100007L, "OTHER"))))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("Conflicting RC_ID 100007");
    }

    private ResolutionCodeDiffResult diff(List<ResolutionCodeIngressRecord> records) {
        // Each record is a separate page, so identity checks must span page boundaries.
        var pages =
                records.stream()
                        .map(
                                record ->
                                        mapper.createArrayNode()
                                                .add(
                                                        mapper.createObjectNode()
                                                                .put(
                                                                        "index",
                                                                        records.indexOf(record))))
                        .map(node -> (JsonNode) node)
                        .toList();
        return service.diff(
                new ResolutionCodeDiffRequest(
                        "resolution_codes",
                        pages,
                        node -> records.get(node.path("index").asInt()),
                        page -> StreamSupport.stream(page.spliterator(), false).toList()));
    }

    private ResolutionCodeIngressRecord record(Long id, Long pssId, String code) {
        return new ResolutionCodeIngressRecord(
                id,
                code,
                "Title",
                "Wording",
                null,
                null,
                null,
                LocalDate.of(2020, 1, 1),
                null,
                1L,
                pssId);
    }
}
