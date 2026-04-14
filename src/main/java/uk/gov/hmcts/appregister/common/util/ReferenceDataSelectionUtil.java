package uk.gov.hmcts.appregister.common.util;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Shared selection rules for active reference data that can legally overlap in the database.
 *
 * <p>Selection is deterministic for already ordered active rows: callers supply the ordered match
 * set and this utility returns the first row while logging a data-quality warning if overlaps
 * exist.
 */
@Slf4j
@UtilityClass
public class ReferenceDataSelectionUtil {

    /** Logs overlapping active rows and returns the first record from an already ordered list. */
    public static <T> T selectFirstOrderedActiveRecord(
            List<T> matches,
            String referenceDataType,
            String referenceKey,
            LocalDate asOfDate,
            Function<T, LocalDate> endDateExtractor) {
        if (matches.size() > 1) {
            long nullEndDateCount =
                    matches.stream().filter(match -> endDateExtractor.apply(match) == null).count();

            log.warn(
                    "Data quality warning: {} active {} records found for key '{}' on {}. "
                            + "Selected the first record after deterministic ordering; "
                            + "{} matching rows have endDate=null.",
                    matches.size(),
                    referenceDataType,
                    referenceKey,
                    asOfDate,
                    nullEndDateCount);
        }

        return matches.getFirst();
    }
}
