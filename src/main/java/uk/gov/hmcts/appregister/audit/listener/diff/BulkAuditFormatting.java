package uk.gov.hmcts.appregister.audit.listener.diff;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

public final class BulkAuditFormatting {

    private BulkAuditFormatting() {
        // Utility class.
    }

    public static String formatSortedUuidArray(Collection<UUID> entryIds) {
        return entryIds.stream()
                .sorted()
                .map("\"%s\""::formatted)
                .collect(Collectors.joining(",", "[", "]"));
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
