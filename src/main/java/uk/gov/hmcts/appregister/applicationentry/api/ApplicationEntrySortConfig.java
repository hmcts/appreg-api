package uk.gov.hmcts.appregister.applicationentry.api;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.appregister.common.mapper.SortConfig;

/**
 * Named sort setups for application entry endpoints, so controllers do not wire them inline.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApplicationEntrySortConfig {
    public static final SortConfig SEARCH =
            new SortConfig(
                    ApplicationEntryDefaultSortFieldEnum.CODE,
                    ApplicationEntrySortFieldEnum::getEntityValue);

    public static final SortConfig BY_LIST_ID =
            new SortConfig(
                    ApplicationEntryByListIdSortFieldEnum.SEQUENCE_NUMBER,
                    ApplicationEntryByListIdSortFieldEnum::getEntityValue);
}
