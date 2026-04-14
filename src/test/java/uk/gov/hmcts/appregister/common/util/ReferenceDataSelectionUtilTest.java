package uk.gov.hmcts.appregister.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDate;
import java.util.List;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;

class ReferenceDataSelectionUtilTest {

    @Test
    void givenOverlappingRows_whenSelectingFirstOrderedRecord_thenReturnsFirstAndLogsWarning() {
        TestReferenceData preferred = new TestReferenceData(null);
        TestReferenceData fallback = new TestReferenceData(LocalDate.of(2025, 10, 31));
        LogCaptor logCaptor = LogCaptor.forClass(ReferenceDataSelectionUtil.class);

        TestReferenceData actual =
                ReferenceDataSelectionUtil.selectFirstOrderedActiveRecord(
                        List.of(preferred, fallback),
                        "application code",
                        "APP001",
                        LocalDate.of(2025, 10, 7),
                        TestReferenceData::endDate);

        assertSame(preferred, actual);
        assertEquals(1, logCaptor.getWarnLogs().size());
    }

    private record TestReferenceData(LocalDate endDate) {}
}
