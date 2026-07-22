package uk.gov.hmcts.appregister.audit.listener.diff;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BulkAuditFormattingTest {

    @Test
    void formatSortedUuidArray_sortsAndFormatsValues() {
        var first = UUID.fromString("11111111-1111-1111-1111-111111111111");
        var second = UUID.fromString("22222222-2222-2222-2222-222222222222");

        assertThat(BulkAuditFormatting.formatSortedUuidArray(List.of(second, first)))
                .isEqualTo("[\"%s\",\"%s\"]".formatted(first, second));
    }

    @Test
    void escape_returnsEmptyStringForNullAndEscapesQuotesAndBackslashes() {
        assertThat(BulkAuditFormatting.escape(null)).isEmpty();
        assertThat(BulkAuditFormatting.escape("a\\b\"c")).isEqualTo("a\\\\b\\\"c");
    }
}
